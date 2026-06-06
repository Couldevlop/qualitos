import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
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
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
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
          this.snack.open($localize`:@@fives.create.success:Audit 5S créé.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(audit);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-create] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`),
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
