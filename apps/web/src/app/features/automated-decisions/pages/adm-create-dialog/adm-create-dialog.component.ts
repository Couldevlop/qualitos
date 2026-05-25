import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AdmService } from '../../adm.service';
import { AdmType, AdmView, Art22Basis, BASIS_LABEL, TYPE_LABEL } from '../../adm.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function uuidLinesValidator(): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const v = (c.value ?? '').toString().trim();
    if (!v) return null;
    for (const l of v.split(/\r?\n/).map((s: string) => s.trim()).filter((s: string) => s)) {
      if (!UUID_REGEX.test(l)) return { lines: 'Un UUID par ligne.' };
    }
    return null;
  };
}

@Component({
  selector: 'qos-adm-create-dialog',
  templateUrl: './adm-create-dialog.component.html',
  styleUrls: ['./adm-create-dialog.component.scss'],
  standalone: false
})
export class AdmCreateDialogComponent {

  submitting = false;
  readonly types: AdmType[] = ['PROFILING_ONLY', 'AUTOMATED_DECISION', 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'];
  readonly bases: Art22Basis[] = ['EXPLICIT_CONSENT', 'CONTRACTUAL_NECESSITY', 'AUTHORIZED_BY_LAW'];
  readonly typeLabel = TYPE_LABEL;
  readonly basisLabel = BASIS_LABEL;

  readonly form = this.fb.nonNullable.group({
    reference:                   ['', [Validators.required, Validators.maxLength(64), Validators.pattern(/^[A-Z][A-Z0-9_-]{1,63}$/)]],
    name:                        ['', [Validators.required, Validators.maxLength(250)]],
    description:                 ['', [Validators.maxLength(4000)]],
    decisionType:                ['AUTOMATED_DECISION' as AdmType, [Validators.required]],
    art22LawfulBasis:            ['' as Art22Basis | '', []],
    lawfulBasisDetails:          ['', [Validators.maxLength(4000)]],
    inputDataCategoriesRaw:      ['', [Validators.maxLength(4000)]],
    linkedActivitiesRaw:         ['', [uuidLinesValidator()]],
    linkedDpiaId:                ['', [Validators.pattern(new RegExp(`^$|${UUID_REGEX.source}`, 'i'))]],
    algorithmDescription:        ['', [Validators.maxLength(8000)]],
    significanceForSubject:      ['', [Validators.maxLength(4000)]],
    humanReviewMechanism:        ['', [Validators.maxLength(4000)]],
    objectionMechanism:          ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AdmService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AdmCreateDialogComponent, AdmView>
  ) {}

  isLegalEffect(): boolean { return this.form.controls.decisionType.value === 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (v.decisionType === 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT') {
      if (!v.art22LawfulBasis) {
        this.snack.open('Art. 22.2 — base légale obligatoire pour une décision automatisée à effet juridique.', 'OK', { duration: 5000 });
        return;
      }
      if (!v.humanReviewMechanism?.trim()) {
        this.snack.open('Art. 22.3 — mécanisme de révision humaine obligatoire.', 'OK', { duration: 5000 });
        return;
      }
    }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.create({
      reference: v.reference.trim(),
      name: v.name.trim(),
      description: v.description?.trim() || undefined,
      decisionType: v.decisionType,
      art22LawfulBasis: v.art22LawfulBasis || undefined,
      lawfulBasisDetails: v.lawfulBasisDetails?.trim() || undefined,
      inputDataCategories: this.lines(v.inputDataCategoriesRaw),
      linkedProcessingActivityIds: this.lines(v.linkedActivitiesRaw),
      linkedDpiaId: v.linkedDpiaId?.trim() || undefined,
      algorithmDescription: v.algorithmDescription?.trim() || undefined,
      significanceForSubject: v.significanceForSubject?.trim() || undefined,
      humanReviewMechanism: v.humanReviewMechanism?.trim() || undefined,
      objectionMechanism: v.objectionMechanism?.trim() || undefined,
      createdByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => this.dialogRef.close(r),
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[adm-create] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Création impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }

  private lines(raw: string): string[] {
    return (raw ?? '').split(/\r?\n/).map(s => s.trim()).filter(s => s);
  }
}
