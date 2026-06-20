import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, EventEmitter,
  Input, NgZone, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild
} from '@angular/core';
import type { EChartsCoreOption, ECharts } from 'echarts/core';
import { Subscription } from 'rxjs';

import { ThemeMode, ThemeService } from '../../../core/theme/theme.service';

type EchartsCore = typeof import('echarts/core');

let echartsCore: Promise<EchartsCore> | null = null;

/**
 * Charge ECharts à la demande (import dynamique) : la lib (~340 kB min) sort
 * du bundle initial — UiModule est importé par le shell EAGER alors que seuls
 * dashboard et SPC affichent des graphiques. Le chunk est partagé et mis en
 * cache au premier rendu d'un chart. Singleton : `use()` n'est appelé qu'une fois.
 */
function loadEcharts(): Promise<EchartsCore> {
  if (!echartsCore) {
    echartsCore = Promise.all([
      import('echarts/core'),
      import('echarts/charts'),
      import('echarts/components'),
      import('echarts/renderers')
    ]).then(([core, charts, components, renderers]) => {
      core.use([
        charts.LineChart, charts.BarChart, charts.PieChart,
        charts.HeatmapChart, charts.ScatterChart,
        components.GridComponent, components.TooltipComponent,
        components.LegendComponent, components.TitleComponent,
        components.DataZoomComponent, components.VisualMapComponent,
        components.MarkLineComponent,
        renderers.CanvasRenderer
      ]);
      return core;
    });
  }
  return echartsCore;
}

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

  /**
   * Émet la catégorie (libellé de l'axe / nom de série) du point cliqué.
   * Utilisé par le cross-filtering des dashboards (§7.3) : un clic propage un
   * filtre partagé à tous les widgets de la page.
   */
  @Output() pointSelected = new EventEmitter<EchartPointSelection>();

  @ViewChild('host', { static: true }) host!: ElementRef<HTMLDivElement>;

  private chart: ECharts | null = null;
  private observer?: ResizeObserver;
  private themeSub?: Subscription;
  private currentMode: ThemeMode = 'light';
  private destroyed = false;

  constructor(private readonly zone: NgZone, private readonly themeSvc: ThemeService) {}

  ngAfterViewInit(): void {
    void loadEcharts().then(core => {
      if (this.destroyed) return;   // composant détruit pendant le chargement
      this.zone.runOutsideAngular(() => {
        this.chart = core.init(this.host.nativeElement);
        this.attachResize();
        this.attachClick();
        this.themeSub = this.themeSvc.mode().subscribe(mode => {
          this.currentMode = mode;
          this.render();
        });
      });
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['option'] || changes['loading']) this.render();
  }

  ngOnDestroy(): void {
    this.destroyed = true;
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

  /**
   * Relaie les clics sur un point de données vers {@link pointSelected}, en
   * réentrant dans la zone Angular (le chart est initialisé hors-zone pour la
   * perf). Le payload est volontairement minimal et typé.
   */
  private attachClick(): void {
    if (!this.chart) return;
    this.chart.on('click', (params: EchartsClickParams) => {
      const category = typeof params.name === 'string' && params.name.length > 0
        ? params.name
        : (params.seriesName ?? '');
      if (!category) return;
      this.zone.run(() => this.pointSelected.emit({
        category,
        seriesName: params.seriesName,
        value: params.value
      }));
    });
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

/** Sélection émise au clic sur un point/segment/barre d'un graphique. */
export interface EchartPointSelection {
  /** Catégorie de l'axe (ou nom de la série si pas de catégorie). */
  category: string;
  seriesName?: string;
  value?: unknown;
}

/** Sous-ensemble typé des params de clic ECharts qui nous intéressent. */
interface EchartsClickParams {
  name?: string;
  seriesName?: string;
  value?: unknown;
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
