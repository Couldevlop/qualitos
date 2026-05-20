import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditType } from '../../audits.types';

@Component({
  selector: 'qos-audits-create-dialog',
  templateUrl: './audits-create-dialog.component.html',
  styleUrls: ['./audits-create-dialog.component.scss'],
  standalone: false
})
export class AuditsCreateDialogComponent {

  submitting = false;

  readonly types: { value: AuditType; label: string }[] = [
    { value: 'INTERNAL',      label: 'Interne' },
    { value: 'EXTERNAL',      label: 'Externe' },
    { value: 'SUPPLIER',      label: 'Fournisseur' },
    { value: 'LPA',           label: 'LPA (Layered Process Audit)' },
    { value: 'CERTIFICATION', label: 'Certification' },
    { value: 'SURVEILLANCE',  label: 'Surveillance' }
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
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<AuditsCreateDialogComponent, AuditPlanResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    const leadAuditorId = this.auth.snapshot()?.userId;
    if (!leadAuditorId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.audits
      .createPlan({
        title: v.title.trim(),
        scope: v.scope?.trim() || undefined,
        type: v.type,
        standard: v.standard?.trim() || undefined,
        scheduledDate: v.scheduledDate || undefined,
        leadAuditorId
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: plan => {
          this.snack.open("Plan d'audit créé.", 'OK', { duration: 2500 });
          this.dialogRef.close(plan);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-create] failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de la création.'),
            'OK',
            { duration: 4000 }
          );
        }
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
