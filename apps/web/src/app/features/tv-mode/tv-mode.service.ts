import { Injectable } from '@angular/core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import type { EChartsCoreOption } from 'echarts/core';

import { DashboardService } from '../dashboard/dashboard.service';
import {
  AiPrediction, KpiCard, QualityTrendPoint, TopRisk
} from '../dashboard/dashboard.types';
import { TvSlide } from './tv-mode.types';

/**
 * Libellés localisés des slides, injectés depuis le composant.
 * On évite tout `$localize` dans le service pour qu'il reste testable sans
 * runtime i18n et réutilisable hors contexte de template.
 */
export interface TvSlideLabels {
  kpisTitle: string;
  kpisSubtitle: string;
  risksTitle: string;
  risksSubtitle: string;
  predictionsTitle: string;
  predictionsSubtitle: string;
  trendTitle: string;
  trendSubtitle: string;
  emptyTitle: string;
  emptySubtitle: string;
  trendSeriesName: string;
  trendTargetName: string;
}

/**
 * Service d'orchestration du Mode TV (§7.3).
 *
 * Il NE refait AUCUN appel HTTP : il réutilise {@link DashboardService}
 * (source unique des KPIs exécutifs, déjà filtrés par `tenant_id` côté
 * serveur via le JWT — OWASP A01) et transforme les flux en une liste de
 * « slides » prêtes à projeter. Si une source ne renvoie rien, la slide
 * correspondante est omise ; si TOUTES sont vides, une slide « aucune
 * donnée » est produite pour dégrader proprement.
 */
@Injectable()
export class TvModeService {

  constructor(private readonly dashboard: DashboardService) {}

  /** Construit le flux de slides à partir des données du dashboard exécutif. */
  buildSlides(labels: TvSlideLabels): Observable<TvSlide[]> {
    return combineLatest([
      this.dashboard.getExecutiveKpis(),
      this.dashboard.getTopRisks(),
      this.dashboard.getAiPredictions(),
      this.dashboard.getQualityTrend()
    ]).pipe(
      map(([kpis, risks, predictions, trend]) =>
        this.assemble(labels, kpis, risks, predictions, trend))
    );
  }

  /**
   * Assemble les slides (méthode pure, testable directement) : ignore les
   * sources vides et garantit au moins une slide de repli.
   */
  assemble(
    labels: TvSlideLabels,
    kpis: KpiCard[],
    risks: TopRisk[],
    predictions: AiPrediction[],
    trend: QualityTrendPoint[]
  ): TvSlide[] {
    const slides: TvSlide[] = [];

    if (kpis.length > 0) {
      slides.push({
        id: 'kpis', kind: 'kpis',
        title: labels.kpisTitle, subtitle: labels.kpisSubtitle,
        // On limite à 8 KPIs pour rester lisible à distance (anti-pattern §6.1).
        kpis: kpis.slice(0, 8)
      });
    }

    if (trend.length > 0) {
      slides.push({
        id: 'trend', kind: 'trend',
        title: labels.trendTitle, subtitle: labels.trendSubtitle,
        option: this.toTrendOption(trend, labels)
      });
    }

    if (risks.length > 0) {
      slides.push({
        id: 'risks', kind: 'risks',
        title: labels.risksTitle, subtitle: labels.risksSubtitle,
        risks: risks.slice(0, 6)
      });
    }

    if (predictions.length > 0) {
      slides.push({
        id: 'predictions', kind: 'predictions',
        title: labels.predictionsTitle, subtitle: labels.predictionsSubtitle,
        predictions: predictions.slice(0, 6)
      });
    }

    if (slides.length === 0) {
      slides.push({
        id: 'empty', kind: 'empty',
        title: labels.emptyTitle, subtitle: labels.emptySubtitle
      });
    }

    return slides;
  }

  /** Option ECharts de la tendance qualité — gros traits pour lisibilité TV. */
  private toTrendOption(points: QualityTrendPoint[], labels: TvSlideLabels): EChartsCoreOption {
    return {
      tooltip: { trigger: 'axis' },
      legend: {
        right: 0, top: 0, icon: 'circle',
        textStyle: { fontSize: 16 }
      },
      xAxis: {
        type: 'category', boundaryGap: false,
        data: points.map(p => p.month),
        axisLabel: { fontSize: 16 }
      },
      yAxis: {
        type: 'value', min: 85, max: 100,
        axisLabel: { formatter: '{value} %', fontSize: 16 }
      },
      series: [
        {
          name: labels.trendSeriesName,
          type: 'line', smooth: true,
          symbol: 'circle', symbolSize: 10,
          lineStyle: { width: 4 },
          areaStyle: {
            opacity: 0.16,
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(37, 99, 235, 0.40)' },
                { offset: 1, color: 'rgba(37, 99, 235, 0)' }
              ]
            }
          },
          data: points.map(p => p.value)
        },
        {
          name: labels.trendTargetName,
          type: 'line', smooth: true, symbol: 'none',
          lineStyle: { type: 'dashed', width: 2.5 },
          data: points.map(p => p.target)
        }
      ]
    };
  }
}
