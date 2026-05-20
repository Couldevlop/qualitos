import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { FivesService } from '../../fives.service';
import { FiveSAuditResponse } from '../../fives.types';

export interface FivesEditDialogData {
  audit: FiveSAuditResponse;
}

@Component({
  selector: 'qos-fives-edit-dialog',
  templateUrl: './fives-edit-dialog.component.html',
  styleUrls: ['./fives-edit-dialog.component.scss'],
  standalone: false
})
export class FivesEditDialogComponent {
  submitting = false;

  readonly form = this.fb.nonNullable.group({
    zone: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    scheduledAt: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly fives: FivesService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FivesEditDialogComponent, FiveSAuditResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: FivesEditDialogData
  ) {
    // datetime-local needs format YYYY-MM-DDTHH:mm (no Z, no seconds)
    const sched = data.audit.scheduledAt ? data.audit.scheduledAt.slice(0, 16) : '';
    this.form.patchValue({
      zone: data.audit.zone,
      description: data.audit.description ?? '',
      scheduledAt: sched
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.fives
      .updateAudit(this.data.audit.id, {
        zone: v.zone.trim(),
        description: v.description?.trim() || undefined,
        scheduledAt: v.scheduledAt ? new Date(v.scheduledAt).toISOString() : undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => {
          this.snack.open('Audit mis à jour.', 'OK', { duration: 2500 });
          this.dialogRef.close(a);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-edit] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de la mise à jour.'),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
