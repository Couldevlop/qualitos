import { Component, Inject, Optional } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PrivacyNoticesService } from '../../privacy-notices.service';
import { PrivacyNoticeView } from '../../privacy-notices.types';

export interface PnDialogData { notice?: PrivacyNoticeView; }

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function linesToList(input?: string | null): string[] {
  if (!input) return [];
  const set = new Set<string>();
  for (const line of input.split('\n')) {
    const v = line.trim();
    if (v) set.add(v);
  }
  return Array.from(set);
}

function listToLines(arr?: string[] | null): string {
  return (arr ?? []).join('\n');
}

/** OWASP A03 — chaque ligne du textarea linked-ROPA doit être un UUID v4. */
function uuidLinesValidator(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const ids = linesToList(ctrl.value);
    for (const id of ids) if (!UUID_REGEX.test(id)) return { uuidLine: id };
    return null;
  };
}

@Component({
  selector: 'qos-pn-dialog',
  templateUrl: './pn-dialog.component.html',
  styleUrls: ['./pn-dialog.component.scss'],
  standalone: false
})
export class PnDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: PrivacyNoticesService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PnDialogComponent, PrivacyNoticeView>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: PnDialogData | null
  ) {
    this.isEdit = !!data?.notice;
    const n = data?.notice;
    this.form = this.fb.nonNullable.group({
      // OWASP A03 — regex + length miroirs backend @Pattern + @Size.
      // reference / version / language sont immutables en édition
      // (clé fonctionnelle + version + langue, miroir backend).
      reference: [
        { value: n?.reference ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(64),
         Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]
      ],
      version: [
        { value: n?.version ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(32),
         Validators.pattern(/^[A-Za-z0-9._:-]{1,32}$/)]
      ],
      language: [
        { value: n?.language ?? 'fr', disabled: this.isEdit },
        [Validators.required, Validators.minLength(2), Validators.maxLength(2),
         Validators.pattern(/^[a-z]{2}$/)]
      ],

      title:   [n?.title ?? '',   [Validators.required, Validators.maxLength(250)]],
      summary: [n?.summary ?? '', [Validators.maxLength(2000)]],
      contentMarkdown: [n?.contentMarkdown ?? '', [Validators.maxLength(65000)]],

      linkedProcessingActivityIds: [
        listToLines(n?.linkedProcessingActivityIds),
        [uuidLinesValidator()]
      ],

      publishUrl: ['', [Validators.maxLength(1024), Validators.pattern(/^https:\/\/.+/)]],
      contactName:  [n?.contactName  ?? '', [Validators.maxLength(250)]],
      contactEmail: [n?.contactEmail ?? '', [Validators.maxLength(250), Validators.email]]
    });

    // publishUrl pattern only applies when value is non-empty.
    this.form.controls.publishUrl.valueChanges.subscribe(v => {
      if (!v) this.form.controls.publishUrl.setErrors(null);
    });
    this.form.controls.publishUrl.setValue(n?.publishUrl ?? '');
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();

    const payload = {
      title: v.title.trim(),
      summary: v.summary?.trim() || undefined,
      contentMarkdown: v.contentMarkdown?.trim() || undefined,
      linkedProcessingActivityIds: linesToList(v.linkedProcessingActivityIds),
      publishUrl:   v.publishUrl?.trim() || undefined,
      contactName:  v.contactName?.trim()  || undefined,
      contactEmail: v.contactEmail?.trim() || undefined
    };

    const op$ = this.isEdit
      ? this.svc.edit(this.data!.notice!.id, payload)
      : (() => {
          const createdByUserId = this.auth.snapshot()?.userId;
          if (!createdByUserId) {
            this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.create({
            reference: v.reference.trim(),
            version:   v.version.trim(),
            language:  v.language.trim().toLowerCase(),
            ...payload,
            createdByUserId
          });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: n => {
          this.snack.open(this.isEdit ? 'Mention mise à jour.' : 'Mention créée (DRAFT).', 'OK', { duration: 2500 });
          this.dialogRef.close(n);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pn-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
