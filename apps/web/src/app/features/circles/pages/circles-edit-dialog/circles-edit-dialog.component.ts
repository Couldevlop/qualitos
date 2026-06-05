import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CirclesService } from '../../circles.service';
import { CircleResponse } from '../../circles.types';

export interface CirclesEditDialogData {
  circle: CircleResponse;
}

@Component({
  selector: 'qos-circles-edit-dialog',
  templateUrl: './circles-edit-dialog.component.html',
  styleUrls: ['./circles-edit-dialog.component.scss'],
  standalone: false
})
export class CirclesEditDialogComponent {
  submitting = false;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    topic: ['', [Validators.maxLength(255)]],
    description: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly circles: CirclesService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CirclesEditDialogComponent, CircleResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CirclesEditDialogData
  ) {
    this.form.patchValue({
      name: data.circle.name,
      topic: data.circle.topic ?? '',
      description: data.circle.description ?? ''
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.circles
      .updateCircle(this.data.circle.id, {
        name: v.name.trim(),
        topic: v.topic?.trim() || undefined,
        description: v.description?.trim() || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          this.snack.open($localize`:@@circles.edit.updated:Cercle mis à jour.`, 'OK', { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[circles-edit] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@circles.edit.error:Erreur lors de la mise à jour.`),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
