import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import { SupplierResponse, SupplierType } from '../../suppliers.types';

export interface SuppliersEditDialogData { supplier: SupplierResponse; }

@Component({
  selector: 'qos-suppliers-edit-dialog',
  templateUrl: './suppliers-edit-dialog.component.html',
  styleUrls: ['./suppliers-edit-dialog.component.scss'],
  standalone: false
})
export class SuppliersEditDialogComponent {

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
    name: [this.data.supplier.name, [Validators.required, Validators.maxLength(250)]],
    countryCode: [this.data.supplier.countryCode ?? '', [Validators.pattern(/^[A-Z]{2}$/)]],
    contactEmail: [this.data.supplier.contactEmail ?? '', [Validators.email, Validators.maxLength(320)]],
    supplierType: [this.data.supplier.supplierType, [Validators.required]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly svc: SuppliersService,
    private readonly snack: MatSnackBar,
    private readonly dialogRef: MatDialogRef<SuppliersEditDialogComponent, SupplierResponse>,
    @Inject(MAT_DIALOG_DATA) public readonly data: SuppliersEditDialogData
  ) {}

  submit(): void {
    if (this.form.invalid || this.submitting) { this.form.markAllAsTouched(); return; }
    this.submitting = true;
    const v = this.form.getRawValue();
    this.svc.update(this.data.supplier.id, {
      name: v.name.trim(),
      countryCode: v.countryCode?.trim().toUpperCase() || undefined,
      contactEmail: v.contactEmail?.trim() || undefined,
      supplierType: v.supplierType
    })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: s => { this.snack.open('Fournisseur mis à jour.', 'OK', { duration: 2500 }); this.dialogRef.close(s); },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[suppliers-edit] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de la mise à jour.'), 'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
