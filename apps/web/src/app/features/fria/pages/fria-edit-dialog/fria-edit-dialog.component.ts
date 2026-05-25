import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { FriaService } from '../../fria.service';
import { FriaView } from '../../fria.types';

export interface FriaEditDialogData { row: FriaView; }

@Component({
  selector: 'qos-fria-edit-dialog',
  templateUrl: './fria-edit-dialog.component.html',
  styleUrls: ['./fria-edit-dialog.component.scss'],
  standalone: false
})
export class FriaEditDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    processDescription:            [this.data.row.processDescription,             [Validators.required, Validators.maxLength(4000)]],
    deploymentDurationDescription: [this.data.row.deploymentDurationDescription ?? '', [Validators.maxLength(4000)]],
    affectedPersonsCategories:     [this.data.row.affectedPersonsCategories,      [Validators.required, Validators.maxLength(4000)]],
    specificRisks:                 [this.data.row.specificRisks,                  [Validators.required, Validators.maxLength(4000)]],
    mitigationMeasures:            [this.data.row.mitigationMeasures ?? '',       [Validators.maxLength(4000)]],
    humanOversightMeasures:        [this.data.row.humanOversightMeasures ?? '',   [Validators.maxLength(4000)]],
    complaintMechanismDescription: [this.data.row.complaintMechanismDescription ?? '', [Validators.maxLength(4000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: FriaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FriaEditDialogComponent, FriaView>,
    @Inject(MAT_DIALOG_DATA) public readonly data: FriaEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.edit(this.data.row.id, {
      processDescription: v.processDescription.trim(),
      deploymentDurationDescription: v.deploymentDurationDescription?.trim() || undefined,
      affectedPersonsCategories: v.affectedPersonsCategories.trim(),
      specificRisks: v.specificRisks.trim(),
      mitigationMeasures: v.mitigationMeasures?.trim() || undefined,
      humanOversightMeasures: v.humanOversightMeasures?.trim() || undefined,
      complaintMechanismDescription: v.complaintMechanismDescription?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: f => { this.snack.open('FRIA mise à jour.', 'OK', { duration: 2200 }); this.dialogRef.close(f); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fria-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Mise à jour impossible.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
