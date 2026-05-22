import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { KpisService } from '../../kpis.service';
import { MeasurementResponse, MeasurementSource } from '../../kpis.types';

export interface KpisMeasurementDialogData {
  kpiId: string;
  defaultUnit?: string;
}

@Component({
  selector: 'qos-kpis-measurement-dialog',
  templateUrl: './kpis-measurement-dialog.component.html',
  styleUrls: ['./kpis-measurement-dialog.component.scss'],
  standalone: false
})
export class KpisMeasurementDialogComponent {

  submitting = false;

  readonly sources: MeasurementSource[] = ['MANUAL', 'COMPUTED', 'IMPORT', 'IOT_AGGREGATED'];

  readonly form = this.fb.nonNullable.group({
    periodStart: ['', [Validators.required]],
    periodEnd:   ['', [Validators.required]],
    value:       [null as number | null, [Validators.required]],
    unit:        [this.data.defaultUnit ?? '', [Validators.maxLength(32)]],
    source:      ['MANUAL' as MeasurementSource],
    notes:       ['', [Validators.maxLength(1000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: KpisService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<KpisMeasurementDialogComponent, MeasurementResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: KpisMeasurementDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (v.value === null) {
      this.snack.open('Une valeur numérique est requise.', 'OK', { duration: 4000 });
      return;
    }
    // OWASP A03 — refuse swapped period bounds at the form layer
    if (new Date(v.periodEnd) < new Date(v.periodStart)) {
      this.snack.open('La fin de période doit être ≥ au début.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.record(this.data.kpiId, {
      periodStart: new Date(v.periodStart).toISOString(),
      periodEnd:   new Date(v.periodEnd).toISOString(),
      value: v.value,
      unit:   v.unit?.trim() || undefined,
      source: v.source,
      recordedByUserId: this.auth.snapshot()?.userId,
      notes:  v.notes?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: m => { this.snack.open('Mesure enregistrée.', 'OK', { duration: 2500 }); this.dialogRef.close(m); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[kpis-measurement] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
