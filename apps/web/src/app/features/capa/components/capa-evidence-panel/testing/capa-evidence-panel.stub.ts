import { Component, Input, NgModule } from '@angular/core';

/**
 * Doublure du panneau des preuves du dossier, pour les écrans qui l'hébergent.
 *
 * <p>Le panneau a son propre banc : les bancs de la fiche d'écart ne vérifient
 * que son <b>branchement</b> — reçoit-il le bon dossier, et son statut ?
 * L'importer en vrai y ferait appeler le service CAPA et ouvrir une fenêtre de
 * confirmation, deux dépendances étrangères à ce qu'on mesure.
 *
 * <p>Elle expose <b>exactement</b> les mêmes entrées que l'original : plus
 * permissive, elle laisserait passer un branchement sur un attribut inexistant.
 */
@Component({
  selector: 'qos-capa-evidence-panel',
  template: '',
  standalone: false
})
export class CapaEvidencePanelStubComponent {
  @Input() capaCaseId = '';
  @Input() statut: string | undefined;
  @Input() sansAide = false;
}

@NgModule({
  declarations: [CapaEvidencePanelStubComponent],
  exports: [CapaEvidencePanelStubComponent]
})
export class CapaEvidencePanelStubModule {}
