import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { FivesService } from '../../fives.service';
import { FiveSAuditResponse } from '../../fives.types';

@Component({
  selector: 'qos-fives-create-dialog',
  templateUrl: './fives-create-dialog.component.html',
  styleUrls: ['./fives-create-dialog.component.scss'],
  standalone: false
})
export class FivesCreateDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    zone: ['', [Validators.required, Validators.maxLength(200)]],
    description: [''],
    scheduledAt: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly fives: FivesService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FivesCreateDialogComponent, FiveSAuditResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const auditorId = this.auth.snapshot()?.userId;
    if (!auditorId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const { zone, description, scheduledAt } = this.form.getRawValue();
    this.fives
      .createAudit({
        zone: zone.trim(),
        description: description?.trim() || undefined,
        auditorId,
        scheduledAt: scheduledAt ? new Date(scheduledAt).toISOString() : undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: audit => {
          this.snack.open('Audit 5S créé.', 'OK', { duration: 2500 });
          this.dialogRef.close(audit);
        },
        error: err => {
          this.snack.open(
            err?.error?.message ?? err?.message ?? 'Erreur lors de la création',
            'OK',
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
