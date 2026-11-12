import { Component, EventEmitter, Input, NgModule, Output } from '@angular/core';

/**
 * Doublure du panneau d'action CAPA, pour les écrans qui l'hébergent.
 *
 * <p>Le panneau a son propre banc (`capa-action-panel.component.spec.ts`) : les
 * bancs de la fiche d'écart ne vérifient que son <b>branchement</b> — reçoit-il
 * le bon dossier, et l'écran se relit-il quand il annonce un changement ?
 * L'importer en vrai y ferait charger la fenêtre de création et appeler le
 * service CAPA, deux dépendances qui n'ont rien à voir avec ce qu'on mesure.
 *
 * <p>La doublure expose <b>exactement</b> la même entrée et la même sortie que
 * l'original : plus permissive, elle laisserait passer un branchement sur un
 * attribut qui n'existe pas.
 */
@Component({
  selector: 'qos-capa-action-panel',
  template: '',
  standalone: false
})
export class CapaActionPanelStubComponent {
  @Input() capaCaseId = '';
  @Output() readonly changed = new EventEmitter<void>();
}

@NgModule({
  declarations: [CapaActionPanelStubComponent],
  exports: [CapaActionPanelStubComponent]
})
export class CapaActionPanelStubModule {}
