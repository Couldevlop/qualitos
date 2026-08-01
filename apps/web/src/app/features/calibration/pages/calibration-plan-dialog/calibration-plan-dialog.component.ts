import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CalibrationService, addDays, toLocalDate } from '../../calibration.service';
import { PlanResponse } from '../../calibration.types';

export interface CalibrationPlanDialogData {
  equipmentId: string;
  /** `null` = création du plan ; sinon modification. */
  plan: PlanResponse | null;
}

/**
 * Plan de calibration d'un équipement (§4.10) — route `PUT …/plan` (upsert).
 *
 * `firstDueOn` n'est utilisé par le serveur que tant qu'aucune échéance n'est fixée :
 * ensuite ce sont les enregistrements de calibration qui pilotent `nextDueOn`. On le
 * pré-remplit donc avec l'échéance courante en édition, et on le dit à l'utilisateur
 * plutôt que de lui laisser croire qu'il peut décaler l'échéance ici.
 */
@Component({
  selector: 'qos-calibration-plan-dialog',
  templateUrl: './calibration-plan-dialog.component.html',
  styleUrls: ['./calibration-plan-dialog.component.scss'],
  standalone: false
})
export class CalibrationPlanDialogComponent {

  submitting = false;

  readonly isEdit: boolean;
  readonly title: string;

  readonly form = this.fb.nonNullable.group({
    frequencyMonths: [12, [Validators.required, Validators.min(1), Validators.max(120)]],
    firstDueOn: ['', [Validators.required]],
    procedureReference: ['', [Validators.maxLength(500)]],
    tolerance: ['', [Validators.maxLength(500)]],
    accreditationRef: ['', [Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CalibrationService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CalibrationPlanDialogComponent, PlanResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CalibrationPlanDialogData
  ) {
    this.isEdit = !!data.plan;
    this.title = this.isEdit
      ? $localize`:@@calibration.plan.edit-title:Modifier le plan de calibration`
      : $localize`:@@calibration.plan.create-title:Définir le plan de calibration`;
    const p = data.plan;
    this.form.patchValue({
      frequencyMonths: p?.frequencyMonths ?? 12,
      // Sans plan, l'échéance par défaut est dans un an : la périodicité proposée.
      firstDueOn: p?.nextDueOn ?? toLocalDate(addDays(new Date(), 365)),
      procedureReference: p?.procedureReference ?? '',
      tolerance: p?.tolerance ?? '',
      accreditationRef: p?.accreditationRef ?? ''
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.submitting = true;
    this.svc.upsertPlan(this.data.equipmentId, {
      frequencyMonths: v.frequencyMonths,
      firstDueOn: v.firstDueOn,
      procedureReference: v.procedureReference.trim() || undefined,
      tolerance: v.tolerance.trim() || undefined,
      accreditationRef: v.accreditationRef.trim() || undefined
    })
      .pipe(finalize(() => { this.submitting = false; }))
      .subscribe({
        next: plan => {
          this.snack.open($localize`:@@calibration.plan.saved:Plan enregistré.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(plan);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[calibration-plan] failed', (err as { status?: number })?.status);
          this.snack.open(safeErrorMessage(err,
            $localize`:@@calibration.save-error:Erreur lors de l'enregistrement.`),
            $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
