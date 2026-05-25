import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { BreachService } from '../../breach.service';
import { BreachView } from '../../breach.types';

export interface BreachContainDialogData { id: string; }

@Component({
  selector: 'qos-breach-contain-dialog',
  templateUrl: './breach-contain-dialog.component.html',
  styleUrls: ['./breach-contain-dialog.component.scss'],
  standalone: false
})
export class BreachContainDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    containmentMeasures: ['', [Validators.required, Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: BreachService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<BreachContainDialogComponent, BreachView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: BreachContainDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const userId = this.auth.snapshot()?.userId;
    this.submitting = true;
    this.svc.contain(this.data.id, {
      containmentMeasures: this.form.getRawValue().containmentMeasures.trim(),
      handledByUserId: userId
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: b => { this.snack.open('Violation endiguée.', 'OK', { duration: 2200 }); this.dialogRef.close(b); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[breach-contain] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Endiguement impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
