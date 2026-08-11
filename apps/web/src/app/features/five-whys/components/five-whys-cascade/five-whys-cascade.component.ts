import { Directionality } from '@angular/cdk/bidi';
import { Component, Input, OnChanges, Optional } from '@angular/core';

import { FiveWhysStep } from '../../five-whys.types';
import { buildCascadeLayout, CascadeLayout } from './five-whys-cascade.layout';

/** Compteur de composants : les `id` référencés par `aria-labelledby` doivent
 *  être uniques dans le document, or rien n'interdit deux cascades sur une
 *  page (comparaison de deux analyses, mode présentation). */
let instanceSeq = 0;

/**
 * La cascade des 5 Pourquoi, en SVG dessiné à la main.
 *
 * <p>Pas d'ECharts ici : ce n'est pas un graphe de données. Il n'y a ni axe,
 * ni échelle, ni série — il y a une chaîne de causes qu'on lit du symptôme
 * vers la racine. Le seul « calcul » est une géométrie, et une bibliothèque de
 * graphiques ne ferait qu'ajouter un poids de bundle pour la contrarier.
 *
 * <p>Accessibilité (WCAG 2.2 AA). Un dessin ne se lit pas au lecteur d'écran.
 * Le SVG porte donc `role="img"` — ce qui rend son contenu interne
 * inaccessible aux technologies d'assistance, volontairement : un empilement
 * de `<text>` sans ordre sémantique serait pire que rien — et il est nommé par
 * un `<title>` et décrit par un `<desc>` qui annonce la profondeur de la
 * chaîne. Le contenu détaillé, lui, reste intégralement dans la liste ordonnée
 * qui suit le dessin dans la page : c'est elle qui porte le texte complet
 * (jamais tronqué) et les commandes. Le dessin illustre, la liste énonce.
 */
@Component({
  selector: 'qos-five-whys-cascade',
  templateUrl: './five-whys-cascade.component.html',
  styleUrls: ['./five-whys-cascade.component.scss'],
  standalone: false
})
export class FiveWhysCascadeComponent implements OnChanges {

  /** Chaîne à tracer, dans n'importe quel ordre : le tracé la remet en rang. */
  @Input() steps: readonly FiveWhysStep[] = [];
  /** Cause racine conclue, s'il y en a une : elle ferme la descente. */
  @Input() rootCause: string | null = null;

  layout: CascadeLayout = { width: 0, height: 0, cards: [], connectors: [] };

  /** Crans de la rampe : un `marker` de flèche par teinte (cf. gabarit). */
  readonly tones = [0, 1, 2, 3];

  private readonly seq = ++instanceSeq;
  readonly titleId = `qos-cascade-title-${this.seq}`;
  readonly descId = `qos-cascade-desc-${this.seq}`;
  /** Les `marker` SVG sont référencés par `url(#id)` : ils doivent être uniques. */
  readonly arrowIdPrefix = `qos-cascade-arrow-${this.seq}`;

  constructor(@Optional() private readonly directionality: Directionality | null) {}

  ngOnChanges(): void {
    this.layout = buildCascadeLayout(
      (this.steps ?? []).map(s => ({ id: s.id, position: s.position, answer: s.answer })),
      { rtl: this.isRtl, rootCause: this.rootCause }
    );
  }

  /**
   * Sens de lecture. En arabe la descente doit partir de la droite : sinon
   * l'escalier « recule », et la flèche de progression contredit le sens de
   * lecture au lieu de l'accompagner. Le miroir est appliqué aux abscisses par
   * la géométrie ; le texte, lui, reste dans son sens naturel.
   */
  get isRtl(): boolean {
    return this.directionality?.value === 'rtl';
  }

  get hasDiagram(): boolean {
    return this.layout.cards.length > 0;
  }

  /** Titre accessible du dessin. */
  get diagramTitle(): string {
    return $localize`:@@fivewhys.diagram.title:Cascade des pourquoi`;
  }

  /**
   * Description accessible : elle dit ce que le dessin MONTRE (une descente de
   * n pourquoi, conclue ou non), pas ce qu'il contient — le contenu est dans
   * la liste qui suit.
   */
  get diagramDescription(): string {
    const depth = this.steps?.length ?? 0;
    return this.rootCause
      ? $localize`:@@fivewhys.diagram.desc-concluded:Descente de ${depth}:count: pourquoi jusqu'à la cause racine. Le détail de chaque réponse est repris dans la liste qui suit.`
      : $localize`:@@fivewhys.diagram.desc-open:Descente de ${depth}:count: pourquoi, sans cause racine conclue à ce jour. Le détail de chaque réponse est repris dans la liste qui suit.`;
  }

  /** Libellé du rang, pour la légende de chaque encart. */
  caption(position: number): string {
    return $localize`:@@fivewhys.diagram.step:Pourquoi n°${position}:rank:`;
  }

  get rootCaption(): string {
    return $localize`:@@fivewhys.root-cause-title:Cause racine`;
  }

  trackByKey(_index: number, item: { key: string }): string {
    return item.key;
  }
}
