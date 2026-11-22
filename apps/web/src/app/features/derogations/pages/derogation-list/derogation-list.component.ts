import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/auth/auth.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import {
  decisionLabel, STATUS_ORDER, statusLabel, statusTone
} from '../../derogations.labels';
import { DerogationsService } from '../../derogations.service';
import { DerogationRow, DerogationStatus } from '../../derogations.types';

/**
 * Qui peut ouvrir une dérogation.
 *
 * Le serveur réserve ce geste au pilotage (`derogation.create`) ; la maquette ne
 * nomme que deux rôles, « Responsable qualité fournisseur » et « Manager
 * qualité ». Le front liste les deux formes de la direction qualité parce qu'il
 * n'a pas l'alias que le serveur applique (`DIRECTOR_QUALITY`).
 */
const ROLES_SAISIE =
  ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'QUALITY_DIRECTOR', 'ADMIN_TENANT', 'SUPER_ADMIN'];

/** Les deux onglets : le registre, et ce qui attend ma signature. */
export type DerogationTab = 'registre' | 'validations';

/**
 * Le registre des dérogations client.
 *
 * Les colonnes et les filtres suivent la maquette. L'onglet « Mes validations »
 * porte l'alerte rouge et son nombre : c'est par là qu'un manager entre quand un
 * courriel l'a prévenu, et c'est la seule liste qui ne passe pas par la portée du
 * registre — ce qu'on doit signer nous concerne par définition.
 *
 * L'état se tient en champs simples et non en flux : les trois défauts déjà
 * rencontrés sur les listes de ce projet (boucle de démontage, interblocage du
 * drapeau de chargement, sujet non rémanent) naissaient tous d'un assemblage
 * d'observables là où une affectation suffisait.
 */
@Component({
  selector: 'qos-derogation-list',
  templateUrl: './derogation-list.component.html',
  styleUrls: ['./derogation-list.component.scss'],
  standalone: false
})
export class DerogationListComponent implements OnInit {

  readonly statuts = STATUS_ORDER;
  readonly peutCreer: boolean;

  readonly statusLabel = statusLabel;
  readonly statusTone = statusTone;
  readonly decisionLabel = decisionLabel;

  /** Les colonnes de la maquette, dans son ordre — les pièces jointes en dernier. */
  readonly colonnes = ['createdAt', 'reference', 'partReference', 'customerName',
    'createdByName', 'firstDeviation', 'status', 'attachmentsNote'];

  onglet: DerogationTab = 'registre';

  readonly filtres = new FormGroup({
    owner: new FormControl<string | null>(null),
    partReference: new FormControl<string | null>(null),
    customerName: new FormControl<string | null>(null),
    status: new FormControl<DerogationStatus | null>(null)
  });

  lignes: DerogationRow[] = [];
  total = 0;
  page = 0;
  readonly taille = 20;

  /** Le nombre derrière l'alerte rouge. Chargé dès l'arrivée sur l'écran. */
  aValider = 0;

  chargement = false;
  echec = false;

  constructor(
    private readonly service: DerogationsService,
    private readonly router: Router,
    private readonly snack: MatSnackBar,
    auth: AuthService
  ) {
    this.peutCreer = auth.hasAnyRole(ROLES_SAISIE);
  }

  ngOnInit(): void {
    this.charger();
    this.compterMesValidations();
  }

  /**
   * Le compteur se charge même quand l'onglet n'est pas ouvert : c'est tout
   * l'intérêt d'une alerte — elle se voit sans qu'on la cherche. Un échec reste
   * silencieux, un compteur absent valant mieux qu'un bandeau d'erreur sur un
   * écran par ailleurs fonctionnel.
   */
  private compterMesValidations(): void {
    this.service.myApprovalCount().subscribe({
      next: resultat => (this.aValider = resultat.count),
      error: () => (this.aValider = 0)
    });
  }

  ouvrirOnglet(onglet: DerogationTab): void {
    if (this.onglet === onglet) {
      return;
    }
    this.onglet = onglet;
    this.page = 0;
    this.charger();
  }

  appliquerLesFiltres(): void {
    this.page = 0;
    this.charger();
  }

  effacerLesFiltres(): void {
    this.filtres.reset();
    this.page = 0;
    this.charger();
  }

  /** Le filtre « créateur » se choisit dans l'annuaire, jamais à la main. */
  surChoixDuCreateur(personne: DirectoryUser | null): void {
    this.filtres.controls.owner.setValue(personne ? personne.accountId : null);
    this.appliquerLesFiltres();
  }

  pageSuivante(): void {
    if ((this.page + 1) * this.taille < this.total) {
      this.page += 1;
      this.charger();
    }
  }

  pagePrecedente(): void {
    if (this.page > 0) {
      this.page -= 1;
      this.charger();
    }
  }

  charger(): void {
    this.chargement = true;
    this.echec = false;
    const flux = this.onglet === 'validations'
      ? this.service.myApprovals(this.page, this.taille)
      : this.service.search({ ...this.filtres.getRawValue(), page: this.page, size: this.taille });

    flux.subscribe({
      next: page => {
        this.lignes = page.content;
        this.total = page.total;
        this.chargement = false;
      },
      error: erreur => {
        this.lignes = [];
        this.total = 0;
        this.chargement = false;
        this.echec = true;
        this.snack.open(safeErrorMessage(erreur,
          $localize`:@@derogation.error.list:Le registre des dérogations n'a pas pu être lu.`),
          undefined, { duration: 6000 });
      }
    });
  }

  ouvrir(ligne: DerogationRow): void {
    void this.router.navigate(['/derogations', ligne.id]);
  }

  creer(): void {
    void this.router.navigate(['/derogations', 'nouvelle']);
  }

  /** La borne haute affichée de la tranche, bornée au total. */
  get jusqua(): number {
    return Math.min((this.page + 1) * this.taille, this.total);
  }

  get depuis(): number {
    return this.total === 0 ? 0 : this.page * this.taille + 1;
  }
}
