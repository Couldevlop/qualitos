import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, Subject, combineLatest, of } from 'rxjs';
import { catchError, map, shareReplay, take, takeUntil } from 'rxjs/operators';
import type { EChartsCoreOption } from 'echarts/core';

import { ThemeService } from '../../core/theme/theme.service';
import { EchartPointSelection } from '../../shared/ui/echart/echart.component';
import { ncCategoryLabel, severityLabel } from '../../shared/ui/severity-label/severity-labels';
import { DashboardService } from './dashboard.service';
import {
  AiPrediction, AlignmentBar, ComplianceHeatCell, DefectByCategory,
  KpiCard, QualityTrendPoint, TopRisk
} from './dashboard.types';
import { CrossFilter, CrossFilterService } from './interactivity/cross-filter.service';
import { TimeTravelService } from './interactivity/time-travel.service';
import { DashboardSnapshot } from './interactivity/time-travel.types';

/**
 * Dashboard exécutif — vue 360° qualité (Pilotage Direction).
 * Conforme à CLAUDE.md §7.1 — Niveau 1 Executive Dashboard.
 *
 * Interactivité premium (§7.3, ADR 0034/0035) :
 *  - cross-filtering : un clic sur une catégorie du Pareto pose un filtre partagé
 *    qui met en avant la catégorie dans les autres widgets et l'utilise comme
 *    contexte de drill-down et d'annotation ; annulable (clear) ;
 *  - drill-down : du Pareto agrégé (catégorie 6M) vers le détail des sous-causes ;
 *  - annotations collaboratives : panneau persistant par graphique ;
 *  - time-travel : « afficher l'état au … » via une requête backend as-of réelle.
 */
