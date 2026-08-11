import { Directionality } from '@angular/cdk/bidi';
import { Component, Input, OnChanges, Optional } from '@angular/core';

import {
  buildFishboneLayout,
  FishboneBranchInput,
  FishboneLayout
} from './ishikawa-fishbone.layout';

/** Les `id` cités par `aria-labelledby` doivent être uniques dans le document. */
let instanceSeq = 0;

/**
 * Le diagramme d'Ishikawa en arête de poisson, dessiné en SVG.
 *
 * <p>Pas d'ECharts : il n'y a ni série, ni axe, ni échelle. Il y a une figure
 * conventionnelle — celle que tout qualiticien reconnaît au premier coup d'œil
 * — dont le tracé se calcule à partir du nombre de branches et de causes.
 *
 * <p>Accessibilité (WCAG 2.2 AA). Le SVG porte `role="img"`, un `<title>` et
 * un `<desc>` qui annoncent ce que la figure montre et combien elle porte de
 * branches. Son contenu interne est délibérément opaque aux lecteurs d'écran :
 * une arête lue trait par trait n'a aucun sens. L'équivalent textuel est la
 * série de cartes de branche située sous le diagramme — elle porte les
 * libellés complets, la hiérarchie des sous-causes dépliée, et les commandes.
 */
@Component({
  selector: 'qos-ishikawa-fishbone',
  templateUrl: './ishikawa-fishbone.component.html',
  styleUrls: ['./ishikawa-fishbone.component.scss'],
  standalone: false
})
export class IshikawaFishboneComponent implements OnChanges {

  /** Énoncé du problème : c'est lui que porte la tête. */
  @Input() problem = '';
  /** Une entrée par catégorie du mode retenu (6M, 7M ou 8M). */
  @Input() branches: readonly FishboneBranchInput[] = [];

  layout: FishboneLayout = buildFishboneLayout('', [], { rtl: false });

  private readonly seq = ++instanceSeq;
  readonly titleId = `qos-fishbone-title-${this.seq}`;
  readonly descId = `qos-fishbone-desc-${this.seq}`;
  readonly arrowId = `qos-fishbone-arrow-${this.seq}`;

  constructor(@Optional() private readonly directionality: Directionality | null) {}

  ngOnChanges(): void {
    this.layout = buildFishboneLayout(this.problem, this.branches ?? [], { rtl: this.isRtl });
  }

  /**
   * En arabe, la tête doit se trouver à GAUCHE : l'effet est le point d'arrivée
   * de la lecture, et le laisser à droite ferait remonter l'épine à contresens.
   * Le miroir ne porte que sur les abscisses ; le texte garde son sens propre.
   */
  get isRtl(): boolean {
    return this.directionality?.value === 'rtl';
  }

  get hasDiagram(): boolean {
    return this.layout.bones.length > 0;
  }

  get diagramTitle(): string {
    return $localize`:@@ishikawa.diagram.title:Diagramme en arête de poisson`;
  }

  get diagramDescription(): string {
    const branchCount = this.branches?.length ?? 0;
    const causeCount = (this.branches ?? [])
      .reduce((sum, b) => sum + b.causes.length, 0);
    return $localize`:@@ishikawa.diagram.desc:Arête de poisson : ${branchCount}:branches: familles de causes et ${causeCount}:causes: causes de premier niveau convergeant vers le problème. Le détail, sous-causes comprises, est repris dans les cartes qui suivent.`;
  }

  /** Marque une cause qui en cache d'autres — la hiérarchie est dans la liste. */
  subCountLabel(descendants: number): string {
    return `+${descendants}`;
  }

  trackByKey(_index: number, item: { key: string }): string {
    return item.key;
  }
}
