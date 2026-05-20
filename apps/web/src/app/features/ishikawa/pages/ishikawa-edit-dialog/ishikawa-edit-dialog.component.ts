import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { IshikawaService } from '../../ishikawa.service';
import { IshikawaDiagramResponse, IshikawaMode, IshikawaStatus } from '../../ishikawa.types';

export interface IshikawaEditDialogData {
  diagram: IshikawaDiagramResponse;
}

@Component({
  selector: 'qos-ishikawa-edit-dialog',
  templateUrl: './ishikawa-edit-dialog.component.html',
  styleUrls: ['./ishikawa-edit-dialog.component.scss'],
  standalone: false
})
export class IshikawaEditDialogComponent {

  submitting = false;

  readonly modes: { value: IshikawaMode; label: string }[] = [
    { value: 'SIX_M', label: '6M' },
    { value: 'SEVEN_M', label: '7M' },
    { value: 'EIGHT_M', label: '8M' }
  ];
  readonly statuses: IshikawaStatus[] = ['DRAFT', 'IN_REVIEW', 'VALIDATED', 'ARCHIVED'];

  readonly form = this.fb.nonNullable.group({
    problemStatement: ['', [Validators.required, Validators.maxLength(500)]],
    description: [''],
    mode: ['SIX_M' as IshikawaMode, [Validators.required]],
    status: ['DRAFT' as IshikawaStatus, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly ishikawa: IshikawaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<IshikawaEditDialogComponent, IshikawaDiagramResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: IshikawaEditDialogData
  ) {
    this.form.patchValue({
      problemStatement: data.diagram.problemStatement,
      description: data.diagram.description ?? '',
      mode: data.diagram.mode,
      status: data.diagram.status
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.ishikawa
      .updateDiagram(this.data.diagram.id, {
        problemStatement: v.problemStatement.trim(),
        description: v.description?.trim() || undefined,
        mode: v.mode,
        status: v.status
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: d => {
          this.snack.open('Diagramme mis à jour.', 'OK', { duration: 2500 });
          this.dialogRef.close(d);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ishikawa-edit] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de la mise à jour.'),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
