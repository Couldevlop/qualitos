import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CalibrationService, toLocalDate } from '../../calibration.service';
import { MsaResponse, MsaResult, MsaType } from '../../calibration.types';

export interface CalibrationMsaDialogData {
  equipmentId: string;
}

/**
 * Étude MSA (Measurement System Analysis — AIAG / IATF 16949, §4.10).
 *
 * Le verdict est saisi par l'analyste : le serveur ne le déduit pas du couple
 * valeur/seuil, il enregistre la conclusion telle qu'elle est prononcée.
 */
@Component({
  selector: 'qos-calibration-msa-dialog',
  templateUrl: './calibration-msa-dialog.component.html',
  styleUrls: ['./calibration-msa-dialog.component.scss'],
  standalone: false
})
export class CalibrationMsaDialogComponent {

  submitting = false;

  readonly types: { value: MsaType; label: string }[] = [
    { value: 'GAGE_R_R', label: $localize`:@@calibration.msa.type.gage-rr:R&R (répétabilité/reproductibilité)` },
    { value: 'BIAS', label: $localize`:@@calibration.msa.type.bias:Justesse (biais)` },
    { value: 'LINEARITY', label: $localize`:@@calibration.msa.type.linearity:Linéarité` },
    { value: 'STABILITY', label: $localize`:@@calibration.msa.type.stability:Stabilité` }
  ];

  readonly results: { value: MsaResult; label: string }[] = [
    { value: 'PASS', label: $localize`:@@calibration.msa.result.pass:Acceptable` },
    { value: 'MARGINAL', label: $localize`:@@calibration.msa.result.marginal:Marginal` },
    { value: 'FAIL', label: $localize`:@@calibration.msa.result.fail:Inacceptable` }
  ];

  readonly form = this.fb.nonNullable.group({
    type: ['GAGE_R_R' as MsaType, [Validators.required]],
    performedOn: [toLocalDate(new Date()), [Validators.required]],
    studyValue: [null as number | null, [Validators.required]],
    passingThreshold: [null as number | null, []],
    result: ['PASS' as MsaResult, [Validators.required]],
    notes: ['', [Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CalibrationService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CalibrationMsaDialogComponent, MsaResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CalibrationMsaDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    // Le serveur exige un `createdBy` : sans session valide, l'appel échouerait en 400.
    const createdBy = this.auth.snapshot()?.userId;
    if (!createdBy) {
      this.snack.open(
        $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.addMsa(this.data.equipmentId, {
      type: v.type,
      performedOn: v.performedOn,
      studyValue: Number(v.studyValue),
      passingThreshold: v.passingThreshold === null ? undefined : Number(v.passingThreshold),
      result: v.result,
      notes: v.notes.trim() || undefined,
      createdBy
    })
      .pipe(finalize(() => { this.submitting = false; }))
      .subscribe({
        next: msa => {
          this.snack.open($localize`:@@calibration.msa.saved:Étude MSA enregistrée.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(msa);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[calibration-msa] failed', (err as { status?: number })?.status);
          this.snack.open(safeErrorMessage(err,
            $localize`:@@calibration.save-error:Erreur lors de l'enregistrement.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
