import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiConformityService } from '../../ai-conformity.service';
import { ConformityView } from '../../ai-conformity.types';

export interface CnfReasonDialogData {
  conformityId: string;
  mode: 'REVOKE' | 'FAIL';
}

@Component({
  selector: 'qos-cnf-reason-dialog',
  templateUrl: './cnf-reason-dialog.component.html',
  styleUrls: ['./cnf-reason-dialog.component.scss'],
  standalone: false
})
export class CnfReasonDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiConformityService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CnfReasonDialogComponent, ConformityView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CnfReasonDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'REVOKE' ? 'Révoquer le certificat' : 'Marquer en échec';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const reason = this.form.getRawValue().reason.trim();
    const op$ = this.data.mode === 'REVOKE'
      ? this.svc.revoke(this.data.conformityId, { reason })
      : this.svc.fail(this.data.conformityId, { reason });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => { this.snack.open(this.data.mode === 'REVOKE' ? 'Certificat révoqué.' : 'Évaluation marquée en échec.', 'OK', { duration: 2500 }); this.dialogRef.close(c); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cnf-reason] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Opération impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
