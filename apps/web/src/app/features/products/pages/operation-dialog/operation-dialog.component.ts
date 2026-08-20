import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ProductsService } from '../../products.service';
import { ProductOperationResponse } from '../../products.types';

export interface OperationDialogData {
  productId: string;
  operation?: ProductOperationResponse;
}

/**
 * Saisie d'une opération de gamme.
 *
 * <p>C'est le mot commun entre le PFMEA et le control plan : sans elle, les deux
 * documents parlent d'un « poste » en texte libre et ne se recoupent jamais.
 */
@Component({
  selector: 'qos-operation-dialog',
  templateUrl: './operation-dialog.component.html',
  styleUrls: ['./operation-dialog.component.scss'],
  standalone: false
})
export class OperationDialogComponent {

  readonly form: FormGroup;
  readonly editing: boolean;
  saving = false;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: ProductsService,
    private readonly dialogRef: MatDialogRef<OperationDialogComponent>,
    private readonly snack: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) private readonly data: OperationDialogData
  ) {
    const operation = this.data.operation;
    this.editing = !!operation;
    this.form = this.fb.group({
      sequenceNo: [operation?.sequenceNo ?? 10, [Validators.required, Validators.min(0)]],
      code: [operation?.code ?? '', [Validators.required, Validators.maxLength(32)]],
      label: [operation?.label ?? '', [Validators.required, Validators.maxLength(250)]],
      workstation: [operation?.workstation ?? '', Validators.maxLength(120)]
    });
  }

  save(): void {
    if (this.form.invalid || this.saving) return;
    this.saving = true;
    const value = this.form.getRawValue();
    const payload = {
      sequenceNo: Number(value.sequenceNo),
      code: value.code,
      label: value.label,
      workstation: value.workstation || undefined
    };
    const request = this.editing
      ? this.service.updateOperation(this.data.productId, this.data.operation!.id, payload)
      : this.service.addOperation(this.data.productId, payload);

    request.subscribe({
      next: operation => this.dialogRef.close(operation),
      error: err => {
        this.saving = false;
        this.snack.open(
          err?.status === 409
            ? $localize`:@@product.operation-conflict:Ce code d'opération existe déjà sur ce produit.`
            : $localize`:@@product.save-failed:Enregistrement impossible.`,
          $localize`:@@common.ok:OK`, { duration: 4000 });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
