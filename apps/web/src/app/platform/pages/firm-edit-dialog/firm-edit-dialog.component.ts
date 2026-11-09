import { Component, Inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { PartnerFirm, PartnerFirmRequest } from '../../../core/partner/partner.types';

/** Ce que la fenêtre rend : la fiche corrigée, prête pour le serveur. */
export type FirmEditResult = PartnerFirmRequest;

/**
 * Corriger la fiche d'un cabinet partenaire.
 *
 * <p>Un cabinet se déclarait une fois pour toutes : un numéro
 * d'immatriculation saisi de travers, une adresse de contact qui change de
 * main, un nom commercial qui évolue — rien ne se reprenait depuis la console,
 * alors que le serveur acceptait déjà la correction.
 *
 * <p>Le repère (`slug`) est renvoyé tel quel : il nomme le cabinet dans les
 * accords que des clients ont signés, et le changer ferait mentir ces accords.
 * Il est affiché, jamais modifiable.
 */
@Component({
  selector: 'qos-firm-edit-dialog',
  templateUrl: './firm-edit-dialog.component.html',
  styleUrls: ['./firm-edit-dialog.component.scss'],
  standalone: false
})
export class FirmEditDialogComponent {

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(255)]],
    registrationNumber: ['', [Validators.maxLength(64)]],
    contactEmail: ['', [Validators.email, Validators.maxLength(254)]],
    contactPhone: ['', [Validators.maxLength(32)]]
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly dialogRef: MatDialogRef<FirmEditDialogComponent, FirmEditResult>,
    @Inject(MAT_DIALOG_DATA) public readonly firm: PartnerFirm
  ) {
    this.form.setValue({
      name: firm.name,
      registrationNumber: firm.registrationNumber ?? '',
      contactEmail: firm.contactEmail ?? '',
      contactPhone: firm.contactPhone ?? ''
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.dialogRef.close({
      slug: this.firm.slug,
      name: v.name.trim(),
      // Un champ vidé vaut « pas de valeur » : le serveur range le vide en
      // absence, et renvoyer une chaîne vide ferait afficher un contact qui
      // n'en est pas un.
      registrationNumber: v.registrationNumber.trim() || null,
      contactEmail: v.contactEmail.trim() || null,
      contactPhone: v.contactPhone.trim() || null
    });
  }

  cancel(): void { this.dialogRef.close(); }
}
