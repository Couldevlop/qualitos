import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PageBreadcrumb } from '../../../../shared/ui/page-header/page-header.component';
import { ROLES_PILOTAGE } from '../../../risk-register/pages/register/register.component';
import { RiskCapaOpener } from '../../../risk-register/risk-capa-opener.service';
import {
  TYPES, levelOf, originLabel, riskLevelLabel, typeLabel
} from '../../../risk-register/risk-register.labels';
import { RiskRegisterService } from '../../../risk-register/risk-register.service';
import { RegisterType, RiskLevel, RiskSheet, RiskView } from '../../../risk-register/risk-register.types';
import { RatingView } from '../../smi.types';

/** Les dossiers CAPA encore à mener : ni clos, ni rejetés. */
const CAPA_EN_COURS = (status: string) => status !== 'CLOSED' && status !== 'REJECTED';

/**
 * « Risques d'une case » : ce que contient une case de la matrice du SMI.
 *
 * <p>Toutes les cases mènent ici, avec leur gravité et leur probabilité dans
 * l'adresse (`?g=4&p=3&vue=gross&norme=iso-9001`). La liste se filtre par type,
 * processus et site ; la bascule brut / résiduel relit la case sur l'autre
 * cotation. Le panneau de droite détaille le risque choisi et permet d'ouvrir
 * une action CAPA sans quitter l'écran.
 */
@Component({
  selector: 'qos-smi-risk-cell',
  templateUrl: './risk-cell.component.html',
  styleUrls: ['./risk-cell.component.scss'],
  standalone: false
})
export class RiskCellComponent implements OnInit, OnDestroy {

  readonly types = TYPES;
  readonly editable: boolean;

  severity = 1;
  probability = 1;
  view: RatingView = 'gross';
  standard: string | null = null;

  typeFilter: RegisterType | '' = '';
  processFilter = '';
  siteFilter = '';

  all: RiskView[] = [];
  /** Le nombre de CAPA en cours, par risque — lu dans la fiche de chacun. */
  capaCounts: Record<string, number> = {};
  selected: RiskSheet | null = null;
  loading = false;
  detailLoading = false;
  failed = false;
  busy = false;

  private sub?: Subscription;
  private detailSub?: Subscription;

