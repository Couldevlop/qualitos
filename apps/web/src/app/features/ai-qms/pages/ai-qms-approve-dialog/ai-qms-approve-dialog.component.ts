import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiQmsService } from '../../ai-qms.service';
import { AiQmsView } from '../../ai-qms.types';

export interface AiQmsApproveDialogData {
  qmsId: string;
  /** Le compte qui a soumis le QMS, quand il est connu : proposé d'office. */
  submittedByUserId?: string;
}

@Component({
  selector: 'qos-ai-qms-approve-dialog',
  templateUrl: './ai-qms-approve-dialog.component.html',
  styleUrls: ['./ai-qms-approve-dialog.component.scss'],
  standalone: false
})
export class AiQmsApproveDialogComponent {

  submitting = false;

  /** Séparation des rôles : l'approbateur (vous) n'est pas proposé comme soumissionnaire. */
  readonly excluded: readonly string[];

  readonly form = this.fb.group({
    submittedByUserId: [null as string | null, [Validators.required]],
    approvalNotes: ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiQmsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AiQmsApproveDialogComponent, AiQmsView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AiQmsApproveDialogData
  ) {
    const me = this.auth.snapshot()?.userId;
    this.excluded = me ? [me] : [];
    if (data.submittedByUserId && data.submittedByUserId !== me) {
      this.form.controls.submittedByUserId.setValue(data.submittedByUserId);
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const approvedByUserId = this.auth.snapshot()?.userId;
    if (!approvedByUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const v = this.form.getRawValue();
    // OWASP A04 — séparation des rôles : submitter ≠ approver.
    const submittedByUserId = (v.submittedByUserId ?? '').trim();
    if (submittedByUserId === approvedByUserId) {
      this.snack.open(
        $localize`:@@ai-qms.approve.role-separation:Séparation des rôles : le soumissionnaire et l'approbateur doivent être différents.`,
        $localize`:@@common.ok:OK`, { duration: 4500 }
      );
      return;
    }
    this.submitting = true;
    this.svc.approve(this.data.qmsId, {
      submittedByUserId,
      approvedByUserId,
      approvalNotes: v.approvalNotes?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: q => { this.snack.open($localize`:@@ai-qms.approve.approved:QMS approuvé.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(q); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ai-qms-approve] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@ai-qms.approve.failed:Approbation impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
