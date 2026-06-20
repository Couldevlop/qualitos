import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { BehaviorSubject, Observable, Subject, combineLatest, of } from 'rxjs';
import { catchError, map, shareReplay, takeUntil } from 'rxjs/operators';
import type { EChartsCoreOption } from 'echarts/core';

import { EchartPointSelection } from '../../shared/ui/echart/echart.component';
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

  readonly periods = ['7 jours', '30 jours', '90 jours', '12 mois'];
  selectedPeriod = '30 jours';

  private defects: DefectByCategory[] = [];
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly svc: DashboardService,
    private readonly crossFilter: CrossFilterService,
    private readonly timeTravel: TimeTravelService
  ) {}

  ngOnInit(): void {
    this.kpis$        = this.svc.getExecutiveKpis().pipe(shareReplay(1));
    this.alignments$  = this.svc.getAlignmentBars().pipe(shareReplay(1));
    this.topRisks$    = this.svc.getTopRisks().pipe(shareReplay(1));
    this.predictions$ = this.svc.getAiPredictions().pipe(shareReplay(1));

    this.filter$ = this.crossFilter.filter$;

    const trend$   = this.svc.getQualityTrend().pipe(shareReplay(1));
    const defects$ = this.svc.getDefectsByCategory().pipe(shareReplay(1));
    const heat$    = this.svc.getComplianceHeatmap().pipe(shareReplay(1));

    defects$.pipe(takeUntil(this.destroy$)).subscribe(d => (this.defects = d));

    // Les options réagissent au filtre courant (surbrillance / atténuation).
    this.trendOption$ = trend$.pipe(map(p => this.toTrendOption(p)));

    this.paretoOption$ = combineLatest([defects$, this.crossFilter.filter$]).pipe(
      map(([d, f]) => this.toParetoOption(d, f))
    );

    this.heatmapOption$ = heat$.pipe(map(h => this.toHeatmapOption(h)));

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
    this.crossFilter.apply({
      dimension: DashboardComponent.DIM_CATEGORY,
      value: sel.category,
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

  private toTrendOption(points: QualityTrendPoint[]): EChartsCoreOption {
    return {
      tooltip: { trigger: 'axis' },
      legend: { right: 0, top: 0, icon: 'circle', textStyle: { fontSize: 11 } },
      xAxis: { type: 'category', boundaryGap: false, data: points.map(p => p.month) },
      yAxis: { type: 'value', min: 85, max: 100, axisLabel: { formatter: '{value} %' } },
      series: [
        {
          name: 'Qualité globale',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          lineStyle: { width: 2.5 },
          areaStyle: {
            opacity: 0.12,
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(37, 99, 235, 0.36)' },
                { offset: 1, color: 'rgba(37, 99, 235, 0)' }
              ]
            }
          },
          data: points.map(p => p.value)
        },
        {
          name: 'Cible',
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
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { right: 0, top: 0, icon: 'circle', textStyle: { fontSize: 11 } },
      xAxis: { type: 'category', data: defects.map(d => d.category) },
      yAxis: [
        { type: 'value', name: 'Défauts', position: 'left' },
        {
          type: 'value', name: 'Cumul %', position: 'right',
          min: 0, max: 100, axisLabel: { formatter: '{value} %' }
        }
      ],
      series: [
        {
          name: 'Défauts', type: 'bar',
          data: barData,
          itemStyle: { borderRadius: [6, 6, 0, 0] },
          barWidth: 28,
          cursor: 'pointer'
        },
        {
          name: 'Cumul', type: 'line', yAxisIndex: 1,
          smooth: true, symbol: 'circle', symbolSize: 6,
          lineStyle: { width: 2 },
          data: cumulative,
          markLine: {
            silent: true,
            symbol: 'none',
            lineStyle: { type: 'dashed', color: 'rgba(245, 158, 11, 0.6)' },
            data: [{ yAxis: 80, label: { formatter: '80 %' } }]
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
        inRange: { color: ['#fee2e2', '#fde68a', '#bbf7d0', '#10b981'] },
        textStyle: { fontSize: 10 }
      },
      series: [{
        type: 'heatmap',
        data,
        label: { show: true, formatter: (p: { value: number[] }) => p.value[2] + '%', fontSize: 10 },
        itemStyle: { borderRadius: 4, borderWidth: 1, borderColor: 'transparent' },
        emphasis: { itemStyle: { borderColor: 'rgba(0,0,0,0.2)' } }
      }]
    };
  }
}
