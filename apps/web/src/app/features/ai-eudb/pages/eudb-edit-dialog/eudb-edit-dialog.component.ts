import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EudbService } from '../../eudb.service';
import { EudbView } from '../../eudb.types';

export interface EudbEditDialogData { row: EudbView; }

@Component({
  selector: 'qos-eudb-edit-dialog',
  templateUrl: './eudb-edit-dialog.component.html',
  styleUrls: ['./eudb-edit-dialog.component.scss'],
  standalone: false
})
export class EudbEditDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    providerEntityName:              [this.data.row.providerEntityName ?? '',              [Validators.maxLength(250)]],
    providerEuRepresentative:        [this.data.row.providerEuRepresentative ?? '',        [Validators.maxLength(250)]],
    memberStateOfReference:          [this.data.row.memberStateOfReference ?? '',          [Validators.pattern(/^$|^[A-Z]{2}$/)]],
    intendedPurposeSummary:          [this.data.row.intendedPurposeSummary ?? '',          [Validators.maxLength(4000)]],
    technicalDocumentationReference: [this.data.row.technicalDocumentationReference ?? '', [Validators.maxLength(250)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EudbService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EudbEditDialogComponent, EudbView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: EudbEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.edit(this.data.row.id, {
      providerEntityName: v.providerEntityName?.trim() || undefined,
      providerEuRepresentative: v.providerEuRepresentative?.trim() || undefined,
      memberStateOfReference: v.memberStateOfReference?.trim().toUpperCase() || undefined,
      intendedPurposeSummary: v.intendedPurposeSummary?.trim() || undefined,
      technicalDocumentationReference: v.technicalDocumentationReference?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: r => { this.snack.open('Brouillon mis à jour.', 'OK', { duration: 2200 }); this.dialogRef.close(r); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[eudb-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Mise à jour impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
