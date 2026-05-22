import { Component, Inject, Optional } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { RopaService } from '../../ropa.service';
import { LawfulBasis, ProcessingActivityView } from '../../ropa.types';

export interface RopaDialogData { activity?: ProcessingActivityView; }

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Parses a textarea (one item per line) into a trimmed, de-duplicated string list. */
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

/**
 * OWASP A03 — refuses any line that is not a UUID v4 inside the
 * "linkedRetentionRuleIds" textarea.
 */
function uuidLinesValidator(): ValidatorFn {
  return (ctrl: AbstractControl): ValidationErrors | null => {
    const ids = linesToList(ctrl.value);
    for (const id of ids) if (!UUID_REGEX.test(id)) return { uuidLine: id };
    return null;
  };
}

@Component({
  selector: 'qos-ropa-dialog',
  templateUrl: './ropa-dialog.component.html',
  styleUrls: ['./ropa-dialog.component.scss'],
  standalone: false
})
export class RopaDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  readonly bases: { value: LawfulBasis; label: string; needsLia?: boolean }[] = [
    { value: 'CONSENT',              label: 'Consentement (Art. 6.1.a)' },
    { value: 'CONTRACT',             label: 'Exécution d\'un contrat (Art. 6.1.b)' },
    { value: 'LEGAL_OBLIGATION',     label: 'Obligation légale (Art. 6.1.c)' },
    { value: 'VITAL_INTERESTS',      label: 'Intérêts vitaux (Art. 6.1.d)' },
    { value: 'PUBLIC_TASK',          label: 'Mission de service public (Art. 6.1.e)' },
    { value: 'LEGITIMATE_INTERESTS', label: 'Intérêt légitime (Art. 6.1.f) — LIA requise', needsLia: true }
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: RopaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<RopaDialogComponent, ProcessingActivityView>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: RopaDialogData | null
  ) {
    this.isEdit = !!data?.activity;
    const a = data?.activity;
    this.form = this.fb.nonNullable.group({
      // OWASP A03 — reference regex + length mirror backend @Pattern + @Size.
      // Disabled in edit (matches backend invariant: reference is immutable).
      reference: [
        { value: a?.reference ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(64),
         Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]
      ],
      name:     [a?.name ?? '',     [Validators.required, Validators.maxLength(250)]],
      purposes: [a?.purposes ?? '', [Validators.required, Validators.maxLength(4000)]],

      lawfulBasis:        [(a?.lawfulBasis ?? 'CONTRACT') as LawfulBasis, [Validators.required]],
      lawfulBasisDetails: [a?.lawfulBasisDetails ?? '',  [Validators.maxLength(4000)]],

      controllerName:    [a?.controllerName    ?? '', [Validators.required, Validators.maxLength(250)]],
      controllerContact: [a?.controllerContact ?? '', [Validators.required, Validators.maxLength(250)]],
      dpoContact:        [a?.dpoContact        ?? '', [Validators.maxLength(250)]],
      jointControllerName:    [a?.jointControllerName    ?? '', [Validators.maxLength(250)]],
      jointControllerContact: [a?.jointControllerContact ?? '', [Validators.maxLength(250)]],

      dataSubjectCategories: [listToLines(a?.dataSubjectCategories)],
      dataCategories:        [listToLines(a?.dataCategories)],
      recipientCategories:   [listToLines(a?.recipientCategories)],
      thirdCountryTransfers: [listToLines(a?.thirdCountryTransfers)],

      specialCategoriesProcessed:    [a?.specialCategoriesProcessed ?? false],
      specialCategoriesJustification:[a?.specialCategoriesJustification ?? '', [Validators.maxLength(4000)]],

      transferSafeguards:    [a?.transferSafeguards    ?? '', [Validators.maxLength(4000)]],
      technicalMeasures:     [a?.technicalMeasures     ?? '', [Validators.maxLength(4000)]],
      organizationalMeasures:[a?.organizationalMeasures?? '', [Validators.maxLength(4000)]],

      linkedRetentionRuleIds: [listToLines(a?.linkedRetentionRuleIds), [uuidLinesValidator()]]
    });
  }

  needsLia(): boolean { return this.form.controls.lawfulBasis.value === 'LEGITIMATE_INTERESTS'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }

    const v = this.form.getRawValue();

    // OWASP A04 — refuse logically incomplete RoPA entries.
    // 1. Special categories processed → justification required (RGPD Art. 9§2).
    if (v.specialCategoriesProcessed && !v.specialCategoriesJustification?.trim()) {
      this.snack.open(
        'Une justification est requise lorsque des catégories particulières (Art. 9) sont traitées.',
        'OK', { duration: 4500 }
      );
      return;
    }
    // 2. Third-country transfer present → safeguards mandatory (RGPD Chap. V).
    const transfers = linesToList(v.thirdCountryTransfers);
    if (transfers.length > 0 && !v.transferSafeguards?.trim()) {
      this.snack.open(
        'Les transferts hors UE exigent des garanties documentées (Chap. V RGPD).',
        'OK', { duration: 4500 }
      );
      return;
    }
    // 3. LIA mandatory for legitimate interests basis.
    if (v.lawfulBasis === 'LEGITIMATE_INTERESTS' && !v.lawfulBasisDetails?.trim()) {
      this.snack.open(
        'L\'intérêt légitime exige une mise en balance (LIA) documentée.',
        'OK', { duration: 4500 }
      );
      return;
    }

    this.submitting = true;

    const payload = {
      name: v.name.trim(),
      purposes: v.purposes.trim(),
      lawfulBasis: v.lawfulBasis,
      lawfulBasisDetails: v.lawfulBasisDetails?.trim() || undefined,
      controllerName:    v.controllerName.trim(),
      controllerContact: v.controllerContact.trim(),
      dpoContact:        v.dpoContact?.trim()        || undefined,
      jointControllerName:    v.jointControllerName?.trim()    || undefined,
      jointControllerContact: v.jointControllerContact?.trim() || undefined,
      dataSubjectCategories: linesToList(v.dataSubjectCategories),
      dataCategories:        linesToList(v.dataCategories),
      specialCategoriesProcessed: v.specialCategoriesProcessed,
      specialCategoriesJustification: v.specialCategoriesJustification?.trim() || undefined,
      recipientCategories:   linesToList(v.recipientCategories),
      thirdCountryTransfers: transfers,
      transferSafeguards:    v.transferSafeguards?.trim() || undefined,
      linkedRetentionRuleIds: linesToList(v.linkedRetentionRuleIds),
      technicalMeasures:      v.technicalMeasures?.trim()      || undefined,
      organizationalMeasures: v.organizationalMeasures?.trim() || undefined
    };

    const op$ = this.isEdit
      ? this.svc.edit(this.data!.activity!.id, payload)
      : (() => {
          const createdByUserId = this.auth.snapshot()?.userId;
          if (!createdByUserId) {
            this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.create({
            reference: v.reference.trim(),
            ...payload,
            createdByUserId
          });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => {
          this.snack.open(this.isEdit ? 'Activité mise à jour.' : 'Activité créée (DRAFT).', 'OK', { duration: 2500 });
          this.dialogRef.close(a);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ropa-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
