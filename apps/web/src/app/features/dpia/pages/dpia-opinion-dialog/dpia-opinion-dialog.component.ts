import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpiaService } from '../../dpia.service';
import { DpiaView } from '../../dpia.types';

export interface DpiaOpinionDialogData {
  dpiaId: string;
  decision: 'APPROVED' | 'REJECTED';
}

@Component({
  selector: 'qos-dpia-opinion-dialog',
  templateUrl: './dpia-opinion-dialog.component.html',
  styleUrls: ['./dpia-opinion-dialog.component.scss'],
  standalone: false
})
export class DpiaOpinionDialogComponent {

  submitting = false;

  readonly rejectLabel  = $localize`:@@dpia.opinion.reject:Rejeter`;
  readonly approveLabel = $localize`:@@dpia.opinion.approve:Approuver`;

  readonly form = this.fb.nonNullable.group({
    dpoOpinion: ['', [Validators.required, Validators.maxLength(8000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DpiaService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DpiaOpinionDialogComponent, DpiaView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DpiaOpinionDialogData
  ) {}

  get title(): string {
    return this.data.decision === 'APPROVED'
      ? $localize`:@@dpia.opinion.title-approve:Avis DPO — Approuver la DPIA`
      : $localize`:@@dpia.opinion.title-reject:Avis DPO — Rejeter la DPIA`;
  }
  get danger(): boolean { return this.data.decision === 'REJECTED'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const dpoUserId = this.auth.snapshot()?.userId;
    if (!dpoUserId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    const op$ = this.data.decision === 'APPROVED'
      ? this.svc.approve(this.data.dpiaId, { dpoUserId, dpoOpinion: v.dpoOpinion.trim() })
      : this.svc.reject(this.data.dpiaId,  { dpoUserId, dpoOpinion: v.dpoOpinion.trim() });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: d => {
          this.snack.open(this.data.decision === 'APPROVED'
            ? $localize`:@@dpia.opinion.approved-toast:DPIA approuvée par le DPO.`
            : $localize`:@@dpia.opinion.rejected-toast:DPIA rejetée par le DPO.`,
            $localize`:@@common.ok:OK`, { duration: 2800 });
          this.dialogRef.close(d);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dpia-opinion] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@dpia.opinion.failed:Décision impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
