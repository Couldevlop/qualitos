import { Component, Inject, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { TrainingService } from '../../training.service';
import { PathResponse } from '../../training.types';

export interface TrainingPathDialogData {
  path?: PathResponse;
}

@Component({
  selector: 'qos-training-path-dialog',
  templateUrl: './training-path-dialog.component.html',
  styleUrls: ['./training-path-dialog.component.scss'],
  standalone: false
})
export class TrainingPathDialogComponent {

  submitting = false;
  readonly isEdit: boolean;

  readonly editTitle = $localize`:@@training.path-dialog.edit-title:Modifier le parcours`;
  readonly createTitle = $localize`:@@training.path-dialog.create-title:Nouveau parcours`;
  readonly editSubmitLabel = $localize`:@@common.save:Enregistrer`;
  readonly createSubmitLabel = $localize`:@@common.create:Créer`;

  readonly form;

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: TrainingService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<TrainingPathDialogComponent, PathResponse>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: TrainingPathDialogData | null
  ) {
    this.isEdit = !!data?.path;
    const p = data?.path;
    this.form = this.fb.nonNullable.group({
      code: [
        { value: p?.code ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(100), Validators.pattern(/^[a-z0-9][a-z0-9_-]{1,99}$/)]
      ],
      name:        [p?.name ?? '',        [Validators.required, Validators.maxLength(200)]],
      description: [p?.description ?? '', [Validators.maxLength(2000)]],
      targetRole:  [p?.targetRole ?? '',  [Validators.maxLength(100)]],
      durationHours:  [p?.durationHours  ?? 8,  [Validators.required, Validators.min(1), Validators.max(10000)]],
      passingScore:   [p?.passingScore   ?? 70, [Validators.min(0), Validators.max(100)]],
      validityMonths: [p?.validityMonths ?? null as number | null, [Validators.min(1), Validators.max(120)]]
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();

    const op$ = this.isEdit
      ? this.svc.updatePath(this.data!.path!.id, {
          name: v.name.trim(),
          description: v.description?.trim() || undefined,
          targetRole:  v.targetRole?.trim()  || undefined,
          durationHours:  v.durationHours,
          passingScore:   v.passingScore   ?? undefined,
          validityMonths: v.validityMonths ?? undefined
        })
      : (() => {
          const createdBy = this.auth.snapshot()?.userId;
          if (!createdBy) {
            this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.createPath({
            code: v.code.trim(),
            name: v.name.trim(),
            description: v.description?.trim() || undefined,
            targetRole:  v.targetRole?.trim()  || undefined,
            durationHours:  v.durationHours,
            passingScore:   v.passingScore   ?? undefined,
            validityMonths: v.validityMonths ?? undefined,
            createdBy
          });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => {
          this.snack.open(this.isEdit ? $localize`:@@training.path-dialog.updated:Parcours mis à jour.` : $localize`:@@training.path-dialog.created:Parcours créé.`, $localize`:@@common.ok:OK`, { duration: 2200 });
          this.dialogRef.close(p);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[training-path-dialog] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@training.path-dialog.save-error:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
