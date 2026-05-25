import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { PmmService } from '../../pmm.service';
import { PmmPlanView } from '../../pmm.types';

export interface PmmReasonDialogData {
  id: string;
  mode: 'SUSPEND' | 'CLOSE';
}

@Component({
  selector: 'qos-pmm-reason-dialog',
  templateUrl: './pmm-reason-dialog.component.html',
  styleUrls: ['./pmm-reason-dialog.component.scss'],
  standalone: false
})
export class PmmReasonDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: PmmService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<PmmReasonDialogComponent, PmmPlanView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: PmmReasonDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'SUSPEND' ? 'Suspendre le plan PMM' : 'Clôturer le plan PMM';
  }

  get hint(): string {
    return this.data.mode === 'SUSPEND'
      ? 'Le plan passe en SUSPENDED — la surveillance est mise en pause mais peut être réactivée.'
      : '<strong>Action terminale.</strong> Le plan PMM est clos — fin de la surveillance post-marché.';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const reason = this.form.getRawValue().reason.trim();
    const op$: Observable<PmmPlanView> = this.data.mode === 'SUSPEND'
      ? this.svc.suspend(this.data.id, { reason })
      : this.svc.close(this.data.id, { reason });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => { this.snack.open(this.data.mode === 'SUSPEND' ? 'Plan suspendu.' : 'Plan clos.',
                                     'OK', { duration: 2200 }); this.dialogRef.close(p); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pmm-reason] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Opération impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