@Component({
  selector: 'qos-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class DashboardComponent implements OnInit, OnDestroy {

  /** Dimension partagée par le cross-filter sur cette page. */
  static readonly DIM_CATEGORY = 'category';

  /** Clés de graphiques pour les annotations (stables, alignées sur l'API). */
  readonly chartKeys = {
    trend: 'exec.trend',
    pareto: 'exec.pareto',
    heatmap: 'exec.heatmap'
  };

  kpis$!: Observable<KpiCard[]>;
  /** Horodatage réel de l'agrégat (remplace un « il y a 2 min » écrit en dur). */
  generatedAt$!: Observable<string | null>;
  /** La courbe de tendance a-t-elle au moins un point mesuré ? */
  trendHasData$!: Observable<boolean>;
  alignments$!: Observable<AlignmentBar[]>;
  topRisks$!: Observable<TopRisk[]>;
  predictions$!: Observable<AiPrediction[]>;

  trendOption$!: Observable<EChartsCoreOption>;
  paretoOption$!: Observable<EChartsCoreOption>;
  heatmapOption$!: Observable<EChartsCoreOption>;

  /** Cross-filter courant (null si aucun). */
  filter$!: Observable<CrossFilter | null>;

  /** Drill-down : sous-causes de la catégorie filtrée (niveau 2). */
  drilldown$!: Observable<DefectByCategory[] | null>;

  /** Time-travel : snapshot as-of (null tant que non demandé). */
  readonly snapshot$ = new BehaviorSubject<DashboardSnapshot | null>(null);
  readonly timeTravelLoading$ = new BehaviorSubject<boolean>(false);
  readonly timeTravelError$ = new BehaviorSubject<string | null>(null);
  /** Date sélectionnée pour le time-travel (format <input type=date>). */
  timeTravelDate = '';

  private defects: DefectByCategory[] = [];
  /** Libellé affiché d'une catégorie du Pareto → code de la catégorie (cross-filter). */
  private paretoCodes = new Map<string, string>();
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly svc: DashboardService,
    private readonly crossFilter: CrossFilterService,
    private readonly timeTravel: TimeTravelService,
    private readonly router: Router,
    private readonly theme: ThemeService
  ) {}

  // ---- Actions de l'en-tête (boutons auparavant inertes) --------------------

  /**
   * Exporte les KPIs affichés en CSV.
   *
   * Volontairement côté client : les valeurs sont déjà chargées, l'export doit rester
   * disponible même hors ligne (§15.2), et cela évite un aller-retour serveur pour
   * reformater ce que l'utilisateur a sous les yeux. Le séparateur `;` et le BOM UTF-8
   * garantissent l'ouverture directe dans Excel en locale française.
   */
  exportKpisCsv(): void {
    this.kpis$.pipe(take(1)).subscribe(kpis => {
      const header = [
        $localize`:@@dashboard.csv.indicator:Indicateur`,
        $localize`:@@dashboard.csv.value:Valeur`,
        $localize`:@@dashboard.csv.unit:Unité`,
        $localize`:@@dashboard.csv.target:Cible`,
        $localize`:@@dashboard.csv.trend:Variation`,
        $localize`:@@dashboard.csv.state:État`
      ];
      const rows = kpis.map(k => [
        k.label, String(k.value), k.unit ?? '', k.target ?? '', k.trend ?? '', k.state
      ]);
      const csv = [header, ...rows]
        .map(cells => cells.map(c => `"${String(c).replace(/"/g, '""')}"`).join(';'))
        .join('\r\n');

      const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `qualitos-kpis-${new Date().toISOString().slice(0, 10)}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    });
  }

  /**
   * Ouvre le générateur de récit narratif (§7.4), qui produit le rapport commenté à
   * partir de ces mêmes indicateurs.
   */
  newReport(): void {
    void this.router.navigate(['/storyboard']);
  }

  /** Ouvre la ressource à l'origine du risque (item FMEA ou dossier CAPA). */
  openRisk(risk: TopRisk): void {
    const route = risk.sourceType === 'CAPA' ? '/capa' : '/fmea';
    void this.router.navigate([route, risk.id]);
  }

  ngOnInit(): void {
    // Les données du dashboard viennent d'une agrégation serveur mise en cache par le
    // service ; sans ce rappel, un retour sur la page réafficherait l'instantané de la
    // visite précédente. L'appel précède les abonnements : une seule requête part.
    this.svc.refresh();

    this.kpis$        = this.svc.getExecutiveKpis().pipe(shareReplay(1));
    this.generatedAt$ = this.svc.getGeneratedAt().pipe(shareReplay(1));
    this.alignments$  = this.svc.getAlignmentBars().pipe(shareReplay(1));
    this.topRisks$    = this.svc.getTopRisks().pipe(shareReplay(1));
    this.predictions$ = this.svc.getAiPredictions().pipe(shareReplay(1));

    this.filter$ = this.crossFilter.filter$;

    const trend$   = this.svc.getQualityTrend().pipe(shareReplay(1));
    // catchError → []: défaut sûr partagé par les deux consommateurs (la
    // souscription ci-dessous ET le pareto). Sans lui, un échec remontait en
    // erreur non gérée et cassait le widget Pareto.
    const defects$ = this.svc.getDefectsByCategory().pipe(
      catchError(() => of([] as DefectByCategory[])),
      shareReplay(1)
    );
    const heat$    = this.svc.getComplianceHeatmap().pipe(shareReplay(1));

    defects$.pipe(takeUntil(this.destroy$)).subscribe(d => (this.defects = d));

    // Les options réagissent au filtre courant (surbrillance / atténuation) et au
    // thème : les couleurs propres à la page sont relues dans les tokens.
    const mode$ = this.theme.mode();
    this.trendHasData$ = trend$.pipe(map(p => p.length > 0));
    this.trendOption$ = combineLatest([trend$, mode$]).pipe(map(([p]) => this.toTrendOption(p)));

    this.paretoOption$ = combineLatest([defects$, this.crossFilter.filter$, mode$]).pipe(
      map(([d, f]) => this.toParetoOption(d, f))
    );

    this.heatmapOption$ = combineLatest([heat$, mode$]).pipe(map(([h]) => this.toHeatmapOption(h)));

    this.drilldown$ = this.crossFilter.filter$.pipe(
      map(f => (f ? this.svc.getDefectSubcategoriesSync(f.value) : null))
    );
  }

  ngOnDestroy(): void {
    // Réinitialise le filtre en quittant la page.
    this.crossFilter.clear();
    this.destroy$.next();
    this.destroy$.complete();
  }

  /* ============================================================
   * Cross-filtering & drill-down.
   * ============================================================ */

  /** Clic sur une barre/segment du Pareto → applique le cross-filter. */
  onParetoSelect(sel: EchartPointSelection): void {
    if (!sel.category) return;
    // L'axe porte le libellé traduit ; le filtre, lui, garde le code de la catégorie.
    this.crossFilter.apply({
      dimension: DashboardComponent.DIM_CATEGORY,
      value: this.paretoCodes.get(sel.category) ?? sel.category,
      label: sel.category
    });
  }

  /** Retire le filtre croisé. */
  clearFilter(): void {
    this.crossFilter.clear();
  }

  /** Anchor label transmis au panneau d'annotation Pareto (catégorie filtrée). */
  paretoAnchor(f: CrossFilter | null): string | null {
    return f && f.dimension === DashboardComponent.DIM_CATEGORY ? f.label : null;
  }

  /* ============================================================
   * Time-travel (état as-of réel via backend).
   * ============================================================ */

  applyTimeTravel(): void {
    if (!this.timeTravelDate) return;
    // <input type=date> → minuit UTC de la journée choisie.
    const iso = new Date(this.timeTravelDate + 'T00:00:00.000Z').toISOString();
    this.timeTravelLoading$.next(true);
    this.timeTravelError$.next(null);
    this.timeTravel.kpisAsOf(iso).pipe(
      catchError(() => {
        this.timeTravelError$.next(
          $localize`:@@dashboard.tt.error:Impossible de récupérer l'état à cette date.`);
        return of<DashboardSnapshot | null>(null);
      }),
      takeUntil(this.destroy$)
    ).subscribe(snap => {
      this.timeTravelLoading$.next(false);
      this.snapshot$.next(snap);
    });
  }

  clearTimeTravel(): void {
    this.timeTravelDate = '';
    this.snapshot$.next(null);
    this.timeTravelError$.next(null);
  }

  trackByKpi(_: number, k: { code: string }): string {
    return k.code;
  }

  /* ============================================================
   * Helpers d'état (existant).
   * ============================================================ */

  alignmentTone(score: number): 'success' | 'warn' | 'danger' {
    if (score >= 80) return 'success';
    if (score >= 60) return 'warn';
    return 'danger';
  }

  /** Nombre de normes réellement adoptées, lu dans l'agrégat (et non écrit en dur). */
  normsTrackedLabel(count: number): string {
    return $localize`:@@dashboard.exec.norms-tracked-count:Normes suivies : ${count}:count:`;
  }

  /** Libellé traduit d'une gravité (« Critique »), jamais le code en capitales. */
  severityText(s: TopRisk['severity']): string {
    return severityLabel(s.toUpperCase());
  }

  severityTone(s: TopRisk['severity']): 'danger' | 'warn' | 'neutral' {
    if (s === 'critical') return 'danger';
    if (s === 'high')     return 'warn';
    return 'neutral';
  }

  predictionIcon(kind: AiPrediction['kind']): string {
    switch (kind) {
      case 'drift':     return 'monitoring';
      case 'objective': return 'flag';
      case 'supplier':  return 'local_shipping';
      case 'complaint': return 'forum';
    }
  }

  /* ============================================================
   * Options ECharts.
   * ============================================================ */

  /** Valeur d'un token du design system, relue au moment de construire l'option. */
  private token(name: string): string {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  }

  /**
   * Bornes de l'axe des pourcentages : un pas rond (5, 10 ou 20) et un minimum
   * multiple de ce pas. Un minimum fixé à 85 donnait des graduations
   * irrégulières (100, 99, 96, 93…).
   */
  static percentAxis(values: number[]): { min: number; max: number; interval: number } {
    const lowest = values.length ? Math.max(0, Math.min(...values, 100)) : 0;
    const span = 100 - lowest;
    const interval = span <= 20 ? 5 : span <= 50 ? 10 : 20;
    const min = Math.max(0, Math.floor((lowest - 1) / interval) * interval);
    return { min: Math.min(min, 100 - interval), max: 100, interval };
  }

  private toTrendOption(points: QualityTrendPoint[]): EChartsCoreOption {
    const axis = DashboardComponent.percentAxis(points.flatMap(p => [p.value, p.target]));
    return {
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0, icon: 'circle', textStyle: { fontSize: 11 } },
      grid: { top: 16, bottom: 48, left: 8, right: 16, containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: points.map(p => p.month) },
      yAxis: { type: 'value', ...axis, axisLabel: { formatter: '{value} %' } },
      series: [
        {
          name: $localize`:@@dashboard.chart.overall-quality:Qualité globale`,
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 2.5 },
          // Sans couleur imposée, l'aire reprend celle de la série (token du thème).
          areaStyle: { opacity: 0.12 },
          data: points.map(p => p.value)
        },
        {
          name: $localize`:@@dashboard.chart.target:Cible`,
          type: 'line',
          smooth: true,
          symbol: 'none',
          lineStyle: { type: 'dashed', width: 1.5 },
          data: points.map(p => p.target)
        }
      ]
    };
  }

  private toParetoOption(defects: DefectByCategory[], filter: CrossFilter | null): EChartsCoreOption {
    const labels = defects.map(d => ncCategoryLabel(d.category));
    this.paretoCodes = new Map(labels.map((label, i) => [label, defects[i].category]));
    const total = defects.reduce((s, d) => s + d.count, 0) || 1;
    let acc = 0;
    const cumulative = defects.map(d => {
      acc += d.count;
      return Math.round((acc / total) * 100);
    });
    // Cross-filtering : la barre sélectionnée reste pleine, les autres atténuées.
    const barData = defects.map(d => {
      const dimmed = filter
        && filter.dimension === DashboardComponent.DIM_CATEGORY
        && filter.value !== d.category;
      return {
        value: d.count,
        itemStyle: { opacity: dimmed ? 0.3 : 1 }
      };
    });
    const defectsName = $localize`:@@dashboard.chart.defects:Défauts`;
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      // Légende sous le graphique : en haut à droite, elle chevauchait le titre
      // de l'axe « Cumul % ».
      legend: { bottom: 0, icon: 'circle', textStyle: { fontSize: 11 } },
      grid: { top: 40, bottom: 48, left: 8, right: 8, containLabel: true },
      // `interval: 0` : chaque catégorie garde son libellé (2 sur 5 disparaissaient),
      // tronqué au besoin plutôt que masqué.
      xAxis: {
        type: 'category', data: labels,
        axisLabel: { interval: 0, width: 72, overflow: 'truncate', hideOverlap: false }
      },
      yAxis: [
        { type: 'value', name: defectsName, position: 'left', minInterval: 1 },
        {
          type: 'value', name: $localize`:@@dashboard.chart.cumulative-pct:Cumul %`, position: 'right',
          min: 0, max: 100, interval: 20, axisLabel: { formatter: '{value} %' }
        }
      ],
      series: [
        {
          name: defectsName, type: 'bar',
          data: barData,
          itemStyle: { borderRadius: [6, 6, 0, 0] },
          barWidth: 28,
          cursor: 'pointer'
        },
        {
          name: $localize`:@@dashboard.chart.cumulative:Cumul`, type: 'line', yAxisIndex: 1,
          smooth: true, symbol: 'circle', symbolSize: 6,
          lineStyle: { width: 2 },
          data: cumulative,
          markLine: {
            silent: true,
            symbol: 'none',
            lineStyle: { type: 'dashed', color: this.token('--qos-chart-3'), opacity: 0.7 },
            // Libellé posé au début de la ligne, dans le graphique : à son bout, il
            // recouvrait la graduation « 80 % » de l'axe de droite.
            data: [{ yAxis: 80, label: { formatter: '80 %', position: 'insideStartTop' } }]
          }
        }
      ]
    };
  }

  private toHeatmapOption(cells: ComplianceHeatCell[]): EChartsCoreOption {
    const norms   = Array.from(new Set(cells.map(c => c.norm)));
    const clauses = Array.from(new Set(cells.map(c => c.clause)));
    const data = cells.map(c => [
      clauses.indexOf(c.clause),
      norms.indexOf(c.norm),
      c.score
    ]);
    return {
      tooltip: {
        position: 'top',
        formatter: (p: { value: number[] }) =>
          `<strong>${norms[p.value[1]]}</strong><br/>${clauses[p.value[0]]} · ${p.value[2]}%`
      },
      grid: { top: 24, bottom: 8, left: 80, right: 16, containLabel: true },
      xAxis: {
        type: 'category', data: clauses, splitArea: { show: true },
        axisLine: { show: false }, axisTick: { show: false }
      },
      yAxis: {
        type: 'category', data: norms, splitArea: { show: true },
        axisLine: { show: false }, axisTick: { show: false }
      },
      visualMap: {
        min: 0, max: 100, calculable: false, orient: 'horizontal',
        left: 'center', bottom: 0, itemHeight: 100, itemWidth: 10,
        inRange: { color: [this.token('--qos-chart-4'), this.token('--qos-chart-3'), this.token('--qos-chart-2')] },
        textStyle: { fontSize: 10 }
      },
      series: [{
        type: 'heatmap',
        data,
        label: { show: true, formatter: (p: { value: number[] }) => p.value[2] + '%', fontSize: 10 },
        itemStyle: { borderRadius: 4, borderWidth: 1, borderColor: 'transparent' },
        emphasis: { itemStyle: { borderColor: this.token('--qos-border-strong') } }
      }]
    };
  }
}
