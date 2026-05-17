import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef,
  Input, NgZone, OnChanges, OnDestroy, SimpleChanges, ViewChild
} from '@angular/core';
import * as echarts from 'echarts/core';
import {
  LineChart, BarChart, PieChart, HeatmapChart, ScatterChart
} from 'echarts/charts';
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, DataZoomComponent, VisualMapComponent,
  MarkLineComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import type { EChartsCoreOption, ECharts } from 'echarts/core';
import { Subscription } from 'rxjs';

import { ThemeMode, ThemeService } from '../../../core/theme/theme.service';

echarts.use([
  LineChart, BarChart, PieChart, HeatmapChart, ScatterChart,
  GridComponent, TooltipComponent, LegendComponent, TitleComponent,
  DataZoomComponent, VisualMapComponent, MarkLineComponent,
  CanvasRenderer
]);

/**
 * Wrapper léger d'ECharts.
 *  - `[option]` : option ECharts utilisateur. Le composant la merge avec
 *    des defaults inspirés des tokens (couleurs des séries, grille, axes)
 *    qui réagissent au thème courant.
 *  - Resize automatique via ResizeObserver.
 *  - Hauteur paramétrable.
 */
@Component({
  selector: 'qos-echart',
  templateUrl: './echart.component.html',
  styleUrls: ['./echart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class EchartComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() option: EChartsCoreOption | null = null;
  @Input() height = 280;
  @Input() loading = false;
  @Input() autoResize = true;

  @ViewChild('host', { static: true }) host!: ElementRef<HTMLDivElement>;

  private chart: ECharts | null = null;
  private observer?: ResizeObserver;
  private themeSub?: Subscription;
  private currentMode: ThemeMode = 'light';

  constructor(private readonly zone: NgZone, private readonly themeSvc: ThemeService) {}

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      this.chart = echarts.init(this.host.nativeElement);
      this.attachResize();
      this.themeSub = this.themeSvc.mode().subscribe(mode => {
        this.currentMode = mode;
        this.render();
      });
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['option'] || changes['loading']) this.render();
  }

  ngOnDestroy(): void {
    this.themeSub?.unsubscribe();
    this.observer?.disconnect();
    this.chart?.dispose();
    this.chart = null;
  }

  private attachResize(): void {
    if (!this.autoResize || !this.chart) return;
    this.observer = new ResizeObserver(() => this.chart?.resize());
    this.observer.observe(this.host.nativeElement);
  }

  private render(): void {
    if (!this.chart) return;
    if (this.loading) { this.chart.showLoading('default', this.loadingOptions()); return; }
    this.chart.hideLoading();
    const merged = this.option ? this.mergeDefaults(this.option) : {};
    this.chart.setOption(merged, { notMerge: true });
  }

  private mergeDefaults(opt: EChartsCoreOption): EChartsCoreOption {
    const tokens = this.readTokens();
    return {
      backgroundColor: 'transparent',
      animationDuration: 320,
      animationEasing: 'cubicOut',
      color: tokens.series,
      textStyle: {
        fontFamily: tokens.fontFamily,
        color: tokens.fgPrimary
      },
      tooltip: {
        trigger: 'axis',
        backgroundColor: tokens.bgElevated,
        borderColor: tokens.borderSubtle,
        borderWidth: 1,
        padding: 10,
        textStyle: { color: tokens.fgPrimary, fontSize: 12 },
        extraCssText: 'box-shadow: 0 8px 24px -8px rgba(0,0,0,0.20); border-radius: 8px;',
        ...((opt as Record<string, unknown>)['tooltip'] ?? {})
      },
      grid: { left: 16, right: 16, top: 28, bottom: 24, containLabel: true,
              ...((opt as Record<string, unknown>)['grid'] ?? {}) },
      xAxis: this.axisDefaults((opt as Record<string, unknown>)['xAxis'], tokens),
      yAxis: this.axisDefaults((opt as Record<string, unknown>)['yAxis'], tokens),
      ...opt
    } as EChartsCoreOption;
  }

  private axisDefaults(axis: unknown, t: ChartTokens): unknown {
    if (axis == null) return undefined;
    const base = {
      axisLine: { lineStyle: { color: t.chartAxis } },
      axisTick: { show: false },
      axisLabel: { color: t.chartAxis, fontSize: 11 },
      splitLine: { lineStyle: { color: t.chartGrid, type: 'dashed' as const } }
    };
    if (Array.isArray(axis)) return axis.map(a => ({ ...base, ...(a as object) }));
    return { ...base, ...(axis as object) };
  }

  private loadingOptions(): Record<string, unknown> {
    const t = this.readTokens();
    return {
      text: '', color: t.series[0], textColor: t.fgSecondary,
      maskColor: 'transparent', zlevel: 0
    };
  }

  /** Lit les CSS custom properties à chaud — réagit au switch de thème. */
  private readTokens(): ChartTokens {
    const root = getComputedStyle(document.documentElement);
    const read = (name: string, fallback: string) =>
      (root.getPropertyValue(name).trim() || fallback);
    return {
      fontFamily: read('--qos-font-sans', 'Inter, sans-serif'),
      fgPrimary:  read('--qos-fg-primary', '#0F172A'),
      fgSecondary: read('--qos-fg-secondary', '#475569'),
      bgElevated: read('--qos-bg-elevated', '#FFFFFF'),
      borderSubtle: read('--qos-border-subtle', '#E2E8F0'),
      chartGrid:  read('--qos-chart-grid', '#E2E8F0'),
      chartAxis:  read('--qos-chart-axis', '#64748B'),
      series: [
        read('--qos-chart-1', '#2563EB'),
        read('--qos-chart-2', '#059669'),
        read('--qos-chart-3', '#D97706'),
        read('--qos-chart-4', '#DC2626'),
        read('--qos-chart-5', '#8B5CF6'),
        read('--qos-chart-6', '#06B6D4')
      ]
    };
  }
}

interface ChartTokens {
  fontFamily: string;
  fgPrimary: string;
  fgSecondary: string;
  bgElevated: string;
  borderSubtle: string;
  chartGrid: string;
  chartAxis: string;
  series: string[];
}
