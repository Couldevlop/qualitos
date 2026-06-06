import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { describeDuration, RetentionService } from '../../retention.service';
import { ErasureEvaluation } from '../../retention.types';

@Component({
  selector: 'qos-ret-evaluate-dialog',
  templateUrl: './ret-evaluate-dialog.component.html',
  styleUrls: ['./ret-evaluate-dialog.component.scss'],
  standalone: false
})
export class RetEvaluateDialogComponent {

  submitting = false;
  result: ErasureEvaluation | null = null;
  notFound = false;

  // OWASP A03 — regex miroir backend @Pattern.
  readonly form = this.fb.nonNullable.group({
    dataCategoryCode: ['', [
      Validators.required, Validators.maxLength(64),
      Validators.pattern(/^[a-z][a-z0-9._-]{1,63}$/)
    ]],
    recordCreatedAt: ['', [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: RetentionService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<RetEvaluateDialogComponent>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (new Date(v.recordCreatedAt) > new Date()) {
      this.snack.open($localize`:@@retention.evaluate-dialog.future-date:La date de création ne peut pas être dans le futur.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.result = null;
    this.notFound = false;
    this.svc.evaluateErasure(v.dataCategoryCode.trim(), new Date(v.recordCreatedAt).toISOString())
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => {
          if (r) this.result = r;
          else this.notFound = true;
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ret-evaluate] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@retention.evaluate-dialog.eval-failed:Évaluation impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  describe = describeDuration;
  close(): void { this.dialogRef.close(); }
}
