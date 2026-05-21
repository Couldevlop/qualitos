import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ChangesService } from '../../changes.service';
import { ApprovalDecision, ApprovalResponse } from '../../changes.types';

export interface ChangesDecisionDialogData {
  changeId: string;
  decision: 'APPROVED' | 'REJECTED';
}

@Component({
  selector: 'qos-changes-decision-dialog',
  templateUrl: './changes-decision-dialog.component.html',
  styleUrls: ['./changes-decision-dialog.component.scss'],
  standalone: false
})
export class ChangesDecisionDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    comment: ['', this.data.decision === 'REJECTED'
      ? [Validators.required, Validators.maxLength(1000)]
      : [Validators.maxLength(1000)]
    ]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ChangesService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ChangesDecisionDialogComponent, ApprovalResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ChangesDecisionDialogData
  ) {}

  get title(): string { return this.data.decision === 'APPROVED' ? 'Approuver la demande' : 'Rejeter la demande'; }
  get danger(): boolean { return this.data.decision === 'REJECTED'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const approverUserId = this.auth.snapshot()?.userId;
    if (!approverUserId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.decide(this.data.changeId, {
      approverUserId,
      decision: this.data.decision as ApprovalDecision,
      comment: v.comment?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => {
          this.snack.open(this.data.decision === 'APPROVED' ? 'Décision enregistrée : approuvée.' : 'Décision enregistrée : rejetée.', 'OK', { duration: 2500 });
          this.dialogRef.close(a);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[changes-decision] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Décision impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
