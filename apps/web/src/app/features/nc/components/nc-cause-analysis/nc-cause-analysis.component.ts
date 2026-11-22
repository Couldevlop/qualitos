import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, Output
} from '@angular/core';
import { Router } from '@angular/router';

import { FiveWhysService } from '../../../five-whys/five-whys.service';
import {
  FiveWhysAnalysis, MIN_WHYS_BEFORE_ROOT_CAUSE
} from '../../../five-whys/five-whys.types';
import {
  FishboneBranchInput
} from '../../../ishikawa/components/ishikawa-fishbone/ishikawa-fishbone.layout';
import { buildBranchViews, toFishboneBranches } from '../../../ishikawa/ishikawa-branches';
import { IshikawaDiagramResponse } from '../../../ishikawa/ishikawa.types';

/**
 * L'analyse de cause d'un écart, <b>dessinée</b> : les diagrammes d'Ishikawa en
 * arête de poisson, puis les chaînes 5 Pourquoi en cascade.
 *
 * <p>Deux outils, deux cases, un seul D. Ils n'ont pas de discipline propre dans
 * le 8D : la méthode de Ford place en D4 la recherche <i>et</i> la vérification
 * de la cause racine, et les deux outils y travaillent. Ils n'y font pourtant
 * pas la même chose, d'où l'ordre — l'Ishikawa ouvre l'éventail des causes
 * possibles, les 5 Pourquoi creusent une branche jusqu'à la cause racine et la
 * vérifient. Chacun garde sa case, son titre et sa figure ; la figure dit tout
 * ce qu'une phrase redirait, et la phrase a été retirée.
 *
 * <p>L'écran appelant fournit ce qu'il a chargé : le composant ne lit rien
 * lui-même. Deux écrans le montrent — la fiche de l'écart (ADR 0114) et le
 * <b>rapport 8D</b>, dont le D4 est précisément cette analyse (ADR 0115). Le 8D
 * n'en portait que les libellés en texte : on y lisait la cause retenue sans
 * voir le raisonnement qui y mène, alors que c'est le raisonnement qu'un
 * auditeur vient regarder.
 *
 * <p>Il montre, et ne modifie qu'UNE chose : la cause racine d'une chaîne 5
 * Pourquoi, conclue dès le troisième pourquoi. Elle se concluait seulement sur
 * l'écran dédié — il fallait donc quitter le D4, où l'on vient précisément de
 * lire la descente, pour écrire ce qu'elle démontre. Tout le reste reste
 * là-bas : ajouter une sous-cause demande l'arbre entier, le plan d'actions et
 * les suggestions IA, et chaque bloc porte le lien qui y mène.
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

  /** Émis après une conclusion acceptée : l'hôte relit ce qu'il affiche. */
  @Output() readonly rootCauseConcluded = new EventEmitter<FiveWhysAnalysis>();

  /** En deçà, la « cause racine » est encore un symptôme. */
  readonly minWhys = MIN_WHYS_BEFORE_ROOT_CAUSE;

  /** La saisie en cours, par chaîne : deux chaînes se concluent séparément. */
  private readonly brouillons = new Map<string, string>();

  /** La chaîne dont la conclusion part vers le serveur, s'il y en a une. */
  envoiEnCours: string | null = null;

  /** Le refus du serveur, affiché sous le champ de la chaîne concernée. */
  echec: string | null = null;

  constructor(
    private readonly router: Router,
    private readonly analyses: FiveWhysService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  /**
   * La chaîne est-elle assez longue pour qu'on en conclue la cause racine ?
   *
   * <p>Même règle que l'écran dédié et que le serveur : deux pourquoi. En deçà,
   * le bloc dit pourquoi il se tait, au lieu de laisser chercher le champ.
   */
  peutConclure(chaine: FiveWhysAnalysis): boolean {
    return (chaine.steps?.length ?? 0) >= this.minWhys;
  }

  /** Ce qui est écrit dans le champ de cette chaîne : son brouillon, sinon l'acquis. */
  brouillon(chaine: FiveWhysAnalysis): string {
    return this.brouillons.get(chaine.id) ?? chaine.rootCause ?? '';
  }

  saisir(chaine: FiveWhysAnalysis, texte: string): void {
    this.brouillons.set(chaine.id, texte);
  }

  /** Rien à envoyer tant que le champ est vide, ou qu'un envoi court déjà. */
  conclusionPrete(chaine: FiveWhysAnalysis): boolean {
    return !!this.brouillon(chaine).trim() && this.envoiEnCours === null;
  }

  /**
   * Écrit la cause racine de cette chaîne.
   *
   * <p>Le refus du serveur est AFFICHÉ : c'est lui qui porte la règle, et
   * l'avaler laisserait croire que la conclusion est enregistrée.
   */
  /**
   * Retire la cause racine de cette chaîne, pour reprendre la remontée.
   *
   * <p>Les pourquoi restent : c'est la conclusion qu'on rouvre, pas l'analyse
   * qu'on refait. Le refus du serveur est AFFICHÉ, comme pour la conclusion :
   * l'avaler laisserait croire que la conclusion est retirée.
   */
  retirerConclusion(chaine: FiveWhysAnalysis): void {
    if (!chaine.rootCause || this.envoiEnCours !== null) {
      return;
    }
    this.envoiEnCours = chaine.id;
    this.echec = null;
    this.analyses.clearRootCause(chaine.id).subscribe({
      next: maj => {
        chaine.rootCause = maj.rootCause;
        this.brouillons.delete(chaine.id);
        this.envoiEnCours = null;
        this.rootCauseConcluded.emit(maj);
        this.cdr.markForCheck();
      },
      error: () => {
        this.envoiEnCours = null;
        this.echec = chaine.id;
        this.cdr.markForCheck();
      }
    });
  }

  conclure(chaine: FiveWhysAnalysis): void {
    const texte = this.brouillon(chaine).trim();
    if (!texte || !this.peutConclure(chaine) || this.envoiEnCours !== null) {
      return;
    }
    this.envoiEnCours = chaine.id;
    this.echec = null;
    this.analyses.setRootCause(chaine.id, texte).subscribe({
      next: maj => {
        chaine.rootCause = maj.rootCause;
        this.brouillons.delete(chaine.id);
        this.envoiEnCours = null;
        this.rootCauseConcluded.emit(maj);
        this.cdr.markForCheck();
      },
      error: () => {
        this.envoiEnCours = null;
        this.echec = chaine.id;
        this.cdr.markForCheck();
      }
    });
  }

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
