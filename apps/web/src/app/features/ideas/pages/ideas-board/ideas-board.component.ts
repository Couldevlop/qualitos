import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { IdeasService } from '../../ideas.service';
import { Idea, IdeaColumn, IdeaStatus } from '../../ideas.types';
import { IdeaDialogComponent } from '../idea-dialog/idea-dialog.component';
import { IdeaRejectDialogComponent } from '../idea-reject-dialog/idea-reject-dialog.component';

/** Qui arbitre. Miroir exact du contrôle posé côté serveur. */
const ROLES_ARBITRAGE = ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'ADMIN_TENANT', 'SUPER_ADMIN'];

/** Les colonnes affichées, et les statuts que chacune regroupe. */
const COLONNES: { titre: string; statuts: IdeaStatus[] }[] = [
  { titre: $localize`:@@ideas.col-submitted:Soumise`, statuts: ['PROPOSED'] },
  { titre: $localize`:@@ideas.col-review:À l'étude`, statuts: ['UNDER_REVIEW'] },
  { titre: $localize`:@@ideas.col-approved:En cours`, statuts: ['APPROVED'] },
  // Une idée dont l'impact a été mesuré reste réalisée : en faire une cinquième
  // colonne distinguerait à l'écran deux états que le lecteur lit comme un seul.
  { titre: $localize`:@@ideas.col-done:Réalisée`, statuts: ['IMPLEMENTED', 'MEASURED'] }
];

/**
 * La boîte à idées.
 *
 * <p>Déposer et soutenir sont ouverts à tous : c'est l'opérateur qui voit le
 * rebut à son poste. Seul l'arbitrage — retenir, écarter, marquer réalisée —
 * demande un rôle, et l'écran ne propose alors pas un geste qui répondrait 403.
 */
@Component({
  selector: 'qos-ideas-board',
  templateUrl: './ideas-board.component.html',
  styleUrls: ['./ideas-board.component.scss'],
  standalone: false
})
export class IdeasBoardComponent implements OnInit {

  colonnes: { titre: string; ideas: Idea[] }[] = [];
  ecartees: Idea[] = [];
  montrerEcartees = false;
  loading = false;

  readonly editable: boolean;

  constructor(
    private readonly service: IdeasService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_ARBITRAGE);
  }

  ngOnInit(): void {
    this.charger();
  }

  trackById(_index: number, idea: Idea): string {
    return idea.id;
  }

  /**
   * Identité d'une colonne : son titre, stable puisqu'il vient de la même
   * référence `COLONNES` à chaque rechargement.
   *
   * <p>Sans ce `trackBy`, `charger()` et `remplacer()` posent un tableau NEUF
   * de colonnes à chaque vote ou geste d'arbitrage — y compris pour les
   * colonnes non concernées. Angular comparerait alors par identité, ne
   * reconnaîtrait aucune des 4 colonnes, et détruirait puis recréerait tout
   * le sous-arbre DOM (les 4 sections, donc toutes les cartes) à chaque clic
   * sur un vote — rendant inopérant le `trackBy` des cartes elles-mêmes,
   * détruites avec leur hôte avant de pouvoir s'appliquer.
   */
  trackByColonne(_index: number, colonne: { titre: string }): string {
    return colonne.titre;
  }

  /**
   * Soutenir une idée, ou retirer sa voix.
   *
   * <p>Aucun compteur optimiste : le décompte vient du serveur, qui est le seul
   * à voir les voix des autres. L'incrémenter localement afficherait un chiffre
   * faux jusqu'au prochain chargement, et faux d'autant plus que l'idée est
   * populaire.
   */
  basculerVote(idea: Idea): void {
    const appel = idea.votedByMe
      ? this.service.unvote(idea.id)
      : this.service.vote(idea.id);

    appel.subscribe({
      next: mise => this.remplacer(mise),
      error: err => this.echouer(err)
    });
  }

  deposer(): void {
    const ref = this.dialog.open(IdeaDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true
    });
    ref.afterClosed().subscribe(saisie => {
      if (!saisie) return;
      this.service.submit(saisie).subscribe({
        // On recharge : la nouvelle idée entre dans une colonne, et son rang
        // dépend des autres. L'insérer localement devinerait cet ordre.
        next: () => this.charger(),
        error: err => this.echouer(err)
      });
    });
  }

  etudier(idea: Idea): void {
    this.service.review(idea.id).subscribe({
      next: () => this.charger(),
      error: err => this.echouer(err)
    });
  }

  retenir(idea: Idea): void {
    this.service.approve(idea.id).subscribe({
      next: () => this.charger(),
      error: err => this.echouer(err)
    });
  }

  ecarter(idea: Idea): void {
    const ref = this.dialog.open(IdeaRejectDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      // Le même dialogue sert au refus et à l'impact : un seul champ libre
      // obligatoire, deux libellés. Il rend `{ text }`, que l'appelant traduit
      // en la requête qui lui convient.
      data: {
        title: idea.title,
        prompt: $localize`:@@ideas.reject-prompt:Pourquoi cette idée est-elle écartée ?`,
        submitLabel: $localize`:@@ideas.reject-submit:Écarter`
      }
    });
    ref.afterClosed().subscribe(saisie => {
      if (!saisie) return;
      this.service.reject(idea.id, { reason: saisie.text }).subscribe({
        next: () => this.charger(),
        error: err => this.echouer(err)
      });
    });
  }

  realiser(idea: Idea): void {
    this.service.implement(idea.id).subscribe({
      next: () => this.charger(),
      error: err => this.echouer(err)
    });
  }

  /** Consigner l'impact : le pendant du refus, au même dialogue. */
  mesurer(idea: Idea): void {
    const ref = this.dialog.open(IdeaRejectDialogComponent, {
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      data: {
        title: idea.title,
        prompt: $localize`:@@ideas.impact-prompt:Qu'a produit cette idée, une fois en place ?`,
        submitLabel: $localize`:@@ideas.impact-submit:Consigner`
      }
    });
    ref.afterClosed().subscribe(saisie => {
      if (!saisie) return;
      this.service.measure(idea.id, { impactNote: saisie.text }).subscribe({
        next: () => this.charger(),
        error: err => this.echouer(err)
      });
    });
  }

  ariaVote(idea: Idea): string {
    return idea.votedByMe
      ? $localize`:@@ideas.unvote-aria:Retirer ma voix de ${idea.title}`
      : $localize`:@@ideas.vote-aria:Soutenir ${idea.title}`;
  }

  // ---------- interne ----------

  private charger(): void {
    this.loading = true;
    this.service.board().subscribe({
      next: tableau => {
        this.colonnes = COLONNES.map(c => ({
          titre: c.titre,
          ideas: c.statuts.flatMap(s => this.idees(tableau.columns, s))
        }));
        this.ecartees = this.idees(tableau.columns, 'REJECTED');
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.echouer(err);
      }
    });
  }

  private idees(colonnes: IdeaColumn[], statut: IdeaStatus): Idea[] {
    return colonnes.find(c => c.status === statut)?.ideas ?? [];
  }

  /** Remplace une idée sur place : un vote ne déplace rien dans le tableau. */
  private remplacer(mise: Idea): void {
    this.colonnes = this.colonnes.map(c => ({
      titre: c.titre,
      ideas: c.ideas.map(i => i.id === mise.id ? mise : i)
    }));
  }

  private echouer(err: unknown): void {
    this.snack.open(
      safeErrorMessage(err, $localize`:@@ideas.failed:Opération impossible sur cette idée.`),
      $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
