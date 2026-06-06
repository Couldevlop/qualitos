import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { BreachService } from '../../breach.service';
import { BreachSeverity, BreachView } from '../../breach.types';

export interface BreachSeverityDialogData { id: string; current: BreachSeverity; }

@Component({
  selector: 'qos-breach-severity-dialog',
  templateUrl: './breach-severity-dialog.component.html',
  styleUrls: ['./breach-severity-dialog.component.scss'],
  standalone: false
})
export class BreachSeverityDialogComponent {

  submitting = false;
  readonly severities: BreachSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    severity: [this.data.current, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: BreachService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BreachSeverityDialogComponent, BreachView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: BreachSeverityDialogData
  ) {}

  willEnableSubjects(): boolean {
    const s = this.form.controls.severity.value;
    return (s === 'HIGH' || s === 'CRITICAL') && (this.data.current === 'LOW' || this.data.current === 'MEDIUM');
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    this.svc.updateSeverity(this.data.id, { severity: this.form.getRawValue().severity })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: b => { this.snack.open($localize`:@@breach.severity.updated-toast:Sévérité mise à jour.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.dialogRef.close(b); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[breach-sev] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@breach.severity.update-failed:Mise à jour impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
