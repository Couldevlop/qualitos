import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

/** Ce qu'il faut pour ouvrir la fenêtre : le dossier dont on lit les actions. */
export interface CapaActionsDialogData {
  capaCaseId: string;
  /** La référence de l'écart d'où l'on vient, pour que la fenêtre dise de quoi elle parle. */
  reference?: string;
}

/**
 * Les actions d'un dossier CAPA, lues depuis la fiche de l'écart.
 *
 * <p>La fiche porte déjà le tableau, en bas. Cette fenêtre existe pour le geste
 * inverse : on arrive par le bouton, en haut, parce qu'on vient AJOUTER une
 * action — et faire défiler l'écran jusqu'au tableau pour trouver le bouton
 * d'ajout était le chemin le plus long vers le geste le plus fréquent.
 *
 * <p>Elle ne duplique rien : c'est le même composant de tableau
 * ({@code qos-capa-action-panel}), avec les mêmes droits et la même fenêtre
 * d'action. Une jumelle divergerait au premier champ ajouté d'un seul côté.
 */
@Component({
  selector: 'qos-capa-actions-dialog',
  templateUrl: './capa-actions-dialog.component.html',
  styleUrls: ['./capa-actions-dialog.component.scss'],
  standalone: false
})
export class CapaActionsDialogComponent {

  /** Vrai dès qu'une action a été ajoutée ou corrigée : l'écran appelant se relit. */
  private touche = false;

  constructor(
    private readonly dialogRef: MatDialogRef<CapaActionsDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) readonly data: CapaActionsDialogData
  ) {}

  onChanged(): void {
    this.touche = true;
  }

  close(): void {
    this.dialogRef.close(this.touche);
  }
}
