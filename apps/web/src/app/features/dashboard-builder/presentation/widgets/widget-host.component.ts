/**
 * Widget host — renders a Widget based on its type.
 * Each chart is an ECharts placeholder so the dashboard remains usable
 * without ngx-echarts installed; once `npm install ngx-echarts echarts`
 * runs, replace the placeholder with a real <div echarts ...> binding.
 */
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';

import { DashboardBuilderService } from '../../application/dashboard-builder.service';
import { Widget } from '../../domain/dashboard.model';

type EChartOption = Record<string, unknown>;

@Component({
  selector: 'qos-widget-host',
  templateUrl: './widget-host.component.html',
  styleUrls: ['./widget-host.component.scss'],
  standalone: false
})
export class WidgetHostComponent implements OnInit, OnDestroy {
  @Input() widget!: Widget;

  echartsOptions: EChartOption | null = null;
  kpiValue: number | string = '—';
  kpiTrend: number | null = null;
  narrative = '';
  private readonly destroy$ = new Subject<void>();

  constructor(private readonly svc: DashboardBuilderService) {}

  ngOnInit(): void {
    this.computeView();
    this.svc.onFilter()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.computeView());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Stub view computation — in production this would call the AI service
   * (RAG / NLQ) or the quality-engine KPI endpoint to retrieve the data.
   */
  private computeView(): void {
    switch (this.widget.type) {
      case 'kpi':
        this.kpiValue = 27;
        this.kpiTrend = -4;
        break;
      case 'narrative':
        this.narrative = (this.widget.config['text'] as string)
          ?? 'AI storyboard: no insights yet — connect data to populate.';
        break;
      default:
        this.echartsOptions = this.placeholderOption();
    }
  }

  private placeholderOption(): EChartOption {
    return {
      title: { text: this.widget.title, left: 'center', textStyle: { fontSize: 14 } },
      tooltip: {},
      xAxis: { type: 'category', data: ['Q1', 'Q2', 'Q3', 'Q4'] },
      yAxis: { type: 'value' },
      series: [{ type: this.widget.type === 'pie' ? 'pie' : 'bar', data: [12, 18, 9, 21] }]
    };
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
