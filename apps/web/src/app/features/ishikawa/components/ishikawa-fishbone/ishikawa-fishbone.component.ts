import { Directionality } from '@angular/cdk/bidi';
import { Component, EventEmitter, Input, OnChanges, Optional, Output } from '@angular/core';

import {
  buildFishboneLayout,
  FishboneBranchInput,
  FishboneCauseVm,
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
 * <p>Accessibilité (WCAG 2.2 AA). Le SVG porte `role="group"`, un `<title>` et
 * un `<desc>` qui annoncent ce que la figure montre et combien elle porte de
 * branches. Les causes, elles, sont des `role="button"` focusables : depuis
 * que les cartes de branche ont disparu du détail, c'est ICI qu'on ajoute un
 * sous-pourquoi, et une figure qu'on ne peut que regarder rendrait la méthode
 * inutilisable au-delà du premier niveau. Le nom accessible de chaque cause
 * porte son libellé ENTIER — le dessin, lui, tronque — et son score.
 *
 * <p>L'équivalent textuel complet (hiérarchie dépliée, descriptions) reste
 * fourni par le détail sous forme de liste, hors de ce composant.
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

  /**
   * Identifiant de la cause activée (clic, « Entrée » ou barre d'espace).
   *
   * <p>Le composant ne décide PAS ce que l'activation déclenche : il dit
   * seulement laquelle. C'est le détail qui ouvre le dialogue de sous-cause —
   * la figure reste réutilisable ailleurs (impression, export) sans traîner
   * une dépendance vers un dialogue.
   */
  @Output() readonly causeActivate = new EventEmitter<string>();

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
    return $localize`:@@ishikawa.diagram.desc:Arête de poisson : ${branchCount}:branches: familles de causes et ${causeCount}:causes: causes de premier niveau convergeant vers le problème. Activez une cause pour lui ajouter un sous-pourquoi ; le détail complet, sous-causes comprises, est repris en texte sous le diagramme.`;
  }

  /** Marque une cause qui en cache d'autres — la hiérarchie est dans la liste. */
  subCountLabel(descendants: number): string {
    return `+${descendants}`;
  }

  /**
   * Nom accessible d'une cause : ce que le lecteur d'écran annonce, et ce que
   * l'infobulle montre à la souris.
   *
   * <p>Il porte le libellé ENTIER, alors que le dessin le tronque à la largeur
   * de sa colonne : une cause coupée au milieu d'un mot ne s'identifie pas, et
   * c'est précisément sur elle qu'on va cliquer.
   */
  causeActionLabel(cause: FishboneCauseVm): string {
    const scored = cause.score
      ? $localize`:@@ishikawa.diagram.cause-scored:${cause.fullLabel}:label: — ${cause.score}:score:`
      : cause.fullLabel;
    return $localize`:@@ishikawa.diagram.cause-action:Ajouter un sous-pourquoi à : ${scored}:cause:`;
  }

  trackByKey(_index: number, item: { key: string }): string {
    return item.key;
  }
}
