import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpoAppointmentsService } from '../../dpo-appointments.service';
import { DpoAppointmentView } from '../../dpo-appointments.types';

export interface DpoActivateDialogData { appointment: DpoAppointmentView; }

@Component({
  selector: 'qos-dpo-activate-dialog',
  templateUrl: './dpo-activate-dialog.component.html',
  styleUrls: ['./dpo-activate-dialog.component.scss'],
  standalone: false
})
export class DpoActivateDialogComponent {

  submitting = false;

  readonly today = new Date().toISOString().slice(0, 10);

  readonly form = this.fb.nonNullable.group({
    effectiveFrom:                  [this.today, [Validators.required]],
    regulatorNotifiedAt:            ['', [Validators.required]],
    regulatorNotificationReference: ['', [Validators.required, Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DpoAppointmentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DpoActivateDialogComponent, DpoAppointmentView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DpoActivateDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    // OWASP A04 — la notification doit être antérieure ou simultanée à l'activation
    if (new Date(v.regulatorNotifiedAt) > new Date(v.effectiveFrom)) {
      this.snack.open(
        $localize`:@@dpo-appointments.activate.notif-before-effect:La notification à l'autorité doit être antérieure ou simultanée à la prise d'effet (Art. 37§7).`,
        $localize`:@@common.ok:OK`, { duration: 4500 }
      );
      return;
    }
    this.submitting = true;
    this.svc.activate(this.data.appointment.id, {
      effectiveFrom: new Date(v.effectiveFrom).toISOString(),
      regulatorNotifiedAt: new Date(v.regulatorNotifiedAt).toISOString(),
      regulatorNotificationReference: v.regulatorNotificationReference.trim()
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => { this.snack.open($localize`:@@dpo-appointments.activate.activated-toast:Désignation activée.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(a); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dpo-activate] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@dpo-appointments.activate.failed:Activation impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
