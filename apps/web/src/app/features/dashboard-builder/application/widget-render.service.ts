/**
 * Construit les options ECharts d'un widget à partir de son type et de sa
 * config. Le rendu réel passe par le composant partagé <qos-echart> (§7.2).
 *
 * Les données affichées dans le builder sont des aperçus déterministes dérivés
 * de l'id du KPI : le builder sert à composer la mise en page ; le câblage aux
 * flux réels (KPI engine / time-series) se fait au rendu du dashboard publié.
 */
import { Injectable } from '@angular/core';
import type { EChartsCoreOption } from 'echarts/core';

import { Widget } from '../domain/dashboard.model';

@Injectable()
export class WidgetRenderService {

  /** Catégories d'axe réutilisées par les aperçus. */
  private readonly months = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun'];
  private readonly categories = ['Méthode', 'Machine', 'Main d\'œuvre', 'Matière', 'Milieu', 'Mesure'];

  /**
   * Renvoie l'option ECharts pour un widget de type graphique.
   * Retourne null pour kpi / narrative (rendus sans ECharts).
   */
  optionFor(widget: Widget): EChartsCoreOption | null {
    switch (widget.type) {
      case 'line':
        return this.lineOption(widget);
      case 'bar':
      case 'table':
        return this.barOption(widget);
      case 'pie':
        return this.pieOption(widget);
      case 'gauge':
        return this.gaugeOption(widget);
      case 'control-chart':
        return this.controlChartOption(widget);
      case 'heatmap':
        return this.heatmapOption(widget);
      default:
        return null;
    }
  }

  /** Valeur KPI d'aperçu, déterministe par id (stable entre rendus). */
  kpiValue(widget: Widget): number {
    return this.seedFrom(widget.config.kpiId ?? widget.id, 12, 88);
  }

  /** Tendance d'aperçu en %, déterministe (négatif = amélioration). */
  kpiTrend(widget: Widget): number {
    const raw = this.seedFrom((widget.config.kpiId ?? widget.id) + ':trend', 0, 20);
    return raw - 10;
  }

  private series(widget: Widget, count: number, min: number, max: number): number[] {
    const seed = widget.config.kpiId ?? widget.id;
    return Array.from({ length: count }, (_, i) => this.seedFrom(`${seed}:${i}`, min, max));
  }

  private lineOption(widget: Widget): EChartsCoreOption {
    return {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', boundaryGap: false, data: this.months },
      yAxis: { type: 'value' },
      series: [{
        type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2.5 },
        areaStyle: { opacity: 0.12 },
        data: this.series(widget, this.months.length, 60, 98)
      }]
    };
  }

  private barOption(widget: Widget): EChartsCoreOption {
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      xAxis: { type: 'category', data: this.categories },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar', barWidth: 22,
        itemStyle: { borderRadius: [6, 6, 0, 0] },
        data: this.series(widget, this.categories.length, 4, 40)
      }]
    };
  }

  private pieOption(widget: Widget): EChartsCoreOption {
    const values = this.series(widget, this.categories.length, 4, 40);
    return {
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, icon: 'circle', textStyle: { fontSize: 10 } },
      series: [{
        type: 'pie', radius: ['42%', '70%'], avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: this.categories.map((c, i) => ({ name: c, value: values[i] }))
      }]
    };
  }

  private gaugeOption(widget: Widget): EChartsCoreOption {
    const value = this.kpiValue(widget);
    return {
      series: [{
        type: 'gauge', startAngle: 210, endAngle: -30, min: 0, max: 100,
        progress: { show: true, width: 14 },
        axisLine: { lineStyle: { width: 14 } },
        axisLabel: { distance: 18, fontSize: 10 },
        pointer: { width: 5 },
        detail: { valueAnimation: true, fontSize: 26, offsetCenter: [0, '64%'], formatter: '{value}%' },
        data: [{ value }]
      }]
    };
  }

  private controlChartOption(widget: Widget): EChartsCoreOption {
    const data = this.series(widget, 12, 40, 60);
    const mean = data.reduce((s, v) => s + v, 0) / data.length;
    const sd = Math.sqrt(data.reduce((s, v) => s + (v - mean) ** 2, 0) / data.length) || 1;
    const ucl = Math.round(mean + 3 * sd);
    const lcl = Math.round(mean - 3 * sd);
    return {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: data.map((_, i) => `${i + 1}`) },
      yAxis: { type: 'value' },
      series: [{
        type: 'line', symbol: 'circle', symbolSize: 6, data,
        markLine: {
          silent: true, symbol: 'none',
          lineStyle: { type: 'dashed' },
          data: [
            { yAxis: ucl, label: { formatter: `UCL ${ucl}` } },
            { yAxis: Math.round(mean), label: { formatter: `x̄ ${Math.round(mean)}` } },
            { yAxis: lcl, label: { formatter: `LCL ${lcl}` } }
          ]
        }
      }]
    };
  }

  private heatmapOption(widget: Widget): EChartsCoreOption {
    const rows = ['ISO 9001', 'ISO 14001', 'ISO 27001'];
    const cols = ['§4', '§5', '§6', '§7', '§8', '§9'];
    const data: number[][] = [];
    rows.forEach((_, r) =>
      cols.forEach((__, c) =>
        data.push([c, r, this.seedFrom(`${widget.id}:${r}:${c}`, 40, 100)])));
    return {
      tooltip: { position: 'top' },
      grid: { top: 16, bottom: 32, left: 70, right: 12, containLabel: true },
      xAxis: { type: 'category', data: cols, splitArea: { show: true } },
      yAxis: { type: 'category', data: rows, splitArea: { show: true } },
      visualMap: {
        min: 0, max: 100, calculable: false, orient: 'horizontal',
        left: 'center', bottom: 0, itemHeight: 80,
        inRange: { color: ['#fee2e2', '#fde68a', '#bbf7d0', '#10b981'] },
        textStyle: { fontSize: 9 }
      },
      series: [{
        type: 'heatmap', data,
        label: { show: true, fontSize: 9, formatter: (p: { value: number[] }) => p.value[2] + '%' },
        itemStyle: { borderRadius: 3, borderWidth: 1, borderColor: 'transparent' }
      }]
    };
  }

  /**
   * Génère un entier déterministe dans [min, max] à partir d'une graine texte
   * (hash FNV-1a simplifié). Évite Math.random : les aperçus restent stables.
   */
  private seedFrom(seed: string, min: number, max: number): number {
    let h = 2166136261;
    for (let i = 0; i < seed.length; i++) {
      h ^= seed.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    const norm = (h >>> 0) / 0xffffffff;
    return Math.round(min + norm * (max - min));
  }
}
