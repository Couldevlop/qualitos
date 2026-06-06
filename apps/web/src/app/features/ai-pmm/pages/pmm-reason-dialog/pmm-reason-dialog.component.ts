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
    return this.data.mode === 'SUSPEND'
      ? $localize`:@@ai-pmm.reason.title-suspend:Suspendre le plan PMM`
      : $localize`:@@ai-pmm.reason.title-close:Clôturer le plan PMM`;
  }

  get hint(): string {
    return this.data.mode === 'SUSPEND'
      ? $localize`:@@ai-pmm.reason.hint-suspend:Le plan passe en SUSPENDED — la surveillance est mise en pause mais peut être réactivée.`
      : $localize`:@@ai-pmm.reason.hint-close:<strong>Action terminale.</strong> Le plan PMM est clos — fin de la surveillance post-marché.`;
  }

  get reasonLabel(): string {
    return this.data.mode === 'SUSPEND'
      ? $localize`:@@ai-pmm.reason.label-suspend:Motif de la suspension`
      : $localize`:@@ai-pmm.reason.label-close:Motif de la clôture`;
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
        next: p => { this.snack.open(this.data.mode === 'SUSPEND'
                                       ? $localize`:@@ai-pmm.reason.suspended:Plan suspendu.`
                                       : $localize`:@@ai-pmm.reason.closed:Plan clos.`,
                                     $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(p); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pmm-reason] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@ai-pmm.reason.op-failed:Opération impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
