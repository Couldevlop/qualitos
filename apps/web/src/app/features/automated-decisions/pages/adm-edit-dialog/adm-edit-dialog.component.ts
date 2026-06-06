import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AdmService } from '../../adm.service';
import { AdmType, AdmView, Art22Basis, BASIS_LABEL, TYPE_LABEL } from '../../adm.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function uuidLinesValidator(): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const v = (c.value ?? '').toString().trim();
    if (!v) return null;
    for (const l of v.split(/\r?\n/).map((s: string) => s.trim()).filter((s: string) => s)) {
      if (!UUID_REGEX.test(l)) return { lines: $localize`:@@automated-decisions.create.uuid-per-line:Un UUID par ligne.` };
    }
    return null;
  };
}

export interface AdmEditDialogData { row: AdmView; }

@Component({
  selector: 'qos-adm-edit-dialog',
  templateUrl: './adm-edit-dialog.component.html',
  styleUrls: ['./adm-edit-dialog.component.scss'],
  standalone: false
})
export class AdmEditDialogComponent {

  submitting = false;
  readonly types: AdmType[] = ['PROFILING_ONLY', 'AUTOMATED_DECISION', 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'];
  readonly bases: Art22Basis[] = ['EXPLICIT_CONSENT', 'CONTRACTUAL_NECESSITY', 'AUTHORIZED_BY_LAW'];
  readonly typeLabel = TYPE_LABEL;
  readonly basisLabel = BASIS_LABEL;

  readonly humanReviewLabel         = $localize`:@@automated-decisions.edit.human-review:Révision humaine`;
  readonly humanReviewLabelRequired = $localize`:@@automated-decisions.edit.human-review-required:Révision humaine (Art. 22.3 — obligatoire)`;

  readonly form = this.fb.nonNullable.group({
    name:                   [this.data.row.name,                              [Validators.required, Validators.maxLength(250)]],
    description:            [this.data.row.description ?? '',                 [Validators.maxLength(4000)]],
    decisionType:           [this.data.row.decisionType,                      [Validators.required]],
    art22LawfulBasis:       [(this.data.row.art22LawfulBasis ?? '') as Art22Basis | '', []],
    lawfulBasisDetails:     [this.data.row.lawfulBasisDetails ?? '',          [Validators.maxLength(4000)]],
    inputDataCategoriesRaw: [(this.data.row.inputDataCategories ?? []).join('\n'),                [Validators.maxLength(4000)]],
    linkedActivitiesRaw:    [(this.data.row.linkedProcessingActivityIds ?? []).join('\n'),        [uuidLinesValidator()]],
    linkedDpiaId:           [this.data.row.linkedDpiaId ?? '',                [Validators.pattern(new RegExp(`^$|${UUID_REGEX.source}`, 'i'))]],
    algorithmDescription:   [this.data.row.algorithmDescription ?? '',        [Validators.maxLength(8000)]],
    significanceForSubject: [this.data.row.significanceForSubject ?? '',      [Validators.maxLength(4000)]],
    humanReviewMechanism:   [this.data.row.humanReviewMechanism ?? '',        [Validators.maxLength(4000)]],
    objectionMechanism:     [this.data.row.objectionMechanism ?? '',          [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AdmService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AdmEditDialogComponent, AdmView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AdmEditDialogData
  ) {}

  isLegalEffect(): boolean { return this.form.controls.decisionType.value === 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (v.decisionType === 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT') {
      if (!v.art22LawfulBasis) {
        this.snack.open($localize`:@@automated-decisions.edit.art22-basis-required:Art. 22.2 — base légale obligatoire.`, $localize`:@@common.ok:OK`, { duration: 5000 });
        return;
      }
      if (!v.humanReviewMechanism?.trim()) {
        this.snack.open($localize`:@@automated-decisions.create.art22-human-review-required:Art. 22.3 — mécanisme de révision humaine obligatoire.`, $localize`:@@common.ok:OK`, { duration: 5000 });
        return;
      }
    }
    this.submitting = true;
    this.svc.edit(this.data.row.id, {
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
      objectionMechanism: v.objectionMechanism?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => { this.snack.open($localize`:@@automated-decisions.edit.updated-toast:Décision mise à jour.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(r); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[adm-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@automated-decisions.edit.update-failed:Mise à jour impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }

  private lines(raw: string): string[] {
    return (raw ?? '').split(/\r?\n/).map(s => s.trim()).filter(s => s);
  }
}
