import { Component } from '@angular/core';

import { APQP_PHASES, ApqpPhase } from '../../apqp.reference';

/**
 * Le cycle APQP, vu d'ensemble.
 *
 * <p>Les cinq phases sont dessinées comme ce qu'elles sont : une suite, où
 * chacune s'appuie sur la précédente. Une liste à puces aurait dit les mêmes
 * mots sans dire cela — or l'ordre EST la méthode, et c'est la seule chose que
 * cet écran a à transmettre avant qu'on entre dans une phase.
 *
 * <p>Chaque phase est un lien : on entre par le schéma, pas par un menu à
 * côté. Le menu latéral les liste aussi, pour y revenir sans repasser ici.
 */
@Component({
  selector: 'qos-apqp-overview',
  templateUrl: './apqp-overview.component.html',
  styleUrls: ['./apqp-overview.component.scss'],
  standalone: false
})
export class ApqpOverviewComponent {

  readonly phases: readonly ApqpPhase[] = APQP_PHASES;

  /** Nombre de livrables d'une phase — ce qui donne son poids d'un coup d'œil. */
  compteLivrables(phase: ApqpPhase): number {
    return phase.livrables.length;
  }

  trackBySlug(_index: number, phase: ApqpPhase): string {
    return phase.slug;
  }
}
