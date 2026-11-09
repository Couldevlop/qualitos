import { Component } from '@angular/core';
import { catalogOptions } from '../../../../core/catalogs/catalog-overrides';
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

  /** Les valeurs de la plateforme : le repli quand le client n'a rien réglé. */
  private readonly typesPlateforme: { value: SupplierType; label: string }[] = [
    { value: 'RAW_MATERIAL',          label: $localize`:@@suppliers.type.raw-material:Matière première` },
    { value: 'COMPONENT',             label: $localize`:@@suppliers.type.component:Composant` },
    { value: 'SERVICE',               label: $localize`:@@suppliers.type.service:Service` },
    { value: 'CONTRACT_MANUFACTURER', label: $localize`:@@suppliers.type.contract-manufacturer:Sous-traitant` },
    { value: 'SOFTWARE',              label: $localize`:@@suppliers.create.type-software:Logiciel / SaaS` },
    { value: 'LOGISTICS',             label: $localize`:@@suppliers.type.logistics:Logistique` },
    { value: 'OTHER',                 label: $localize`:@@suppliers.type.other:Autre` }
  ];

    /** Dans l'ordre et les mots du client (ADR 0111). */
  readonly types = catalogOptions<SupplierType>('supplier-type', this.typesPlateforme);

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
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
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
          this.snack.open($localize`:@@suppliers.create.created:Fournisseur créé (PROSPECT).`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.dialogRef.close(s);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[suppliers-create] failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@common.error-create:Erreur lors de la création.`), $localize`:@@common.ok:OK`, { duration: 4000 });
        }
      });
  }

  cancel(): void { this.dialogRef.close(); }
}
