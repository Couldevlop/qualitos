import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiView } from '../../cyi.types';

export interface CyiMitigateDialogData { id: string; }

@Component({
  selector: 'qos-cyi-mitigate-dialog',
  templateUrl: './cyi-mitigate-dialog.component.html',
  styleUrls: ['./cyi-mitigate-dialog.component.scss'],
  standalone: false
})
export class CyiMitigateDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    containmentMeasures: ['', [Validators.required, Validators.maxLength(4000)]],
    impactDescription:   ['', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: CyberIncidentsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CyiMitigateDialogComponent, CyiView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CyiMitigateDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const userId = this.auth.snapshot()?.userId;
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.mitigate(this.data.id, {
      containmentMeasures: v.containmentMeasures.trim(),
      impactDescription: v.impactDescription?.trim() || undefined,
      handledByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open('Incident endigué.', 'OK', { duration: 2200 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[cyi-mitigate] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Endiguement impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
