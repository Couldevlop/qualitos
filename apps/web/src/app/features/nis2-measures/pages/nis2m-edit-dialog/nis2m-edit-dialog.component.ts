import { Component, Inject } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { Nis2MeasuresService } from '../../nis2m.service';
import { Nis2MeasureView, ResidualRiskRating } from '../../nis2m.types';

const URL_LINE = /^https?:\/\/[^\s]+$/;
const UUID_LINE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function linesValidator(re: RegExp, msg: string): ValidatorFn {
  return (c: AbstractControl): ValidationErrors | null => {
    const v = (c.value ?? '').toString().trim();
    if (!v) return null;
    for (const l of v.split(/\r?\n/).map((s: string) => s.trim()).filter((s: string) => s)) {
      if (!re.test(l)) return { lines: msg };
    }
    return null;
  };
}

export interface Nis2mEditDialogData { row: Nis2MeasureView; }

@Component({
  selector: 'qos-nis2m-edit-dialog',
  templateUrl: './nis2m-edit-dialog.component.html',
  styleUrls: ['./nis2m-edit-dialog.component.scss'],
  standalone: false
})
export class Nis2mEditDialogComponent {

  submitting = false;
  readonly risks: ResidualRiskRating[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    title:                     [this.data.row.title,                          [Validators.required, Validators.maxLength(250)]],
    description:               [this.data.row.description ?? '',              [Validators.maxLength(4000)]],
    ownerUserId:               [this.data.row.ownerUserId ?? '',              [Validators.pattern(/^$|^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)]],
    maturityLevel:             [this.data.row.maturityLevel,                  [Validators.required, Validators.min(1), Validators.max(5)]],
    residualRiskRating:        [this.data.row.residualRiskRating,             [Validators.required]],
    criticalRiskJustification: [this.data.row.criticalRiskJustification ?? '',[Validators.maxLength(4000)]],
    reviewIntervalDays:        [this.data.row.reviewIntervalDays,             [Validators.required, Validators.min(30), Validators.max(1095)]],
    evidenceUrlsRaw:           [(this.data.row.evidenceUrls ?? []).join('\n'),                 [linesValidator(URL_LINE, $localize`:@@nis2-measures.plan.url-per-line:Une URL http(s) par ligne.`)]],
    linkedActivitiesRaw:       [(this.data.row.linkedProcessingActivityIds ?? []).join('\n'),  [linesValidator(UUID_LINE, $localize`:@@nis2-measures.plan.uuid-per-line:Un UUID par ligne.`)]],
    linkedAgreementsRaw:       [(this.data.row.linkedProcessorAgreementIds ?? []).join('\n'),  [linesValidator(UUID_LINE, $localize`:@@nis2-measures.plan.uuid-per-line:Un UUID par ligne.`)]],
    notes:                     [this.data.row.notes ?? '',                    [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: Nis2MeasuresService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<Nis2mEditDialogComponent, Nis2MeasureView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: Nis2mEditDialogData
  ) {}

  isCritical(): boolean { return this.form.controls.residualRiskRating.value === 'CRITICAL'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (v.residualRiskRating === 'CRITICAL' && !v.criticalRiskJustification.trim()) {
      this.snack.open($localize`:@@nis2-measures.plan.critical-required:Justification obligatoire pour un risque résiduel CRITICAL.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.edit(this.data.row.id, {
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      ownerUserId: v.ownerUserId?.trim() || undefined,
      maturityLevel: v.maturityLevel,
      residualRiskRating: v.residualRiskRating,
      criticalRiskJustification: v.criticalRiskJustification?.trim() || undefined,
      reviewIntervalDays: v.reviewIntervalDays,
      evidenceUrls: this.lines(v.evidenceUrlsRaw),
      linkedProcessingActivityIds: this.lines(v.linkedActivitiesRaw),
      linkedProcessorAgreementIds: this.lines(v.linkedAgreementsRaw),
      notes: v.notes?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: m => { this.snack.open($localize`:@@nis2-measures.edit.updated:Mesure mise à jour.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(m); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[nis2m-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@nis2-measures.edit.update-failed:Mise à jour impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }

  private lines(raw: string): string[] {
    return (raw ?? '').split(/\r?\n/).map(s => s.trim()).filter(s => s);
  }
}
