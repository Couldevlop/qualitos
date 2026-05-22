import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiIncidentsService } from '../../ai-inc.service';
import { AiIncView } from '../../ai-inc.types';

export interface IncCloseDialogData {
  incidentId: string;
  mode: 'CLOSE' | 'DISMISS';
}

@Component({
  selector: 'qos-inc-close-dialog',
  templateUrl: './inc-close-dialog.component.html',
  styleUrls: ['./inc-close-dialog.component.scss'],
  standalone: false
})
export class IncCloseDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    text: ['', [Validators.required, Validators.maxLength(this.data.mode === 'CLOSE' ? 4000 : 2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiIncidentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IncCloseDialogComponent, AiIncView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: IncCloseDialogData
  ) {}

  get title(): string {
    return this.data.mode === 'CLOSE'
      ? 'Clôturer l\'incident (actions correctives)'
      : 'Rejeter l\'incident (faux positif / non-incident)';
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const text = this.form.getRawValue().text.trim();
    const op$ = this.data.mode === 'CLOSE'
      ? this.svc.close(this.data.incidentId, { correctiveActions: text })
      : this.svc.dismiss(this.data.incidentId, { reason: text });
    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open(this.data.mode === 'CLOSE' ? 'Incident clôturé.' : 'Incident rejeté.', 'OK', { duration: 2500 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[inc-close] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Opération impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
