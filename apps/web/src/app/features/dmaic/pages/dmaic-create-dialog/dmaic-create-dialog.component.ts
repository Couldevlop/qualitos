import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DmaicService } from '../../dmaic.service';
import { DmaicProjectResponse } from '../../dmaic.types';

@Component({
  selector: 'qos-dmaic-create-dialog',
  templateUrl: './dmaic-create-dialog.component.html',
  styleUrls: ['./dmaic-create-dialog.component.scss'],
  standalone: false
})
export class DmaicCreateDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    problemStatement: [''],
    goalStatement:    [''],
    championId:       [''],
    targetCompletionDate: [''],
    specLowerLimit: [null as number | null],
    specUpperLimit: [null as number | null],
    specTarget:     [null as number | null],
    specUnit: ['', [Validators.maxLength(50)]],
    estimatedSavingsEur: [null as number | null]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: DmaicService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<DmaicCreateDialogComponent, DmaicProjectResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const blackBeltId = this.auth.snapshot()?.userId;
    if (!blackBeltId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.createProject({
      title: v.title.trim(),
      problemStatement: v.problemStatement?.trim() || undefined,
      goalStatement:    v.goalStatement?.trim()    || undefined,
      blackBeltId,
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
          this.snack.open($localize`:@@dmaic.create.created:Projet DMAIC créé.`, 'OK', { duration: 2500 });
          this.dialogRef.close(p);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[dmaic-create] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
