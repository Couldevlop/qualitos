import { Component, Inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface DerogationSendBackDialogData {
  reference: string;
}

/**
 * Le renvoi pour correction.
 *
 * <p>LE MOTIF EST OBLIGATOIRE, et c'est tout l'objet de cette fenêtre. Le
 * manager qualité ne refuse pas un dossier — refuser usurperait la décision du
 * client, seul à pouvoir accorder la dérogation : il signe, ou il renvoie. Et
 * renvoyer sans dire quoi corriger laisse l'initiateur deviner, ce qui fait un
 * aller-retour de plus à chaque fois.
 *
 * <p>Le texte saisi est la SEULE chose que l'initiateur verra du refus de
 * signer : il tient lieu d'explication, pas de formalité.
 */
@Component({
  selector: 'qos-derogation-send-back-dialog',
  templateUrl: './derogation-send-back-dialog.component.html',
  styleUrls: ['./derogation-send-back-dialog.component.scss'],
  standalone: false
})
export class DerogationSendBackDialogComponent {

  readonly formulaire = new FormGroup({
    reason: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(20000)]
    })
  });

  constructor(
    private readonly dialogRef: MatDialogRef<DerogationSendBackDialogComponent, string>,
    @Inject(MAT_DIALOG_DATA) readonly data: DerogationSendBackDialogData
  ) {}

  renvoyer(): void {
    const motif = this.formulaire.controls.reason.value.trim();
    if (!motif) {
      // Un motif fait d'espaces n'est pas un motif : on le refuse comme vide,
      // sinon le contrôle de présence se contourne par une touche d'espace.
      this.formulaire.controls.reason.setErrors({ required: true });
      this.formulaire.markAllAsTouched();
      return;
    }
    this.dialogRef.close(motif);
  }

  annuler(): void {
    this.dialogRef.close();
  }
}
