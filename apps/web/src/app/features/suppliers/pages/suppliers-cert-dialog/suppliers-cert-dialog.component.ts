import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import { CertificateResponse } from '../../suppliers.types';

export interface SuppliersCertDialogData { supplierId: string; }

@Component({
  selector: 'qos-suppliers-cert-dialog',
  templateUrl: './suppliers-cert-dialog.component.html',
  styleUrls: ['./suppliers-cert-dialog.component.scss'],
  standalone: false
})
export class SuppliersCertDialogComponent {

  submitting = false;

  readonly form = this.fb.nonNullable.group({
    standardCode: ['', [
      Validators.required, Validators.maxLength(64),
      Validators.pattern(/^[a-z0-9][a-z0-9_-]{1,62}$/)
    ]],
    reference: ['', [Validators.maxLength(200)]],
    issuedOn:  ['', [Validators.required]],
    expiresOn: ['', [Validators.required]],
    documentUrl: ['', [Validators.maxLength(1024)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SuppliersService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SuppliersCertDialogComponent, CertificateResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SuppliersCertDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    if (new Date(v.expiresOn) <= new Date(v.issuedOn)) {
      this.snack.open('La date d\'expiration doit être postérieure à la date d\'émission.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    this.svc.addCert(this.data.supplierId, {
      standardCode: v.standardCode.trim().toLowerCase(),
      reference:    v.reference?.trim() || undefined,
      issuedOn:     v.issuedOn,
      expiresOn:    v.expiresOn,
      documentUrl:  v.documentUrl?.trim() || undefined
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: c => { this.snack.open('Certificat ajouté.', 'OK', { duration: 2500 }); this.dialogRef.close(c); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[suppliers-cert] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
