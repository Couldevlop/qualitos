import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

/** Ce que la console passe : le cabinet, et son nom pour le titre. */
export interface FirmMailDialogData {
  firmId: string;
  firmName: string;
}

/**
 * La messagerie d'un cabinet partenaire, dans une fenêtre.
 *
 * <p>Elle n'héberge que l'écran de messagerie — le même que celui du client,
 * avec son cabinet pour cible. Elle n'ajoute aucune règle : tout ce qui vaut
 * pour une messagerie vaut ici, et le dupliquer pour un cabinet aurait fait
 * diverger la moitié qu'on ouvre le moins.
 *
 * <p>Une fenêtre plutôt qu'une route, parce qu'on y vient depuis la fiche d'un
 * cabinet et qu'on y retourne : une page aurait fait perdre la liste, ses
 * filtres et le cabinet qu'on venait de déplier.
 *
 * <p>Pas de bouton « Enregistrer » ici : il est **dans** l'écran, qui sauvegarde
 * lui-même. En ajouter un à la fenêtre aurait donné deux boutons pour une seule
 * action, dont un qui ne ferait rien.
 */
@Component({
  selector: 'qos-firm-mail-dialog',
  templateUrl: './firm-mail-dialog.component.html',
  styleUrls: ['./firm-mail-dialog.component.scss'],
  standalone: false
})
export class FirmMailDialogComponent {

  constructor(
    @Inject(MAT_DIALOG_DATA) public readonly data: FirmMailDialogData,
    private readonly ref: MatDialogRef<FirmMailDialogComponent>
  ) {}

  fermer(): void {
    this.ref.close();
  }
}
