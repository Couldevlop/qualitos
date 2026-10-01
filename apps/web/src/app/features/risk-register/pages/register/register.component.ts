import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { RiskRegisterService } from '../../risk-register.service';
import {
  OPPORTUNITY_STATUSES, RISK_STATUSES, TYPES, opportunityLevelLabel, opportunityStatusLabel,
  riskLevelLabel, riskStatusLabel, typeLabel
} from '../../risk-register.labels';
import {
  OpportunityLevel, OpportunityStatus, OpportunityView, RegisterType, RiskLevel, RiskStatus, RiskView
} from '../../risk-register.types';
import { csvCell } from '../../risk-register.csv';

/** Qui écrit : le serveur tranche (`RiskRegisterController.ROLES_PILOTAGE`), l'écran n'affiche que ce qui servira. */
export const ROLES_PILOTAGE =
  ['QUALITY_MANAGER', 'DIRECTOR_QUALITY', 'QUALITY_DIRECTOR', 'ADMIN_TENANT', 'SUPER_ADMIN'];

export type RegisterTab = 'risks' | 'opportunities';
/** Sur quelle cotation se lit et se trie le registre des risques. */
export type RatingView = 'gross' | 'residual';

/**
 * Le registre unique des risques et opportunités.
 *
 * <p>Deux onglets qui se relient : chacun a sa route, pour qu'un lien vers les
 * opportunités ouvre les opportunités. Le registre entier est chargé une fois —
 * quelques centaines de lignes au plus — et filtré ici, sans aller-retour à
 * chaque frappe.
 */
@Component({
  selector: 'qos-risk-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
  standalone: false
})
export class RegisterComponent implements OnInit {

  readonly types = TYPES;
  readonly riskStatuses = RISK_STATUSES;
  readonly opportunityStatuses = OPPORTUNITY_STATUSES;
  readonly editable: boolean;
  readonly searchRiskPlaceholder = $localize`:@@rr.search-risk:Rechercher un risque`;
  readonly searchOpportunityPlaceholder = $localize`:@@rr.search-opportunity:Rechercher une opportunité`;

  tab: RegisterTab = 'risks';
  risks: RiskView[] = [];
  opportunities: OpportunityView[] = [];
  loading = false;
  failed = false;

  search = '';
  ratingView: RatingView = 'gross';
  priorityOnly = false;
  typeFilter: RegisterType | '' = '';
  processFilter = '';
  statusFilter = '';

