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
  /** LES dossiers de l'ecart : une discipline n'a qu'un tableau pour tous. */
  @Input() capaCaseIds: readonly string[] = [];
  /** Les natures montrees : vide = toutes (ADR 0115, D5 et D6 du rapport 8D). */
  @Input() natures: string[] = [];
  /** L'equipe de l'ecart, proposee en tete du selecteur de responsable. */
  @Input() team: readonly { accountId: string }[] = [];
  @Input() colonnePreuve = true;
  /** L'ecart cite quand une discipline ouvre une CAPA de plus. */
  @Input() source?: { type: string; reference: string };
  /** L'ecart accepte-t-il encore qu'on decide des actions ? */
  @Input() ajoutAutorise = true;
  @Output() readonly changed = new EventEmitter<void>();
}

@NgModule({
  declarations: [CapaActionPanelStubComponent],
  exports: [CapaActionPanelStubComponent]
})
export class CapaActionPanelStubModule {}
