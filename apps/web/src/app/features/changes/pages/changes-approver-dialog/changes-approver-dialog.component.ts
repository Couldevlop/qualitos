import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ChangesService } from '../../changes.service';
import { ApprovalResponse } from '../../changes.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export interface ChangesApproverDialogData { changeId: string; }

@Component({
  selector: 'qos-changes-approver-dialog',
  templateUrl: './changes-approver-dialog.component.html',
  styleUrls: ['./changes-approver-dialog.component.scss'],
  standalone: false
})
export class ChangesApproverDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    approverUserId: ['', [Validators.required, Validators.pattern(UUID_REGEX)]],
    approvalLevel:  [1, [Validators.min(1)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ChangesService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ChangesApproverDialogComponent, ApprovalResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ChangesApproverDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.addApprover(this.data.changeId, {
      approverUserId: v.approverUserId.trim(),
      approvalLevel: v.approvalLevel
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => { this.snack.open('Approbateur ajouté.', 'OK', { duration: 2200 }); this.dialogRef.close(a); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[changes-approver] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'ajout.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
