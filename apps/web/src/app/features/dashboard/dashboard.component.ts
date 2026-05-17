import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';
import type { EChartsCoreOption } from 'echarts/core';

import { DashboardService } from './dashboard.service';
import {
  AiPrediction, AlignmentBar, ComplianceHeatCell, DefectByCategory,
  KpiCard, QualityTrendPoint, TopRisk
} from './dashboard.types';

/**
 * Dashboard exécutif — vue 360° qualité (Pilotage Direction).
 * Conforme à CLAUDE.md §7.1 — Niveau 1 Executive Dashboard.
 */
@Component({
  selector: 'qos-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class DashboardComponent implements OnInit {

  kpis$!: Observable<KpiCard[]>;
  alignments$!: Observable<AlignmentBar[]>;
  topRisks$!: Observable<TopRisk[]>;
  predictions$!: Observable<AiPrediction[]>;

  trendOption$!: Observable<EChartsCoreOption>;
  paretoOption$!: Observable<EChartsCoreOption>;
  heatmapOption$!: Observable<EChartsCoreOption>;

  readonly periods = ['7 jours', '30 jours', '90 jours', '12 mois'];
  selectedPeriod = '30 jours';

  constructor(private readonly svc: DashboardService) {}

  ngOnInit(): void {
    this.kpis$        = this.svc.getExecutiveKpis().pipe(shareReplay(1));
    this.alignments$  = this.svc.getAlignmentBars().pipe(shareReplay(1));
    this.topRisks$    = this.svc.getTopRisks().pipe(shareReplay(1));
    this.predictions$ = this.svc.getAiPredictions().pipe(shareReplay(1));

    this.trendOption$  = this.svc.getQualityTrend().pipe(map(p => this.toTrendOption(p)));
    this.paretoOption$ = this.svc.getDefectsByCategory().pipe(map(d => this.toParetoOption(d)));
    this.heatmapOption$ = this.svc.getComplianceHeatmap().pipe(map(h => this.toHeatmapOption(h)));
  }

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

  private toParetoOption(defects: DefectByCategory[]): EChartsCoreOption {
    const total = defects.reduce((s, d) => s + d.count, 0);
    let acc = 0;
    const cumulative = defects.map(d => {
      acc += d.count;
      return Math.round((acc / total) * 100);
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
          data: defects.map(d => d.count),
          itemStyle: { borderRadius: [6, 6, 0, 0] },
          barWidth: 28
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
