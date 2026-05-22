import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiSeverity, CyiView } from '../../cyi.types';

export interface CyiSeverityDialogData { id: string; current: CyiSeverity; }

@Component({
  selector: 'qos-cyi-severity-dialog',
  templateUrl: './cyi-severity-dialog.component.html',
  styleUrls: ['./cyi-severity-dialog.component.scss'],
  standalone: false
})
export class CyiSeverityDialogComponent {

  submitting = false;
  readonly severities: CyiSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    severity: [this.data.current, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CyberIncidentsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CyiSeverityDialogComponent, CyiView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CyiSeverityDialogData
  ) {}

  willEnableNotif(): boolean {
    const s = this.form.controls.severity.value;
    return (s === 'HIGH' || s === 'CRITICAL') && (this.data.current === 'LOW' || this.data.current === 'MEDIUM');
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    this.svc.updateSeverity(this.data.id, { severity: this.form.getRawValue().severity })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open('Sévérité mise à jour.', 'OK', { duration: 2200 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cyi-severity] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Mise à jour impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
