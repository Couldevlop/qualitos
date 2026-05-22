import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiIncidentsService } from '../../ai-inc.service';
import { AiIncView } from '../../ai-inc.types';

export interface IncNotifyDialogData { incidentId: string; }

@Component({
  selector: 'qos-inc-notify-dialog',
  templateUrl: './inc-notify-dialog.component.html',
  styleUrls: ['./inc-notify-dialog.component.scss'],
  standalone: false
})
export class IncNotifyDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    regulatorReference: ['', [Validators.required, Validators.maxLength(250)]],
    rootCauseAnalysis:  ['', [Validators.required, Validators.maxLength(4000)]],
    correctiveActions:  ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: AiIncidentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IncNotifyDialogComponent, AiIncView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: IncNotifyDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.notifyRegulator(this.data.incidentId, {
      regulatorReference: v.regulatorReference.trim(),
      rootCauseAnalysis:  v.rootCauseAnalysis.trim(),
      correctiveActions:  v.correctiveActions?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open('Régulateur notifié.', 'OK', { duration: 2500 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[inc-notify] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Notification impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
