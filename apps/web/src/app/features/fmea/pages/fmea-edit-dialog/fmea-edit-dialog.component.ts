import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { FmeaService } from '../../fmea.service';
import { FmeaProjectResponse } from '../../fmea.types';

export interface FmeaEditDialogData {
  project: FmeaProjectResponse;
}

@Component({
  selector: 'qos-fmea-edit-dialog',
  templateUrl: './fmea-edit-dialog.component.html',
  styleUrls: ['./fmea-edit-dialog.component.scss'],
  standalone: false
})
export class FmeaEditDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    name: [this.data.project.name, [Validators.required, Validators.maxLength(250)]],
    scope: [this.data.project.scope ?? '', [Validators.maxLength(1000)]],
    criticalRpnThreshold: [
      this.data.project.criticalRpnThreshold,
      [Validators.required, Validators.min(1), Validators.max(1000)]
    ]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: FmeaService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<FmeaEditDialogComponent, FmeaProjectResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: FmeaEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.update(this.data.project.id, {
      name: v.name.trim(),
      scope: v.scope?.trim() || undefined,
      criticalRpnThreshold: v.criticalRpnThreshold
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => {
          this.snack.open($localize`:@@fmea.edit.success:Projet mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(p);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fmea-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
