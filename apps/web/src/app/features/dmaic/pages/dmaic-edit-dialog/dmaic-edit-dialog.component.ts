import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DmaicService } from '../../dmaic.service';
import { DmaicProjectResponse } from '../../dmaic.types';

export interface DmaicEditDialogData {
  project: DmaicProjectResponse;
}

@Component({
  selector: 'qos-dmaic-edit-dialog',
  templateUrl: './dmaic-edit-dialog.component.html',
  styleUrls: ['./dmaic-edit-dialog.component.scss'],
  standalone: false
})
export class DmaicEditDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    title: [this.data.project.title, [Validators.required, Validators.maxLength(255)]],
    problemStatement: [this.data.project.problemStatement ?? ''],
    goalStatement:    [this.data.project.goalStatement ?? ''],
    championId:       [this.data.project.championId ?? ''],
    targetCompletionDate: [this.data.project.targetCompletionDate ?? ''],
    specLowerLimit: [this.data.project.specLowerLimit ?? null as number | null],
    specUpperLimit: [this.data.project.specUpperLimit ?? null as number | null],
    specTarget:     [this.data.project.specTarget     ?? null as number | null],
    specUnit: [this.data.project.specUnit ?? '', [Validators.maxLength(50)]],
    estimatedSavingsEur: [this.data.project.estimatedSavingsEur ?? null as number | null]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DmaicService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DmaicEditDialogComponent, DmaicProjectResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: DmaicEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.updateProject(this.data.project.id, {
      title: v.title.trim(),
      problemStatement: v.problemStatement?.trim() || undefined,
      goalStatement:    v.goalStatement?.trim()    || undefined,
      championId: v.championId?.trim() || undefined,
      targetCompletionDate: v.targetCompletionDate || undefined,
      specLowerLimit: v.specLowerLimit ?? undefined,
      specUpperLimit: v.specUpperLimit ?? undefined,
      specTarget:     v.specTarget     ?? undefined,
      specUnit: v.specUnit?.trim() || undefined,
      estimatedSavingsEur: v.estimatedSavingsEur ?? undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => {
          this.snack.open($localize`:@@dmaic.edit.updated:Projet mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(p);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dmaic-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
