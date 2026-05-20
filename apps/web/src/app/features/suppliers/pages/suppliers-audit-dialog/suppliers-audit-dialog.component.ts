import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import { AuditResponse } from '../../suppliers.types';

export interface SuppliersAuditDialogData { supplierId: string; }

@Component({
  selector: 'qos-suppliers-audit-dialog',
  templateUrl: './suppliers-audit-dialog.component.html',
  styleUrls: ['./suppliers-audit-dialog.component.scss'],
  standalone: false
})
export class SuppliersAuditDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    auditedOn: ['', [Validators.required]],
    score:     [80, [Validators.required, Validators.min(0), Validators.max(100)]],
    criticalFindingsCount: [0, [Validators.min(0)]],
    majorFindingsCount:    [0, [Validators.min(0)]],
    minorFindingsCount:    [0, [Validators.min(0)]],
    findingsSummary: ['', [Validators.maxLength(2000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SuppliersService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SuppliersAuditDialogComponent, AuditResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SuppliersAuditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    const auditorUserId = this.auth.snapshot()?.userId;
    this.svc.addAudit(this.data.supplierId, {
      auditedOn: v.auditedOn,
      score: v.score,
      auditorUserId,
      findingsSummary: v.findingsSummary?.trim() || undefined,
      criticalFindingsCount: v.criticalFindingsCount ?? 0,
      majorFindingsCount:    v.majorFindingsCount    ?? 0,
      minorFindingsCount:    v.minorFindingsCount    ?? 0
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: a => { this.snack.open('Audit enregistré.', 'OK', { duration: 2500 }); this.dialogRef.close(a); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[suppliers-audit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
