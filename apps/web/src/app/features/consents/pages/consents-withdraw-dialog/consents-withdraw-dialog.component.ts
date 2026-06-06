import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConsentsService } from '../../consents.service';
import { ConsentView } from '../../consents.types';

export interface ConsentsWithdrawDialogData { consent: ConsentView; }

@Component({
  selector: 'qos-consents-withdraw-dialog',
  templateUrl: './consents-withdraw-dialog.component.html',
  styleUrls: ['./consents-withdraw-dialog.component.scss'],
  standalone: false
})
export class ConsentsWithdrawDialogComponent {

  submitting = false;

  readonly pseudonymizedLabel = $localize`:@@consents.withdraw.pseudonymized:(pseudonymisée)`;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ConsentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ConsentsWithdrawDialogComponent, ConsentView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ConsentsWithdrawDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const actorUserId = this.auth.snapshot()?.userId;
    if (!actorUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.withdraw(this.data.consent.id, {
      actorUserId,
      reason: v.reason?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          this.snack.open($localize`:@@consents.withdraw.success:Consentement retiré.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[consents-withdraw] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@consents.withdraw.error:Retrait impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
