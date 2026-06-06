import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { TransfersService } from '../../transfers.service';
import { TransferView } from '../../transfers.types';

export interface TrxReasonDialogData {
  transferId: string;
  mode: 'SUSPEND' | 'TERMINATE';
}

@Component({
  selector: 'qos-trx-reason-dialog',
  templateUrl: './trx-reason-dialog.component.html',
  styleUrls: ['./trx-reason-dialog.component.scss'],
  standalone: false
})
export class TrxReasonDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: TransfersService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<TrxReasonDialogComponent, TransferView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: TrxReasonDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'SUSPEND'
      ? $localize`:@@transfers.reason-dialog.title-suspend:Suspendre le transfert`
      : $localize`:@@transfers.reason-dialog.title-terminate:Terminer le transfert`;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const reason = this.form.getRawValue().reason.trim();
    const op$ = this.data.mode === 'SUSPEND'
      ? this.svc.suspend(this.data.transferId, { reason })
      : this.svc.terminate(this.data.transferId, { reason });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: t => {
          this.snack.open(this.data.mode === 'SUSPEND' ? $localize`:@@transfers.reason-dialog.suspended:Transfert suspendu.` : $localize`:@@transfers.reason-dialog.terminated:Transfert terminé.`,
            $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(t);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[trx-reason] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@transfers.reason-dialog.op-failed:Opération impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
