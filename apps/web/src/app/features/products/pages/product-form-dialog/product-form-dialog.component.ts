import { Component, Inject, Optional } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ProductsService } from '../../products.service';
import { ProductResponse } from '../../products.types';

export interface ProductFormDialogData {
  product?: ProductResponse;
}

/**
 * Création et modification d'un produit.
 *
 * <p>La référence ne se modifie plus après coup : elle sert de clé humaine, et
 * la renommer casserait la lecture de tout ce qui la cite — PFMEA, control plan,
 * non-conformités.
 */
@Component({
  selector: 'qos-product-form-dialog',
  templateUrl: './product-form-dialog.component.html',
  styleUrls: ['./product-form-dialog.component.scss'],
  standalone: false
})
export class ProductFormDialogComponent {

  readonly form: FormGroup;
  readonly editing: boolean;
  saving = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: ProductsService,
    private readonly dialogRef: MatDialogRef<ProductFormDialogComponent>,
    private readonly snack: MatSnackBar,
    @Optional() @Inject(MAT_DIALOG_DATA) private readonly data: ProductFormDialogData | null
  ) {
    const product = this.data?.product;
    this.editing = !!product;
    this.form = this.fb.group({
      code: [{ value: product?.code ?? '', disabled: this.editing },
        [Validators.required, Validators.maxLength(64),
          Validators.pattern(/^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$/)]],
      designation: [product?.designation ?? '', [Validators.required, Validators.maxLength(250)]],
      family: [product?.family ?? '', Validators.maxLength(120)],
      revisionIndex: [product?.revisionIndex ?? '', Validators.maxLength(16)],
      customerLabel: [product?.customerLabel ?? '', Validators.maxLength(250)],
      siteLabel: [product?.siteLabel ?? '', Validators.maxLength(250)]
    });
  }

  save(): void {
    if (this.form.invalid || this.saving) return;
    this.saving = true;
    const value = this.form.getRawValue();
    const request = this.editing
      ? this.service.update(this.data!.product!.id, {
          designation: value.designation,
          family: value.family || undefined,
          revisionIndex: value.revisionIndex || undefined,
          customerLabel: value.customerLabel || undefined,
          siteLabel: value.siteLabel || undefined
        })
      : this.service.create({
          code: value.code,
          designation: value.designation,
          family: value.family || undefined,
          revisionIndex: value.revisionIndex || undefined,
          customerLabel: value.customerLabel || undefined,
          siteLabel: value.siteLabel || undefined
        });

    request.subscribe({
      next: product => this.dialogRef.close(product),
      error: err => {
        this.saving = false;
        this.snack.open(
          err?.status === 409
            ? $localize`:@@product.code-conflict:Cette référence est déjà utilisée.`
            : $localize`:@@product.save-failed:Enregistrement impossible.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
