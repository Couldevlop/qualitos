import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ChangesService } from '../../changes.service';
import { ChangeResponse } from '../../changes.types';

export interface ChangesImplementDialogData { changeId: string; }

@Component({
  selector: 'qos-changes-implement-dialog',
  templateUrl: './changes-implement-dialog.component.html',
  styleUrls: ['./changes-implement-dialog.component.scss'],
  standalone: false
})
export class ChangesImplementDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    implementedAt: [new Date().toISOString().slice(0, 10), [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ChangesService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ChangesImplementDialogComponent, ChangeResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: ChangesImplementDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.implement(this.data.changeId, { implementedAt: v.implementedAt })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => { this.snack.open('Demande marquée IMPLEMENTED.', 'OK', { duration: 2200 }); this.dialogRef.close(c); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[changes-implement] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Implémentation impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
