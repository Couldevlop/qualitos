import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CirclesService } from '../../circles.service';
import { CircleMeetingResponse } from '../../circles.types';

export interface CirclesMeetingDialogData {
  circleId: string;
}

@Component({
  selector: 'qos-circles-meeting-dialog',
  templateUrl: './circles-meeting-dialog.component.html',
  styleUrls: ['./circles-meeting-dialog.component.scss'],
  standalone: false
})
export class CirclesMeetingDialogComponent {
  submitting = false;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    agenda: [''],
    scheduledAt: ['', [Validators.required]],
    durationMinutes: [60, [Validators.min(1)]],
    location: ['', [Validators.maxLength(500)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly circles: CirclesService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CirclesMeetingDialogComponent, CircleMeetingResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CirclesMeetingDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.circles
      .addMeeting(this.data.circleId, {
        title: v.title.trim(),
        agenda: v.agenda?.trim() || undefined,
        scheduledAt: new Date(v.scheduledAt).toISOString(),
        durationMinutes: v.durationMinutes > 0 ? v.durationMinutes : undefined,
        location: v.location?.trim() || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: m => {
          this.snack.open('Réunion planifiée.', 'OK', { duration: 2500 });
          this.dialogRef.close(m);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[circles-meeting-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de la planification.'),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
