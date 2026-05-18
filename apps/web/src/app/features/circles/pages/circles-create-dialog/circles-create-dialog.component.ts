import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { CirclesService } from '../../circles.service';
import { CircleResponse } from '../../circles.types';

@Component({
  selector: 'qos-circles-create-dialog',
  templateUrl: './circles-create-dialog.component.html',
  styleUrls: ['./circles-create-dialog.component.scss'],
  standalone: false
})
export class CirclesCreateDialogComponent {

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
    private readonly dialogRef: MatDialogRef<CirclesCreateDialogComponent, CircleResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const { name, topic, description } = this.form.getRawValue();
    this.circles
      .createCircle({
        name: name.trim(),
        topic: topic?.trim() || undefined,
        description: description?.trim() || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: circle => {
          this.snack.open('Cercle créé.', 'OK', { duration: 2500 });
          this.dialogRef.close(circle);
        },
        error: err => {
          this.snack.open(
            err?.error?.message ?? err?.message ?? 'Erreur lors de la création',
            'OK',
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
