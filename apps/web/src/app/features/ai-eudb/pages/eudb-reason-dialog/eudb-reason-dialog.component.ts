import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EudbService } from '../../eudb.service';
import { EudbView } from '../../eudb.types';

export interface EudbReasonDialogData {
  id: string;
  mode: 'REJECT' | 'RETIRE';
}

@Component({
  selector: 'qos-eudb-reason-dialog',
  templateUrl: './eudb-reason-dialog.component.html',
  styleUrls: ['./eudb-reason-dialog.component.scss'],
  standalone: false
})
export class EudbReasonDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EudbService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EudbReasonDialogComponent, EudbView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: EudbReasonDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'REJECT'
      ? 'Rejeter l\'enregistrement'
      : 'Retirer l\'enregistrement EUDB';
  }

  get hint(): string {
    return this.data.mode === 'REJECT'
      ? 'Action terminale. Le brouillon ou la soumission est rejeté définitivement.'
      : 'Action terminale. Le système IA n\'est plus considéré comme actif dans EUDB.';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const reason = this.form.getRawValue().reason.trim();
    const op$ = this.data.mode === 'REJECT'
      ? this.svc.reject(this.data.id, { reason })
      : this.svc.retire(this.data.id, { reason });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => {
          this.snack.open(this.data.mode === 'REJECT' ? 'Enregistrement rejeté.' : 'Enregistrement retiré.',
                          'OK', { duration: 2200 });
          this.dialogRef.close(r);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[eudb-reason] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Opération impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
