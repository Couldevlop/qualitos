import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { EhsService } from '../../ehs.service';
import { IncidentSeverity, IncidentType, IncidentView } from '../../ehs.types';

@Component({
  selector: 'qos-ehs-report-dialog',
  templateUrl: './ehs-report-dialog.component.html',
  styleUrls: ['./ehs-report-dialog.component.scss'],
  standalone: false
})
export class EhsReportDialogComponent {

  submitting = false;

  readonly types: { value: IncidentType; label: string }[] = [
    { value: 'INJURY',          label: $localize`:@@ehs.detail.type-injury:Accident corporel` },
    { value: 'NEAR_MISS',       label: $localize`:@@ehs.type.near-miss:Presque-accident` },
    { value: 'ENVIRONMENTAL',   label: $localize`:@@ehs.report.type-environmental:Environnement (déversement, pollution)` },
    { value: 'SECURITY',        label: $localize`:@@ehs.report.type-security:Sécurité / sûreté` },
    { value: 'PROPERTY_DAMAGE', label: $localize`:@@ehs.type.property-damage:Dommage matériel` },
    { value: 'OTHER',           label: $localize`:@@ehs.type.other:Autre` }
  ];
  readonly severities: IncidentSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly form = this.fb.nonNullable.group({
    // OWASP A03: regex mirrors backend @Pattern ^[A-Za-z0-9][A-Za-z0-9._\-]{1,99}$
    code: ['', [
      Validators.required, Validators.maxLength(100),
      Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9._\-]{1,99}$/)
    ]],
    title:       ['', [Validators.required, Validators.maxLength(250)]],
    description: ['', [Validators.maxLength(4000)]],
    type:        ['NEAR_MISS' as IncidentType, [Validators.required]],
    severity:    ['MEDIUM' as IncidentSeverity, [Validators.required]],
    occurredAt:  [''],
    location:    ['', [Validators.maxLength(500)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: EhsService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<EhsReportDialogComponent, IncidentView>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const reportedBy = this.auth.snapshot()?.userId;
    if (!reportedBy) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.report({
      code: v.code.trim(),
      title: v.title.trim(),
      description: v.description?.trim() || undefined,
      type: v.type,
      severity: v.severity,
      occurredAt: v.occurredAt
        ? new Date(v.occurredAt).toISOString()
        : undefined,
      location: v.location?.trim() || undefined,
      reportedBy
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: i => { this.snack.open($localize`:@@ehs.report.reported:Incident déclaré.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(i); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ehs-report] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@ehs.report.failed:Déclaration impossible.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
