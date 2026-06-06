import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditType } from '../../audits.types';

export interface AuditsEditDialogData {
  plan: AuditPlanResponse;
}

@Component({
  selector: 'qos-audits-edit-dialog',
  templateUrl: './audits-edit-dialog.component.html',
  styleUrls: ['./audits-edit-dialog.component.scss'],
  standalone: false
})
export class AuditsEditDialogComponent {
  submitting = false;

  readonly types: { value: AuditType; label: string }[] = [
    { value: 'INTERNAL',      label: $localize`:@@audits.type.internal:Interne` },
    { value: 'EXTERNAL',      label: $localize`:@@audits.type.external:Externe` },
    { value: 'SUPPLIER',      label: $localize`:@@audits.type.supplier:Fournisseur` },
    { value: 'LPA',           label: $localize`:@@audits.type.lpa-short:LPA` },
    { value: 'CERTIFICATION', label: $localize`:@@audits.type.certification:Certification` },
    { value: 'SURVEILLANCE',  label: $localize`:@@audits.type.surveillance:Surveillance` }
  ];

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    scope: [''],
    type: ['INTERNAL' as AuditType, [Validators.required]],
    standard: ['', [Validators.maxLength(100)]],
    scheduledDate: ['']
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly audits: AuditsService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsEditDialogComponent, AuditPlanResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: AuditsEditDialogData
  ) {
    this.form.patchValue({
      title: data.plan.title,
      scope: data.plan.scope ?? '',
      type: data.plan.type,
      standard: data.plan.standard ?? '',
      scheduledDate: data.plan.scheduledDate ?? ''
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.audits
      .updatePlan(this.data.plan.id, {
        title: v.title.trim(),
        scope: v.scope?.trim() || undefined,
        type: v.type,
        standard: v.standard?.trim() || undefined,
        scheduledDate: v.scheduledDate || undefined
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: p => {
          this.snack.open($localize`:@@audits.edit.success:Plan mis à jour.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(p);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-edit] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-update:Erreur lors de la mise à jour.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
