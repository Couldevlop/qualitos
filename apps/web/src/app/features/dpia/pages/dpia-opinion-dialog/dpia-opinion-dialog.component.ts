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
      ? 'Avis DPO — Approuver la DPIA'
      : 'Avis DPO — Rejeter la DPIA';
  }
  get danger(): boolean { return this.data.decision === 'REJECTED'; }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const dpoUserId = this.auth.snapshot()?.userId;
    if (!dpoUserId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
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
            ? 'DPIA approuvée par le DPO.' : 'DPIA rejetée par le DPO.',
            'OK', { duration: 2800 });
          this.dialogRef.close(d);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dpia-opinion] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Décision impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
