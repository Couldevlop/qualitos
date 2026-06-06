import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import { NonConformityResponse, NonConformitySeverity } from '../../suppliers.types';

export interface SuppliersNcDialogData { supplierId: string; }

@Component({
  selector: 'qos-suppliers-nc-dialog',
  templateUrl: './suppliers-nc-dialog.component.html',
  styleUrls: ['./suppliers-nc-dialog.component.scss'],
  standalone: false
})
export class SuppliersNcDialogComponent {

  submitting = false;

  readonly severities: { value: NonConformitySeverity; label: string }[] = [
    { value: 'MINOR',    label: $localize`:@@suppliers.nc.severity-minor:Mineure` },
    { value: 'MAJOR',    label: $localize`:@@suppliers.nc.severity-major:Majeure` },
    { value: 'CRITICAL', label: $localize`:@@suppliers.nc.severity-critical:Critique` }
  ];

  readonly form = this.fb.nonNullable.group({
    detectedOn: ['', [Validators.required]],
    severity:   ['MAJOR' as NonConformitySeverity, [Validators.required]],
    lotReference: ['', [Validators.maxLength(100)]],
    description:  ['', [Validators.maxLength(1000)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SuppliersService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SuppliersNcDialogComponent, NonConformityResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SuppliersNcDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.addNc(this.data.supplierId, {
      detectedOn: v.detectedOn,
      severity: v.severity,
      lotReference: v.lotReference?.trim() || undefined,
      description:  v.description?.trim()  || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: nc => { this.snack.open($localize`:@@suppliers.nc.saved:NC fournisseur enregistrée.`, $localize`:@@common.ok:OK`, { duration: 2500 }); this.dialogRef.close(nc); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[suppliers-nc] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@suppliers.save-error:Erreur lors de l'enregistrement.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
