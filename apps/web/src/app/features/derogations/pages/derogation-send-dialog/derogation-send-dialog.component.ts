import { Component, Inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { DerogationLanguage } from '../../derogations.types';

export interface DerogationSendDialogData {
  reference: string;
  /** Le contact qualité du client, proposé s'il a été saisi. Modifiable. */
  adresseProposee: string | null;
}

export interface DerogationSendDialogResult {
  customerEmail: string;
  comment: string | null;
  language: DerogationLanguage;
}

/**
 * L'envoi au client : le seul geste qui sort de l'organisation.
 *
 * Deux champs ajoutés, et exactement ceux demandés : l'adresse du client et un
 * commentaire. La langue s'y joint parce qu'aucun document ne doit partir dans
 * une langue que personne n'a choisie — le serveur la refuse absente, et la
 * fige ensuite sur le dossier.
 *
 * La fenêtre dit ce que l'envoi engage. Un dossier parti ne se rouvre plus : le
 * client détient un document signé, et le modifier en silence le démentirait
 * sans qu'il en sache rien.
 */
@Component({
  selector: 'qos-derogation-send-dialog',
  templateUrl: './derogation-send-dialog.component.html',
  styleUrls: ['./derogation-send-dialog.component.scss'],
  standalone: false
})
export class DerogationSendDialogComponent {

  readonly formulaire = new FormGroup({
    customerEmail: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(320)]
    }),
    comment: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(20000)] }),
    language: new FormControl<DerogationLanguage>('FR',
      { nonNullable: true, validators: [Validators.required] })
  });

  constructor(
    private readonly dialogRef: MatDialogRef<
      DerogationSendDialogComponent, DerogationSendDialogResult>,
    @Inject(MAT_DIALOG_DATA) readonly data: DerogationSendDialogData
  ) {
    if (data.adresseProposee) {
      this.formulaire.controls.customerEmail.setValue(data.adresseProposee);
    }
  }

  envoyer(): void {
    if (this.formulaire.invalid) {
      this.formulaire.markAllAsTouched();
      return;
    }
    const v = this.formulaire.getRawValue();
    const commentaire = v.comment.trim();
    this.dialogRef.close({
      customerEmail: v.customerEmail.trim(),
      comment: commentaire || null,
      language: v.language
    });
  }

  annuler(): void {
    this.dialogRef.close();
  }
}
