/**
 * Widget host — rend un Widget selon son type.
 * Les graphiques utilisent le composant partagé <qos-echart> (ngx-echarts /
 * echarts déjà installés). Les widgets kpi / narrative ont un rendu dédié.
 */
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import type { EChartsCoreOption } from 'echarts/core';
import { Subject, takeUntil } from 'rxjs';

import { DashboardBuilderService } from '../../application/dashboard-builder.service';
import { WidgetRenderService } from '../../application/widget-render.service';
import { Widget } from '../../domain/dashboard.model';
import { EchartPointSelection } from '../../../../shared/ui/echart/echart.component';

@Component({
  selector: 'qos-widget-host',
  templateUrl: './widget-host.component.html',
  styleUrls: ['./widget-host.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class WidgetHostComponent implements OnInit, OnChanges, OnDestroy {
  @Input() widget!: Widget;
  /** Mode édition : affiche les poignées et désactive l'interaction graphique. */
  @Input() editing = false;

  /** Émet quand un point est cliqué (cross-filtering §7.3). */
  @Output() pointSelected = new EventEmitter<EchartPointSelection>();

  echartsOptions: EChartsCoreOption | null = null;
  kpiValue: number | string = '—';
  kpiTrend: number | null = null;
  kpiBreached = false;
  narrative = '';

  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly svc: DashboardBuilderService,
    private readonly render: WidgetRenderService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.computeView();
    this.svc.onFilter()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => { this.computeView(); this.cdr.markForCheck(); });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['widget']) {
      this.computeView();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** True pour les types rendus via ECharts (vs kpi/narrative). */
  get isChart(): boolean {
    return this.widget.type !== 'kpi' && this.widget.type !== 'narrative';
  }

  onPoint(sel: EchartPointSelection): void {
    if (this.editing) return;
    this.pointSelected.emit(sel);
  }

  private computeView(): void {
    switch (this.widget.type) {
      case 'kpi': {
        const value = this.render.kpiValue(this.widget);
        const unit = this.widget.config.unit ?? '';
        this.kpiValue = unit ? `${value}${unit === '%' || unit === '/5' ? '' : ' '}${unit}` : value;
        this.kpiTrend = this.render.kpiTrend(this.widget);
        const threshold = this.widget.config.threshold;
        this.kpiBreached = typeof threshold === 'number' && value > threshold;
        this.echartsOptions = null;
        break;
      }
      case 'narrative':
        this.narrative = (this.widget.config.text && this.widget.config.text.trim().length > 0)
          ? this.widget.config.text
          : $localize`:@@dbb.narrative.empty:Récit IA : aucun élément pour l'instant — branchez une source pour générer un storyboard.`;
        this.echartsOptions = null;
        break;
      default:
        this.echartsOptions = this.render.optionFor(this.widget);
    }
  }

  trendClass(): string {
    if (this.kpiTrend == null) return '';
    return this.kpiTrend > 0 ? 'trend trend-up' : 'trend trend-down';
  }

  trendLabel(): string {
    if (this.kpiTrend == null) return '';
    const arrow = this.kpiTrend > 0 ? '▲' : '▼';
    return `${arrow} ${Math.abs(this.kpiTrend)}%`;
  }
}
