import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DmaicService } from '../../dmaic.service';
import { MeasureResponse } from '../../dmaic.types';

export interface DmaicMeasureDialogData {
  projectId: string;
}

@Component({
  selector: 'qos-dmaic-measure-dialog',
  templateUrl: './dmaic-measure-dialog.component.html',
  styleUrls: ['./dmaic-measure-dialog.component.scss'],
  standalone: false
})
export class DmaicMeasureDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    value: [null as number | null, [Validators.required]],
    subgroupId: ['', [Validators.maxLength(100)]],
    sourceRef:  ['', [Validators.maxLength(255)]],
    recordedAt: [''],
    note: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DmaicService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DmaicMeasureDialogComponent, MeasureResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DmaicMeasureDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.value === null) {
      this.snack.open($localize`:@@dmaic.measure.value-required:Une valeur numérique est requise.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const operatorId = this.auth.snapshot()?.userId;
    this.submitting = true;
    this.svc.addMeasure(this.data.projectId, {
      value: v.value,
      subgroupId: v.subgroupId?.trim() || undefined,
      sourceRef:  v.sourceRef?.trim()  || undefined,
      recordedAt: v.recordedAt || undefined,
      operatorId,
      note: v.note?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: m => {
          this.snack.open($localize`:@@dmaic.measure.saved:Mesure enregistrée.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(m);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dmaic-measure] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@dmaic.measure.error-save:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
