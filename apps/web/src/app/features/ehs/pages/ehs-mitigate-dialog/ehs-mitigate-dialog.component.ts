import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EhsService } from '../../ehs.service';
import { IncidentView } from '../../ehs.types';

export interface EhsMitigateDialogData { incidentId: string; }

@Component({
  selector: 'qos-ehs-mitigate-dialog',
  templateUrl: './ehs-mitigate-dialog.component.html',
  styleUrls: ['./ehs-mitigate-dialog.component.scss'],
  standalone: false
})
export class EhsMitigateDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    rootCause:         ['', [Validators.required, Validators.maxLength(2000)]],
    correctiveActions: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EhsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EhsMitigateDialogComponent, IncidentView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: EhsMitigateDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.mitigate(this.data.incidentId, {
      rootCause: v.rootCause.trim(),
      correctiveActions: v.correctiveActions.trim()
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open('Incident mitigé.', 'OK', { duration: 2500 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ehs-mitigate] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Mitigation impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
