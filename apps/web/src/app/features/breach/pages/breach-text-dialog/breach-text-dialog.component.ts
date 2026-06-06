import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';
import { Observable } from 'rxjs';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { BreachService } from '../../breach.service';
import { BreachView } from '../../breach.types';

export interface BreachTextDialogData {
  id: string;
  mode: 'CLOSE' | 'REJECT';
}

@Component({
  selector: 'qos-breach-text-dialog',
  templateUrl: './breach-text-dialog.component.html',
  styleUrls: ['./breach-text-dialog.component.scss'],
  standalone: false
})
export class BreachTextDialogComponent {

  submitting = false;

  readonly closeLabel  = $localize`:@@breach.text.close-label:Notes de clôture (optionnel)`;
  readonly rejectLabel = $localize`:@@breach.text.reject-label:Motif du rejet`;

  readonly form = this.fb.nonNullable.group({
    text: ['', this.data.mode === 'REJECT'
        ? [Validators.required, Validators.maxLength(2000)]
        : [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: BreachService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BreachTextDialogComponent, BreachView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: BreachTextDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'CLOSE'
      ? $localize`:@@breach.text.title-close:Clôturer la violation`
      : $localize`:@@breach.text.title-reject:Rejeter (non-violation / faux positif)`;
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const text = this.form.getRawValue().text.trim();
    const op$: Observable<BreachView> = this.data.mode === 'CLOSE'
      ? this.svc.close(this.data.id, { closureNotes: text || undefined })
      : this.svc.reject(this.data.id, { reason: text });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: b => { this.snack.open(this.data.mode === 'CLOSE'
                                     ? $localize`:@@breach.text.closed-toast:Violation clôturée.`
                                     : $localize`:@@breach.text.rejected-toast:Violation rejetée.`,
                                     $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(b); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[breach-text] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@breach.text.failed:Opération impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
