import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiQmsService } from '../../ai-qms.service';
import { AiQmsView } from '../../ai-qms.types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export interface AiQmsApproveDialogData { qmsId: string; }

@Component({
  selector: 'qos-ai-qms-approve-dialog',
  templateUrl: './ai-qms-approve-dialog.component.html',
  styleUrls: ['./ai-qms-approve-dialog.component.scss'],
  standalone: false
})
export class AiQmsApproveDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    submittedByUserId: ['', [Validators.required, Validators.pattern(UUID_REGEX)]],
    approvalNotes: ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiQmsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AiQmsApproveDialogComponent, AiQmsView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AiQmsApproveDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const approvedByUserId = this.auth.snapshot()?.userId;
    if (!approvedByUserId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    const v = this.form.getRawValue();
    // OWASP A04 — séparation des rôles : submitter ≠ approver.
    if (v.submittedByUserId.trim() === approvedByUserId) {
      this.snack.open(
        'Séparation des rôles : le soumissionnaire et l\'approbateur doivent être différents.',
        'OK', { duration: 4500 }
      );
      return;
    }
    this.submitting = true;
    this.svc.approve(this.data.qmsId, {
      submittedByUserId: v.submittedByUserId.trim(),
      approvedByUserId,
      approvalNotes: v.approvalNotes?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: q => { this.snack.open('QMS approuvé.', 'OK', { duration: 2500 }); this.dialogRef.close(q); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ai-qms-approve] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Approbation impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
