import { Component, Inject, Optional } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ChangesService } from '../../changes.service';
import {
  ChangeRequestPriority,
  ChangeRequestType,
  ChangeResponse
} from '../../changes.types';

export interface ChangesCreateDialogData { change?: ChangeResponse; }

@Component({
  selector: 'qos-changes-create-dialog',
  templateUrl: './changes-create-dialog.component.html',
  styleUrls: ['./changes-create-dialog.component.scss'],
  standalone: false
})
export class ChangesCreateDialogComponent {

  submitting = false;
  readonly isEdit: boolean;
  readonly form;

  readonly types: { value: ChangeRequestType; label: string }[] = [
    { value: 'DOCUMENT',       label: 'Document' },
    { value: 'PROCESS',        label: 'Processus' },
    { value: 'EQUIPMENT',      label: 'Équipement' },
    { value: 'SUPPLIER',       label: 'Fournisseur' },
    { value: 'IT_SYSTEM',      label: 'Système IT' },
    { value: 'ORGANIZATIONAL', label: 'Organisationnel' },
    { value: 'OTHER',          label: 'Autre' }
  ];
  readonly priorities: ChangeRequestPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: ChangesService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<ChangesCreateDialogComponent, ChangeResponse>,
    @Optional() @Inject(MAT_DIALOG_DATA) public readonly data: ChangesCreateDialogData | null
  ) {
    this.isEdit = !!data?.change;
    const c = data?.change;
    this.form = this.fb.nonNullable.group({
      code: [
        { value: c?.code ?? '', disabled: this.isEdit },
        [Validators.required, Validators.maxLength(100), Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9._\-]{1,99}$/)]
      ],
      title:       [c?.title ?? '',       [Validators.required, Validators.maxLength(250)]],
      description: [c?.description ?? '', [Validators.maxLength(4000)]],
      type:        [{ value: (c?.type ?? 'DOCUMENT') as ChangeRequestType, disabled: this.isEdit }, [Validators.required]],
      priority:    [(c?.priority ?? 'MEDIUM') as ChangeRequestPriority, [Validators.required]],
      plannedFor:  [c?.plannedFor ?? ''],
      impactSummary:  [c?.impactSummary ?? '',  [Validators.maxLength(2000)]],
      riskAssessment: [c?.riskAssessment ?? '', [Validators.maxLength(2000)]]
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();

    const op$ = this.isEdit
      ? this.svc.update(this.data!.change!.id, {
          title: v.title.trim(),
          description: v.description?.trim() || undefined,
          priority: v.priority,
          plannedFor: v.plannedFor || undefined,
          impactSummary:  v.impactSummary?.trim()  || undefined,
          riskAssessment: v.riskAssessment?.trim() || undefined
        })
      : (() => {
          const requesterUserId = this.auth.snapshot()?.userId;
          if (!requesterUserId) {
            this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
            this.submitting = false;
            throw new Error('No session');
          }
          return this.svc.create({
            code: v.code.trim(),
            title: v.title.trim(),
            description: v.description?.trim() || undefined,
            type: v.type,
            priority: v.priority,
            plannedFor: v.plannedFor || undefined,
            impactSummary:  v.impactSummary?.trim()  || undefined,
            riskAssessment: v.riskAssessment?.trim() || undefined,
            requesterUserId
          });
        })();

    op$
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => {
          this.snack.open(this.isEdit ? 'Demande mise à jour.' : 'Demande créée.', 'OK', { duration: 2200 });
          this.dialogRef.close(c);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[changes-create] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
