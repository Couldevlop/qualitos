import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CalibrationService, toLocalDate } from '../../calibration.service';
import { CalibrationResult, RecordResponse } from '../../calibration.types';

export interface CalibrationRecordDialogData {
  equipmentId: string;
  /** Sert uniquement à avertir : un FAIL met un instrument critique hors service. */
  critical: boolean;
}

/**
 * Saisie d'un enregistrement de calibration (§4.10).
 *
 * Deux effets de bord côté serveur sont annoncés dans l'écran plutôt que subis :
 * l'échéance du plan est recalculée (`performedOn + périodicité`, sauf date forcée),
 * et un résultat non conforme met un instrument critique hors service.
 */
@Component({
  selector: 'qos-calibration-record-dialog',
  templateUrl: './calibration-record-dialog.component.html',
  styleUrls: ['./calibration-record-dialog.component.scss'],
  standalone: false
})
export class CalibrationRecordDialogComponent {

  submitting = false;

  readonly results: { value: CalibrationResult; label: string }[] = [
    { value: 'PASS', label: $localize`:@@calibration.result.pass:Conforme` },
    { value: 'CONDITIONAL', label: $localize`:@@calibration.result.conditional:Conforme sous condition` },
    { value: 'FAIL', label: $localize`:@@calibration.result.fail:Non conforme` }
  ];

  readonly form = this.fb.nonNullable.group({
    performedOn: [toLocalDate(new Date()), [Validators.required]],
    result: ['PASS' as CalibrationResult, [Validators.required]],
    performedByMe: [true],
    performedByOrg: ['', [Validators.maxLength(250)]],
    certificateReference: ['', [Validators.maxLength(250)]],
    measurements: ['', [Validators.maxLength(4000)]],
    nextDueOverride: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CalibrationService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CalibrationRecordDialogComponent, RecordResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CalibrationRecordDialogData
  ) {}

  /** Avertissement affiché quand l'enregistrement va déclasser l'instrument. */
  get failWarning(): boolean {
    return this.data.critical && this.form.controls.result.value === 'FAIL';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.submitting = true;
    this.svc.addRecord(this.data.equipmentId, {
      performedOn: v.performedOn,
      result: v.result,
      // Traçabilité de l'opérateur : seulement s'il déclare avoir réalisé la mesure,
      // sinon la calibration est le fait d'un prestataire et l'utilisateur n'est
      // que le saisisseur.
      performedByUserId: v.performedByMe ? (this.auth.snapshot()?.userId ?? undefined) : undefined,
      performedByOrg: v.performedByOrg.trim() || undefined,
      certificateReference: v.certificateReference.trim() || undefined,
      measurements: v.measurements.trim() || undefined,
      nextDueOverride: v.nextDueOverride || undefined
    })
      .pipe(finalize(() => { this.submitting = false; }))
      .subscribe({
        next: record => {
          this.snack.open($localize`:@@calibration.record.saved:Calibration enregistrée.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(record);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[calibration-record] failed', (err as { status?: number })?.status);
          this.snack.open(safeErrorMessage(err,
            $localize`:@@calibration.save-error:Erreur lors de l'enregistrement.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
