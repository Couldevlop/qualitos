import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import { SupplierResponse, SupplierType } from '../../suppliers.types';

@Component({
  selector: 'qos-suppliers-create-dialog',
  templateUrl: './suppliers-create-dialog.component.html',
  styleUrls: ['./suppliers-create-dialog.component.scss'],
  standalone: false
})
export class SuppliersCreateDialogComponent {

  submitting = false;

  readonly types: { value: SupplierType; label: string }[] = [
    { value: 'RAW_MATERIAL',          label: 'Matière première' },
    { value: 'COMPONENT',             label: 'Composant' },
    { value: 'SERVICE',               label: 'Service' },
    { value: 'CONTRACT_MANUFACTURER', label: 'Sous-traitant' },
    { value: 'SOFTWARE',              label: 'Logiciel / SaaS' },
    { value: 'LOGISTICS',             label: 'Logistique' },
    { value: 'OTHER',                 label: 'Autre' }
  ];

  readonly form = this.fb.nonNullable.group({
    code: ['', [
      Validators.required, Validators.maxLength(120),
      Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9._\-]{0,119}$/)
    ]],
    name: ['', [Validators.required, Validators.maxLength(250)]],
    countryCode: ['', [Validators.pattern(/^[A-Z]{2}$/)]],
    contactEmail: ['', [Validators.email, Validators.maxLength(320)]],
    supplierType: ['COMPONENT' as SupplierType, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SuppliersService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SuppliersCreateDialogComponent, SupplierResponse>
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    const createdBy = this.auth.snapshot()?.userId;
    if (!createdBy) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.create({
      code: v.code.trim(),
      name: v.name.trim(),
      countryCode: v.countryCode?.trim().toUpperCase() || undefined,
      contactEmail: v.contactEmail?.trim() || undefined,
      supplierType: v.supplierType,
      createdBy
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: s => {
          this.snack.open('Fournisseur créé (PROSPECT).', 'OK', { duration: 2500 });
          this.dialogRef.close(s);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[suppliers-create] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de la création.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
