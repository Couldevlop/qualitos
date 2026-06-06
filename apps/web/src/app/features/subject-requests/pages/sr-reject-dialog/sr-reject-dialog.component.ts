import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SubjectRequestsService } from '../../subject-requests.service';
import { SubjectRequestView } from '../../subject-requests.types';

export interface SrRejectDialogData { requestId: string; }

@Component({
  selector: 'qos-sr-reject-dialog',
  templateUrl: './sr-reject-dialog.component.html',
  styleUrls: ['./sr-reject-dialog.component.scss'],
  standalone: false
})
export class SrRejectDialogComponent {

  submitting = false;

  // OWASP A03 + A04 — motif obligatoire pour traçabilité; aucun rejet "silencieux".
  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SubjectRequestsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SrRejectDialogComponent, SubjectRequestView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SrRejectDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.reject(this.data.requestId, {
      reason: v.reason.trim(),
      handledByUserId: this.auth.snapshot()?.userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => { this.snack.open($localize`:@@subject-requests.reject.success:Demande rejetée.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(r); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[sr-reject] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@subject-requests.reject.error:Rejet impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
