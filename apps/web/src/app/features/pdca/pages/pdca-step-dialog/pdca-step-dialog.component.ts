import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { PdcaService } from '../../pdca.service';
import { PdcaPhase, PdcaStepResponse } from '../../pdca.types';

export interface PdcaStepDialogData {
  cycleId: string;
  /** When the cycle is on a specific phase, default the form there. */
  defaultPhase?: PdcaPhase;
}

@Component({
  selector: 'qos-pdca-step-dialog',
  templateUrl: './pdca-step-dialog.component.html',
  styleUrls: ['./pdca-step-dialog.component.scss'],
  standalone: false
})
export class PdcaStepDialogComponent {

  submitting = false;

  readonly phases: PdcaPhase[] = ['PLAN', 'DO', 'CHECK', 'ACT'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    phase: ['PLAN' as PdcaPhase, [Validators.required]],
    dueDate: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly pdca: PdcaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PdcaStepDialogComponent, PdcaStepResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: PdcaStepDialogData
  ) {
    if (data.defaultPhase) {
      this.form.controls.phase.setValue(data.defaultPhase);
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { title, description, phase, dueDate } = this.form.getRawValue();
    this.pdca
      .addStep(this.data.cycleId, {
        title: title.trim(),
        description: description?.trim() || undefined,
        phase,
        dueDate: dueDate || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: step => {
          this.snack.open($localize`:@@pdca.step.success:Étape ajoutée.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(step);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-step-dialog] addStep failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@pdca.step.add-error:Erreur lors de l'ajout.`),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
