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
 *
 * <p>La fiche portait quatre champs quand le cabinet n'était qu'un
 * porte-consultants. Elle en porte douze depuis qu'il figure sur un accord
 * d'engagement : « Cabinet Durand » sans forme juridique, sans adresse et sans
 * numéro de TVA ne désigne personne de vérifiable, alors que nommer qui entre
 * chez le client de façon opposable est tout l'objet de cet accord.
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
    legalName: ['', [Validators.maxLength(255)]],
    legalForm: ['', [Validators.maxLength(64)]],
    registrationNumber: ['', [Validators.maxLength(64)]],
    vatNumber: ['', [Validators.maxLength(32)]],
    addressLine1: ['', [Validators.maxLength(255)]],
    addressLine2: ['', [Validators.maxLength(255)]],
    postalCode: ['', [Validators.maxLength(16)]],
    city: ['', [Validators.maxLength(128)]],
    // DEUX LETTRES, dans la casse qu'on veut : le serveur met en majuscules
    // avant d'enregistrer. Refuser « fr » ici apprendrait une regle de forme
    // qui ne sert qu'a la machine ; le vide reste permis, le champ est
    // facultatif.
    countryCode: ['', [Validators.pattern(/^([A-Za-z]{2})?$/)]],
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
      legalName: firm.legalName ?? '',
      legalForm: firm.legalForm ?? '',
      registrationNumber: firm.registrationNumber ?? '',
      vatNumber: firm.vatNumber ?? '',
      addressLine1: firm.addressLine1 ?? '',
      addressLine2: firm.addressLine2 ?? '',
      postalCode: firm.postalCode ?? '',
      city: firm.city ?? '',
      countryCode: firm.countryCode ?? '',
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
      legalName: v.legalName.trim() || null,
      legalForm: v.legalForm.trim() || null,
      registrationNumber: v.registrationNumber.trim() || null,
      vatNumber: v.vatNumber.trim() || null,
      addressLine1: v.addressLine1.trim() || null,
      addressLine2: v.addressLine2.trim() || null,
      postalCode: v.postalCode.trim() || null,
      city: v.city.trim() || null,
      countryCode: v.countryCode.trim().toUpperCase() || null,
      contactEmail: v.contactEmail.trim() || null,
      contactPhone: v.contactPhone.trim() || null
    });
  }

  cancel(): void { this.dialogRef.close(); }
}
