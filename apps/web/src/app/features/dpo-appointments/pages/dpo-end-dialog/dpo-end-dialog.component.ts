import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpoAppointmentsService } from '../../dpo-appointments.service';
import { DpoAppointmentView } from '../../dpo-appointments.types';

export interface DpoEndDialogData {
  appointment: DpoAppointmentView;
  mode: 'END' | 'CANCEL';
}

@Component({
  selector: 'qos-dpo-end-dialog',
  templateUrl: './dpo-end-dialog.component.html',
  styleUrls: ['./dpo-end-dialog.component.scss'],
  standalone: false
})
export class DpoEndDialogComponent {

  submitting = false;
  readonly today = new Date().toISOString().slice(0, 10);

  readonly form = this.fb.nonNullable.group({
    reason:      ['', [Validators.required, Validators.maxLength(2000)]],
    effectiveTo: [this.today, this.data.mode === 'END' ? [Validators.required] : []]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DpoAppointmentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DpoEndDialogComponent, DpoAppointmentView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DpoEndDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'END'
      ? $localize`:@@dpo-appointments.end.title-end:Clôturer la désignation (départ / fin de mandat)`
      : $localize`:@@dpo-appointments.end.title-cancel:Annuler la désignation (avant prise d'effet)`;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    const op$ = this.data.mode === 'END'
      ? this.svc.end(this.data.appointment.id, {
          reason: v.reason.trim(),
          effectiveTo: new Date(v.effectiveTo).toISOString()
        })
      : this.svc.cancel(this.data.appointment.id, { reason: v.reason.trim() });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => {
          this.snack.open(this.data.mode === 'END'
            ? $localize`:@@dpo-appointments.end.ended-toast:Désignation clôturée.`
            : $localize`:@@dpo-appointments.end.cancelled-toast:Désignation annulée.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(a);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dpo-end] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@dpo-appointments.end.failed:Opération impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