  constructor(
    private readonly registre: RiskRegisterService,
    private readonly capaOpener: RiskCapaOpener,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snack: MatSnackBar,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_PILOTAGE);
  }

  ngOnInit(): void {
    this.sub = this.route.queryParamMap.subscribe(q => {
      this.severity = note(q.get('g'));
      this.probability = note(q.get('p'));
      this.view = q.get('vue') === 'residual' ? 'residual' : 'gross';
      this.standard = q.get('norme') || null;
      if (!this.all.length && !this.loading) {
        this.charger();
      } else {
        this.afterFilter();
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.detailSub?.unsubscribe();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.registre.risks().subscribe({
      next: risques => {
        this.all = risques.filter(r => r.status !== 'CLOSED');
        this.loading = false;
        this.afterFilter();
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.snack.open(safeErrorMessage(err, $localize`:@@smi.cell.failed:Les risques n'ont pas pu être chargés.`),
          undefined, { duration: 5000 });
      }
    });
  }

  // ---------- filtres ----------

  /** Les risques de la case, sur la cotation choisie, dans la norme filtrée. */
  get inCell(): RiskView[] {
    const prefixe = this.standard ? this.standard.toUpperCase().replace(/-/g, '_') + '_' : null;
    return this.all.filter(r => {
      const g = this.view === 'gross' ? r.grossSeverity : r.residualSeverity;
      const p = this.view === 'gross' ? r.grossProbability : r.residualProbability;
      return g === this.severity && p === this.probability
        && (!prefixe || r.requirements.some(x => x.startsWith(prefixe)));
    });
  }

  get rows(): RiskView[] {
    return this.inCell.filter(r =>
      (!this.typeFilter || r.type === this.typeFilter)
      && (!this.processFilter || r.process === this.processFilter)
      && (!this.siteFilter || r.site === this.siteFilter));
  }

  get processes(): string[] {
    return distinct(this.inCell.map(r => r.process));
  }

  get sites(): string[] {
    return distinct(this.inCell.map(r => r.site).filter((s): s is string => !!s));
  }

  get score(): number {
    return this.severity * this.probability;
  }

  get level(): RiskLevel {
    return levelOf(this.score);
  }

  /** « 2 risques · niveau élevé (12) » : ce que la case contient, sur la cotation lue. */
  get cellTitle(): string {
    return $localize`:@@smi.cell.title:${this.rows.length}:count: risque(s) · niveau ${riskLevelLabel(this.level).toLowerCase()}:level: (${this.score}:score:)`;
  }

  setView(view: RatingView): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: { vue: view }, queryParamsHandling: 'merge' });
  }

  onFilterChange(): void {
    this.afterFilter();
  }

  /** Après tout changement : compter les CAPA des lignes visibles, garder ou choisir la ligne détaillée. */
  private afterFilter(): void {
    const lignes = this.rows;
    this.loadCapaCounts(lignes);
    const courant = this.selected?.risk.id;
    if (!courant || !lignes.some(r => r.id === courant)) {
      if (lignes.length) {
        this.select(lignes[0]);
      } else {
        this.selected = null;
      }
    }
  }

  /** Une case porte quelques risques : leurs fiches se lisent en parallèle, une erreur n'en prive pas les autres. */
  private loadCapaCounts(lignes: RiskView[]): void {
    const manquants = lignes.filter(r => this.capaCounts[r.id] === undefined).slice(0, 25);
    if (!manquants.length) return;
    forkJoin(manquants.map(r => this.registre.risk(r.id).pipe(
      map(s => [r.id, s.capas.filter(c => CAPA_EN_COURS(c.status)).length] as const),
      catchError(() => of(null))
    ))).subscribe(resultats => {
      const comptes = { ...this.capaCounts };
      for (const r of resultats) {
        if (r) comptes[r[0]] = r[1];
      }
      this.capaCounts = comptes;
    });
  }

  // ---------- détail ----------

  select(r: RiskView): void {
    this.detailSub?.unsubscribe();
    this.detailLoading = true;
    this.detailSub = this.registre.risk(r.id).subscribe({
      next: s => {
        this.selected = s;
        this.detailLoading = false;
        this.capaCounts = { ...this.capaCounts, [r.id]: s.capas.filter(c => CAPA_EN_COURS(c.status)).length };
      },
      error: err => {
        this.detailLoading = false;
        this.snack.open(safeErrorMessage(err, $localize`:@@rr.detail.failed:La fiche est indisponible.`),
          undefined, { duration: 5000 });
      }
    });
  }

  createCapa(): void {
    const r = this.selected?.risk;
    if (!r || !this.editable || this.busy) return;
    this.busy = true;
    this.capaOpener.open({ id: r.id, reference: r.reference, owner: r.owner }).subscribe({
      next: capa => {
        this.busy = false;
        if (!capa) return;
        this.snack.open($localize`:@@rr.detail.capa-opened:Dossier CAPA ouvert.`, undefined, { duration: 2500 });
        this.select(r);
      },
      error: err => {
        this.busy = false;
        this.snack.open(safeErrorMessage(err, $localize`:@@rr.detail.failed:La fiche est indisponible.`),
          undefined, { duration: 5000 });
      }
    });
  }

  openRisk(): void {
    if (this.selected) this.router.navigate(['/risques', this.selected.risk.id]);
  }

  get activeCapas(): number {
    return this.selected ? this.selected.capas.filter(c => CAPA_EN_COURS(c.status)).length : 0;
  }

  get breadcrumbs(): PageBreadcrumb[] {
    return [
      { label: $localize`:@@smi.crumb:Tableau de bord SMI`, route: '/smi' },
      { label: $localize`:@@smi.crumb-matrix:Matrice des risques`, route: '/smi' },
      { label: $localize`:@@smi.cell.crumb:Gravité ${this.severity}:severity: × probabilité ${this.probability}:probability:` }
    ];
  }

  typeText = typeLabel;
  originText = originLabel;
  levelText = riskLevelLabel;
  levelOf = levelOf;

  trackById(_i: number, r: { id: string }): string {
    return r.id;
  }
}

function note(v: string | null): number {
  const n = Number(v);
  return Number.isInteger(n) && n >= 1 && n <= 5 ? n : 1;
}

function distinct(values: string[]): string[] {
  return [...new Set(values)].sort((a, b) => a.localeCompare(b));
}
