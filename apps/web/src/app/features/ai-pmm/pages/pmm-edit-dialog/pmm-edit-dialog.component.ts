import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { PmmService } from '../../pmm.service';
import { FREQUENCY_LABEL, PmmPlanView, PmmReviewFrequency } from '../../pmm.types';

export interface PmmEditDialogData { row: PmmPlanView; }

@Component({
  selector: 'qos-pmm-edit-dialog',
  templateUrl: './pmm-edit-dialog.component.html',
  styleUrls: ['./pmm-edit-dialog.component.scss'],
  standalone: false
})
export class PmmEditDialogComponent {

  submitting = false;
  readonly freqLabel = FREQUENCY_LABEL;
  readonly frequencies: PmmReviewFrequency[] = ['WEEKLY', 'MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL'];

  readonly form = this.fb.nonNullable.group({
    name:                        [this.data.row.name,                            [Validators.required, Validators.maxLength(250)]],
    description:                 [this.data.row.description ?? '',               [Validators.maxLength(4000)]],
    metricsMonitored:            [this.data.row.metricsMonitored ?? '',          [Validators.maxLength(4000)]],
    collectionMethod:            [this.data.row.collectionMethod ?? '',          [Validators.maxLength(4000)]],
    reviewFrequency:             [this.data.row.reviewFrequency,                 [Validators.required]],
    responsiblePartyDescription: [this.data.row.responsiblePartyDescription ?? '', [Validators.maxLength(4000)]],
    triggerCriteria:             [this.data.row.triggerCriteria ?? '',           [Validators.maxLength(4000)]],
    qmsLinkReference:            [this.data.row.qmsLinkReference ?? '',          [Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: PmmService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PmmEditDialogComponent, PmmPlanView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: PmmEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.edit(this.data.row.id, {
      name: v.name.trim(),
      description: v.description?.trim() || undefined,
      metricsMonitored: v.metricsMonitored?.trim() || undefined,
      collectionMethod: v.collectionMethod?.trim() || undefined,
      reviewFrequency: v.reviewFrequency,
      responsiblePartyDescription: v.responsiblePartyDescription?.trim() || undefined,
      triggerCriteria: v.triggerCriteria?.trim() || undefined,
      qmsLinkReference: v.qmsLinkReference?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => { this.snack.open($localize`:@@ai-pmm.edit.updated:Plan mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(p); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pmm-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@ai-pmm.edit.update-failed:Mise à jour impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