  constructor(
    private readonly service: RiskRegisterService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snack: MatSnackBar,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_PILOTAGE);
  }

  ngOnInit(): void {
    this.tab = this.route.snapshot.data['tab'] === 'opportunities' ? 'opportunities' : 'risks';
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    forkJoin({ risks: this.service.risks(), opportunities: this.service.opportunities() }).subscribe({
      next: ({ risks, opportunities }) => {
        this.risks = risks;
        this.opportunities = opportunities;
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.snack.open(safeErrorMessage(err, $localize`:@@rr.load-failed:Le registre n'a pas pu être chargé.`),
          undefined, { duration: 5000 });
      }
    });
  }

  // ---------- onglets ----------

  /** Change d'onglet ET d'adresse : l'onglet se partage par un lien. */
  setTab(tab: RegisterTab): void {
    if (tab === this.tab) return;
    this.tab = tab;
    this.statusFilter = '';
    this.router.navigate(tab === 'risks' ? ['/risques'] : ['/risques', 'opportunites']);
  }

  // ---------- filtres ----------

  /** Les processus présents dans l'onglet courant, pour le filtre. */
  get processes(): string[] {
    const source = this.tab === 'risks' ? this.risks : this.opportunities;
    return [...new Set(source.map(r => r.process))].sort((a, b) => a.localeCompare(b));
  }

  get filteredRisks(): RiskView[] {
    const lignes = this.risks.filter(r =>
      this.matches(r.reference, r.title, r.owner, r.process)
      && (!this.typeFilter || r.type === this.typeFilter)
      && (!this.processFilter || r.process === this.processFilter)
      && (!this.statusFilter || r.status === this.statusFilter));
    // Les plus lourds d'abord, sur la cotation choisie ; une résiduelle absente
    // se lit comme la brute, faute de mieux — c'est ce qui reste à traiter.
    return [...lignes].sort((a, b) => this.ratingOf(b) - this.ratingOf(a)
      || a.reference.localeCompare(b.reference));
  }

  get filteredOpportunities(): OpportunityView[] {
    const lignes = this.opportunities.filter(o =>
      this.matches(o.reference, o.title, o.owner, o.process)
      && (!this.priorityOnly || o.level === 'PRIORITY')
      && (!this.typeFilter || o.type === this.typeFilter)
      && (!this.processFilter || o.process === this.processFilter)
      && (!this.statusFilter || o.status === this.statusFilter));
    return [...lignes].sort((a, b) => b.score - a.score || a.reference.localeCompare(b.reference));
  }

  ratingOf(r: RiskView): number {
    return this.ratingView === 'residual' ? (r.residualScore ?? r.grossScore) : r.grossScore;
  }

  resetFilters(): void {
    this.search = '';
    this.typeFilter = '';
    this.processFilter = '';
    this.statusFilter = '';
    this.priorityOnly = false;
  }

  private matches(...champs: (string | null)[]): boolean {
    const q = this.search.trim().toLowerCase();
    return !q || champs.some(c => (c ?? '').toLowerCase().includes(q));
  }

  // ---------- navigation ----------

  openRisk(r: RiskView): void {
    this.router.navigate(['/risques', r.id]);
  }

  openOpportunity(o: OpportunityView): void {
    this.router.navigate(['/risques', 'opportunites', o.id]);
  }

  create(): void {
    this.router.navigate(this.tab === 'risks' ? ['/risques', 'nouveau'] : ['/risques', 'opportunites', 'nouvelle']);
  }

  // ---------- export ----------

  /**
   * Les lignes AFFICHÉES, en CSV : ce que l'utilisateur voit est ce qu'il exporte.
   * Chaque cellule passe par `csvCell`, qui neutralise les formules (injection CSV).
   */
  exporter(): void {
    const lignes: string[][] = this.tab === 'risks'
      ? [[
          $localize`:@@rr.col.ref:Réf.`, $localize`:@@rr.col.risk:Risque`, $localize`:@@rr.col.type:Type`,
          $localize`:@@rr.col.process:Processus`, $localize`:@@rr.col.owner:Propriétaire`,
          $localize`:@@rr.col.gross:Cotation brute`, $localize`:@@rr.col.residual:Résiduel`,
          $localize`:@@rr.col.status:Statut`
        ], ...this.filteredRisks.map(r => [
          r.reference, r.title, typeLabel(r.type), r.process, r.owner,
          `${r.grossSeverity}x${r.grossProbability}=${r.grossScore} ${riskLevelLabel(r.grossLevel)}`,
          r.residualScore === null ? '' : `${r.residualScore} ${riskLevelLabel(r.residualLevel)}`,
          riskStatusLabel(r.status)
        ])]
      : [[
          $localize`:@@rr.col.ref:Réf.`, $localize`:@@rr.col.opportunity:Opportunité`,
          $localize`:@@rr.col.type:Type`, $localize`:@@rr.col.process:Processus`,
          $localize`:@@rr.col.owner:Propriétaire`, $localize`:@@rr.col.score:Gain × faisabilité`,
          $localize`:@@rr.col.due:Échéance`, $localize`:@@rr.col.status:Statut`
        ], ...this.filteredOpportunities.map(o => [
          o.reference, o.title, typeLabel(o.type), o.process, o.owner,
          `${o.gain}x${o.feasibility}=${o.score} ${opportunityLevelLabel(o.level)}`,
          o.targetDate ?? '', opportunityStatusLabel(o.status)
        ])];
    const csv = '﻿' + lignes.map(l => l.map(csvCell).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = this.tab === 'risks' ? 'registre-risques.csv' : 'registre-opportunites.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  // ---------- gabarit ----------

  typeText(t: RegisterType): string { return typeLabel(t); }
  riskLevelText(l: RiskLevel | null): string { return riskLevelLabel(l); }
  opportunityLevelText(l: OpportunityLevel): string { return opportunityLevelLabel(l); }
  riskStatusText(s: RiskStatus): string { return riskStatusLabel(s); }
  opportunityStatusText(s: OpportunityStatus): string { return opportunityStatusLabel(s); }

  trackById(_i: number, row: { id: string }): string {
    return row.id;
  }
}
