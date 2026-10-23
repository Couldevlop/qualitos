import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { personName } from '../../../../core/directory/directory-names';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DocumentsService } from '../../documents.service';
import { DocumentVersionResponse, SignatoryRequest } from '../../documents.types';

export interface DocumentsSubmitDialogData {
  version: DocumentVersionResponse;
}

/** Les trois places du circuit de signature, dans l'ordre où l'on signe. */
export const SIGNATORY_SLOTS = ['s1', 's2', 's3'] as const;
export type SignatorySlot = typeof SIGNATORY_SLOTS[number];

/** La même personne ne signe pas deux fois. */
function distinct(group: AbstractControl): ValidationErrors | null {
  const ids = SIGNATORY_SLOTS.map(k => group.get(k)?.value).filter((v): v is string => !!v);
  return new Set(ids).size === ids.length ? null : { duplicate: true };
}

/**
 * Soumettre une version à signature (ADR 0099) : de une à trois personnes,
 * choisies dans l'annuaire, qui signeront dans l'ordre — le signataire 2 après
 * le 1, le 3 après le 2. L'auteur ne signe pas sa propre version.
 */
@Component({
  selector: 'qos-documents-submit-dialog',
  templateUrl: './documents-submit-dialog.component.html',
  styleUrls: ['./documents-submit-dialog.component.scss'],
  standalone: false
})
export class DocumentsSubmitDialogComponent {

  readonly slots = SIGNATORY_SLOTS;
  submitting = false;

  readonly form = this.fb.group({
    s1: this.fb.control<string | null>(null, Validators.required),
    s2: this.fb.control<string | null>(null),
    s3: this.fb.control<string | null>(null)
  }, { validators: distinct });

  /** Le nom de chaque personne choisie, tel que l'annuaire le donne. */
  private readonly names: Partial<Record<SignatorySlot, string>> = {};

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DocumentsService,
    private readonly snack: MatSnackBar,
    private readonly auth: AuthService,
    private readonly dialogRef: MatDialogRef<DocumentsSubmitDialogComponent, DocumentVersionResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DocumentsSubmitDialogData
  ) {}

  get title(): string {
    return $localize`:@@documents.submit.title:Soumettre la version v${this.data.version.versionNumber}:number: à signature`;
  }

  label(i: number): string {
    return $localize`:@@documents.signatory.label:Signataire ${i + 1}:position:`;
  }

  /** Ni l'auteur, ni soi-même (on n'approuve pas seul ce qu'on soumet), ni une personne déjà placée. */
  excluded(slot: SignatorySlot): string[] {
    const me = this.auth.snapshot()?.userId;
    const others = SIGNATORY_SLOTS.filter(k => k !== slot)
      .map(k => this.form.controls[k].value).filter((v): v is string => !!v);
    return [this.data.version.authorId, ...(me && me !== this.data.version.authorId ? [me] : []), ...others];
  }

  onSelected(slot: SignatorySlot, user: DirectoryUser | null): void {
    if (user) {
      this.names[slot] = personName(user);
    } else {
      delete this.names[slot];
    }
  }

  /** Les signataires choisis, dans l'ordre des places, sans les places laissées vides. */
  signatories(): SignatoryRequest[] {
    return SIGNATORY_SLOTS
      .map(k => ({ userId: this.form.controls[k].value, name: this.names[k] }))
      .filter((s): s is SignatoryRequest => !!s.userId && !!s.name);
  }

  submit(): void {
    const chosen = this.signatories();
    if (this.form.invalid || !chosen.length || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.data.version;
    this.svc.submit(v.documentId, v.id, chosen)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: soumise => {
          this.snack.open($localize`:@@documents.submit.done:Version soumise : le signataire 1 est invité à signer.`,
            $localize`:@@common.ok:OK`, { duration: 3000 });
          this.dialogRef.close(soumise);
        },
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@documents.detail.submit-failed:Soumission impossible.`),
          $localize`:@@common.ok:OK`, { duration: 4000 })
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
