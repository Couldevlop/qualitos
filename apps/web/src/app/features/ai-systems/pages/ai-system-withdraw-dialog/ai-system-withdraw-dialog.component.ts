import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiSystemsService } from '../../ai-systems.service';
import { AiSystemView } from '../../ai-systems.types';

export interface AiSystemWithdrawData {
  system: AiSystemView;
}

/**
 * Abandon d'un système avant mise en service.
 *
 * Le motif est exigé par le domaine (`AiSystem#withdraw`) et l'état est terminal :
 * c'est la dernière trace lisible de la décision dans le registre, elle mérite un
 * champ dédié plutôt qu'une simple confirmation.
 */
@Component({
  selector: 'qos-ai-system-withdraw-dialog',
  templateUrl: './ai-system-withdraw-dialog.component.html',
  styleUrls: ['./ai-system-withdraw-dialog.component.scss'],
  standalone: false
})
export class AiSystemWithdrawDialogComponent {

  submitting = false;
  error: string | null = null;

  readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiSystemsService,
    private readonly dialogRef:
      MatDialogRef<AiSystemWithdrawDialogComponent, AiSystemView | undefined>,
    @Inject(MAT_DIALOG_DATA) readonly data: AiSystemWithdrawData
  ) {}

  submit(): void {
    if (this.submitting) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.error = null;
    this.svc.withdraw(this.data.system.id, { reason: this.form.getRawValue().reason.trim() })
      .pipe(finalize(() => { this.submitting = false; }))
      .subscribe({
        next: updated => this.dialogRef.close(updated),
        error: err => {
          this.error = safeErrorMessage(err,
            $localize`:@@ai-systems.withdraw.failed:L'abandon du système a échoué.`);
        }
      });
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }
}
