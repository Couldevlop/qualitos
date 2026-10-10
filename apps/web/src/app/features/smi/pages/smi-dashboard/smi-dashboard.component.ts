import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, Subject, Subscription, merge } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { levelOf, riskLevelLabel } from '../../../risk-register/risk-register.labels';
import { RiskLevel } from '../../../risk-register/risk-register.types';
import { dueText, upcomingLabel, upcomingLink } from '../../smi.labels';
import { SmiService } from '../../smi.service';
import { RatingView, SmiDashboard, Upcoming } from '../../smi.types';

/** Une case de la matrice, prête à afficher : gravité 5 en haut, probabilité 1 à gauche. */
export interface GridCell {
  severity: number;
  probability: number;
  count: number;
  level: RiskLevel;
}

/**
 * Le tableau de bord du système de management intégré (SMI).
 *
 * <p>La maquette `docs/Tableau de bord SMI.pptx` : quatre indicateurs en tête
 * (conformité, actions en retard, risques majeurs, prochain audit), puis la
 * conformité par norme, la matrice des risques et les échéances de la semaine.
 * Chaque case de la matrice ouvre la liste de ses risques ; le titre « Matrice
 * des exigences », en bas, ouvre la page qui croise chapitres et normes.
 *
 * <p>Le filtre de norme vit dans l'adresse (`?norme=iso-9001`) : un lien
 * partagé ouvre la même vue.
 */
@Component({
  selector: 'qos-smi-dashboard',
  templateUrl: './smi-dashboard.component.html',
  styleUrls: ['./smi-dashboard.component.scss'],
  standalone: false
})
export class SmiDashboardComponent implements OnInit, OnDestroy {

  readonly severities = [5, 4, 3, 2, 1];
  readonly probabilities = [1, 2, 3, 4, 5];

  data: SmiDashboard | null = null;
  standard: string | null = null;
  ratingView: RatingView = 'gross';
  /** Calculée à chaque chargement ou bascule, pas à chaque détection : la recréer redessinerait la grille. */
  grid: GridCell[][] = [];
  /** Risques placés dans la grille affichée — la résiduelle laisse hors d'elle ceux qui n'en ont pas. */
  placed = 0;
  loading = false;
  error: string | null = null;

  private readonly retry$ = new Subject<void>();
  private sub?: Subscription;

  constructor(
    private readonly service: SmiService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const norme$ = this.route.queryParamMap.pipe(map(q => q.get('norme') || null), distinctUntilChanged());
    this.sub = merge(norme$, this.retry$.pipe(map(() => this.standard))).pipe(
      tap(norme => {
        this.standard = norme;
        this.loading = true;
        this.error = null;
      }),
      // switchMap : changer de norme pendant un chargement annule le précédent,
      // sans quoi une réponse lente pourrait écraser la vue demandée ensuite.
      // L'erreur est rattrapée DANS la branche : le flux du filtre survit, et
      // la norme suivante se charge normalement.
      switchMap(norme => this.service.dashboard(norme).pipe(
        catchError(err => {
          this.loading = false;
          this.error = safeErrorMessage(err,
            $localize`:@@smi.load-failed:Le tableau de bord SMI n'a pas pu être chargé.`);
          return EMPTY;
        })))
    ).subscribe(d => {
      this.data = d;
      this.standard = d.selected;
      this.loading = false;
      this.rebuildGrid();
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  reload(): void {
    this.retry$.next();
  }

  setView(view: RatingView): void {
    this.ratingView = view;
    this.rebuildGrid();
  }

  selectStandard(code: string | null): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { norme: code ?? null },
      queryParamsHandling: 'merge'
    });
  }

  /** La grille de la cotation choisie, gravité 5 en tête comme sur la maquette. */
  private rebuildGrid(): void {
    const m = this.data?.riskMatrix;
    const source = m ? (this.ratingView === 'gross' ? m.gross : m.residual) : null;
    this.grid = this.severities.map(g => this.probabilities.map(p => ({
      severity: g,
      probability: p,
      count: source?.[g - 1]?.[p - 1] ?? 0,
      level: levelOf(g * p)
    })));
    this.placed = this.grid.reduce((n, ligne) => n + ligne.reduce((s, c) => s + c.count, 0), 0);
  }

  cellQuery(c: GridCell): Record<string, string | number> {
    const q: Record<string, string | number> = { g: c.severity, p: c.probability, vue: this.ratingView };
    if (this.standard) q['norme'] = this.standard;
    return q;
  }

  cellLabel(c: GridCell): string {
    return $localize`:@@smi.matrix.cell-label:Gravité ${c.severity}:severity: × probabilité ${c.probability}:probability: : ${c.count}:count: risque(s), niveau ${riskLevelLabel(c.level)}:level:`;
  }

  /** Largeur de barre bornée à 0..100 : un score hors bornes ne déborde pas de sa piste. */
  barWidth(score: number | null | undefined): number {
    return Math.max(0, Math.min(100, score ?? 0));
  }

  upcomingText = upcomingLabel;
  upcomingRoute = (u: Upcoming) => upcomingLink(u.kind, u.targetId);
  due = dueText;

  trackByCode(_i: number, s: { code: string }): string {
    return s.code;
  }

  trackByUpcoming(_i: number, u: Upcoming): string {
    return `${u.kind}-${u.targetId}-${u.dueOn}`;
  }
}
