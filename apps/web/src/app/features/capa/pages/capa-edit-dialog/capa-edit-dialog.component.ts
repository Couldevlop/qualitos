import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaCriticity } from '../../capa.types';

export interface CapaEditDialogData {
  capa: CapaCaseResponse;
}

@Component({
  selector: 'qos-capa-edit-dialog',
  templateUrl: './capa-edit-dialog.component.html',
  styleUrls: ['./capa-edit-dialog.component.scss'],
  standalone: false
})
export class CapaEditDialogComponent {
  submitting = false;

  readonly criticities: CapaCriticity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    criticity: ['MEDIUM' as CapaCriticity, [Validators.required]],
    sourceRef: ['', [Validators.maxLength(255)]],
    dueDate: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly capa: CapaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<CapaEditDialogComponent, CapaCaseResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: CapaEditDialogData
  ) {
    this.form.patchValue({
      title: data.capa.title,
      description: data.capa.description ?? '',
      criticity: data.capa.criticity,
      sourceRef: data.capa.sourceRef ?? '',
      dueDate: data.capa.dueDate ?? ''
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.capa
      .updateCase(this.data.capa.id, {
        title: v.title.trim(),
        description: v.description?.trim() || undefined,
        criticity: v.criticity,
        sourceRef: v.sourceRef?.trim() || undefined,
        dueDate: v.dueDate || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          this.snack.open('Cas mis à jour.', 'OK', { duration: 2500 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[capa-edit] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de la mise à jour.'),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
