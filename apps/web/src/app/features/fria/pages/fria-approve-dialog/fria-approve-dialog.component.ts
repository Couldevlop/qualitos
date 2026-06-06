import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FriaService } from '../../fria.service';
import { FriaView } from '../../fria.types';

export interface FriaApproveDialogData { id: string; }

@Component({
  selector: 'qos-fria-approve-dialog',
  templateUrl: './fria-approve-dialog.component.html',
  styleUrls: ['./fria-approve-dialog.component.scss'],
  standalone: false
})
export class FriaApproveDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    approvalNotes: ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: FriaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FriaApproveDialogComponent, FriaView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: FriaApproveDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.approve(this.data.id, {
      approvedByUserId: userId,
      approvalNotes: this.form.getRawValue().approvalNotes?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: f => { this.snack.open($localize`:@@fria.approve.approved:FRIA approuvée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(f); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fria-approve] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@fria.approve.approve-failed:Approbation impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
