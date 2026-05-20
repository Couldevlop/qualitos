import { Component, Inject, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { TrainingService } from '../../training.service';
import { SkillResponse } from '../../training.types';

export interface TrainingSkillDialogData { skill?: SkillResponse; }

@Component({
  selector: 'qos-training-skill-dialog',
  templateUrl: './training-skill-dialog.component.html',
  styleUrls: ['./training-skill-dialog.component.scss'],
  standalone: false
})
export class TrainingSkillDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: TrainingService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<TrainingSkillDialogComponent, SkillResponse>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: TrainingSkillDialogData | null
  ) {
    this.isEdit = !!data?.skill;
    const s = data?.skill;
    this.form = this.fb.nonNullable.group({
      code: [
        { value: s?.code ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(100), Validators.pattern(/^[a-z0-9][a-z0-9_-]{1,99}$/)]
      ],
      name:        [s?.name ?? '',        [Validators.required, Validators.maxLength(200)]],
      description: [s?.description ?? '', [Validators.maxLength(1000)]],
      category:    [s?.category ?? '',    [Validators.maxLength(64)]]
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();

    const op$ = this.isEdit
      ? this.svc.updateSkill(this.data!.skill!.id, {
          name: v.name.trim(),
          description: v.description?.trim() || undefined,
          category: v.category?.trim() || undefined
        })
      : this.svc.createSkill({
          code: v.code.trim(),
          name: v.name.trim(),
          description: v.description?.trim() || undefined,
          category: v.category?.trim() || undefined
        });

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: s => {
          this.snack.open(this.isEdit ? 'Compétence mise à jour.' : 'Compétence créée.', 'OK', { duration: 2200 });
          this.dialogRef.close(s);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[training-skill-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
