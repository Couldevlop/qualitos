import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ProductsService } from '../../products.service';
import { ProductComponentResponse } from '../../products.types';

export interface ComponentDialogData {
  productId: string;
  component?: ProductComponentResponse;
}

/** Saisie d'une ligne de nomenclature. */
@Component({
  selector: 'qos-component-dialog',
  templateUrl: './component-dialog.component.html',
  styleUrls: ['./component-dialog.component.scss'],
  standalone: false
})
export class ComponentDialogComponent {

  readonly form: FormGroup;
  readonly editing: boolean;
  saving = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: ProductsService,
    private readonly dialogRef: MatDialogRef<ComponentDialogComponent>,
    private readonly snack: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) private readonly data: ComponentDialogData
  ) {
    const component = this.data.component;
    this.editing = !!component;
    this.form = this.fb.group({
      sequenceNo: [component?.sequenceNo ?? 10, [Validators.required, Validators.min(0)]],
      reference: [component?.reference ?? '', [Validators.required, Validators.maxLength(120)]],
      label: [component?.label ?? '', Validators.maxLength(250)],
      quantity: [component?.quantity ?? null],
      unit: [component?.unit ?? '', Validators.maxLength(24)]
    });
  }

  save(): void {
    if (this.form.invalid || this.saving) return;
    this.saving = true;
    const value = this.form.getRawValue();
    const payload = {
      sequenceNo: Number(value.sequenceNo),
      reference: value.reference,
      label: value.label || undefined,
      quantity: value.quantity === null || value.quantity === '' ? undefined : Number(value.quantity),
      unit: value.unit || undefined
    };
    const request = this.editing
      ? this.service.updateComponent(this.data.productId, this.data.component!.id, payload)
      : this.service.addComponent(this.data.productId, payload);

    request.subscribe({
      next: component => this.dialogRef.close(component),
      error: () => {
        this.saving = false;
        this.snack.open($localize`:@@product.save-failed:Enregistrement impossible.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
