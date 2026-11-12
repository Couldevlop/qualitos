import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Router } from '@angular/router';

import { FiveWhysAnalysis } from '../../../five-whys/five-whys.types';
import {
  FishboneBranchInput
} from '../../../ishikawa/components/ishikawa-fishbone/ishikawa-fishbone.layout';
import { buildBranchViews, toFishboneBranches } from '../../../ishikawa/ishikawa-branches';
import { IshikawaDiagramResponse } from '../../../ishikawa/ishikawa.types';

/**
 * L'analyse de cause d'un écart, <b>dessinée</b> : les chaînes 5 Pourquoi en
 * cascade, les diagrammes d'Ishikawa en arête de poisson.
 *
 * <p>Composant d'affichage pur, sans appel réseau : l'écran appelant fournit ce
 * qu'il a chargé. Deux écrans le montrent — la fiche de l'écart (ADR 0114) et le
 * <b>rapport 8D</b>, dont le D4 est précisément cette analyse (ADR 0115). Le 8D
 * n'en portait que les libellés en texte : on y lisait la cause retenue sans
 * voir le raisonnement qui y mène, alors que c'est le raisonnement qu'un
 * auditeur vient regarder.
 *
 * <p>Il MONTRE et ne modifie pas : ajouter une sous-cause demande l'arbre
 * entier, le plan d'actions et les suggestions IA — c'est l'écran de la méthode,
 * vers lequel chaque bloc porte un lien.
 */
@Component({
  selector: 'qos-nc-cause-analysis',
  templateUrl: './nc-cause-analysis.component.html',
  styleUrls: ['./nc-cause-analysis.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class NcCauseAnalysisComponent {

  /** Les chaînes 5 Pourquoi rattachées à l'écart. */
  @Input() fiveWhys: FiveWhysAnalysis[] = [];

  /** Les diagrammes d'Ishikawa rattachés à l'écart. */
  @Input() ishikawa: IshikawaDiagramResponse[] = [];

  constructor(private readonly router: Router) {}

  /** Vrai dès qu'il y a quelque chose à dessiner : l'hôte décide de son cadre. */
  get hasAnything(): boolean {
    return this.fiveWhys.length > 0 || this.ishikawa.length > 0;
  }

  /**
   * Les branches dessinables d'un diagramme, mémorisées par RÉFÉRENCE.
   *
   * <p>Le gabarit appelle la méthode à chaque cycle de détection, et le tracé
   * (repli de texte compris) se recalcule à chaque appel : sans mémoire, taper
   * une lettre ailleurs dans l'écran redessinerait l'arête. Le cache tombe de
   * lui-même au rechargement, qui ramène d'autres objets.
   */
  private readonly branchesCache = new WeakMap<IshikawaDiagramResponse, FishboneBranchInput[]>();

  /** Les branches à dessiner pour ce diagramme, dans l'ordre des M. */
  fishboneBranches(diagram: IshikawaDiagramResponse): FishboneBranchInput[] {
    const deja = this.branchesCache.get(diagram);
    if (deja) {
      return deja;
    }
    const branches = toFishboneBranches(buildBranchViews(diagram));
    this.branchesCache.set(diagram, branches);
    return branches;
  }

  /**
   * Un clic sur une cause du dessin ouvre le diagramme complet.
   *
   * <p>Emmener là-bas vaut mieux qu'une figure cliquable qui ne répondrait pas.
   */
  openCause(diagram: IshikawaDiagramResponse): void {
    this.router.navigate(['/ishikawa', diagram.id]);
  }

  trackByAnalyse(_: number, analyse: FiveWhysAnalysis): string {
    return analyse.id;
  }

  trackByDiagramme(_: number, diagramme: IshikawaDiagramResponse): string {
    return diagramme.id;
  }
}
