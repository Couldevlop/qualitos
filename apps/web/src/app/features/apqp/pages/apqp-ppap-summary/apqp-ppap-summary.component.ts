import { Component, EventEmitter, Input, Output } from '@angular/core';

import { ApqpDeliverable, ApqpPhase } from '../../apqp.types';

/** Une ligne du dossier : le livrable, et la phase d'où il vient. */
export interface LignePpap {
  phase: ApqpPhase;
  deliverable: ApqpDeliverable;
}

/**
 * Le dossier PPAP : une VUE du cycle, jamais une seconde liste.
 *
 * <p>Les livrables retenus sont ceux que le référentiel marque d'un astérisque
 * (« this deliverable is a PPAP element »). Tenir à part une liste normative
 * aurait divergé du cycle dès que le client l'adapte — et c'est le cycle qui fait
 * foi, puisqu'il lui appartient.
 *
 * <p>Le compte vient du SERVEUR : deux vues du même cycle doivent afficher le
 * même chiffre, et la règle changera le jour où « acquis » voudra dire « coché ET
 * prouvé ».
 */
@Component({
  selector: 'qos-apqp-ppap-summary',
  templateUrl: './apqp-ppap-summary.component.html',
  styleUrls: ['./apqp-ppap-summary.component.scss'],
  standalone: false
})
export class ApqpPpapSummaryComponent {

  @Input() phases: ApqpPhase[] = [];

  /** Livrables PPAP acquis, comptés par le serveur. */
  @Input() done = 0;

  /** Livrables PPAP au total, comptés par le serveur. */
  @Input() total = 0;

  /** Le parent sait ouvrir le popup : la section ne fait que désigner la ligne. */
  @Output() ouvrir = new EventEmitter<LignePpap>();

  /**
   * Les livrables du dossier, dans l'ordre du cycle.
   *
   * <p>Recalculé à chaque rendu plutôt que mémorisé : la liste suit les cases
   * qu'on coche juste au-dessus, et un cache la laisserait en retard d'un clic.
   * Le cycle compte une cinquantaine de livrables — le parcourir ne coûte rien.
   */
  get lignes(): LignePpap[] {
    const lignes: LignePpap[] = [];
    for (const phase of this.phases) {
      for (const deliverable of phase.deliverables) {
        if (deliverable.ppap) {
          lignes.push({ phase, deliverable });
        }
      }
    }
    return lignes;
  }

  /** Pourcentage de complétude, pour la barre et pour les lecteurs d'écran. */
  get pourcentage(): number {
    return this.total === 0 ? 0 : Math.round((this.done / this.total) * 100);
  }

  get resume(): string {
    return $localize`:@@apqp.ppap.progress:${this.done}:done: / ${this.total}:total: livrables acquis`;
  }

  get ariaBarre(): string {
    return $localize`:@@apqp.ppap.progress-aria:Complétude du dossier PPAP : ${this.pourcentage}:percent: %`;
  }

  ariaLigne(ligne: LignePpap): string {
    return ligne.deliverable.done
      ? $localize`:@@apqp.ppap.row-done:${ligne.deliverable.label}:label: — acquis`
      : $localize`:@@apqp.ppap.row-pending:${ligne.deliverable.label}:label: — à fournir`;
  }

  trackByLigne(_index: number, ligne: LignePpap): string {
    return ligne.deliverable.id;
  }
}
