/**
 * Application layer use case — orchestrates port calls.
 * Inject only the port via InjectionToken; never the HTTP adapter directly.
 */
import { Inject, Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

import {
  CrossFilter,
  DashboardExportResult,
  DashboardLayout,
  ExportWidgetSnapshot,
  Widget
} from '../domain/dashboard.model';
import { DashboardLayoutRepository } from '../domain/dashboard-layout.repository';
import { DASHBOARD_LAYOUT_REPOSITORY } from '../domain/dashboard-builder.tokens';

@Injectable()
export class DashboardBuilderService {

  /** Cross-filter bus — widgets subscribe to react to filters. */
  private readonly crossFilter$ = new Subject<CrossFilter | null>();

  constructor(
    @Inject(DASHBOARD_LAYOUT_REPOSITORY)
    private readonly repo: DashboardLayoutRepository
  ) {}

  list(): Observable<DashboardLayout[]> {
    return this.repo.list();
  }

  get(id: string): Observable<DashboardLayout> {
    return this.repo.get(id);
  }

  save(layout: DashboardLayout): Observable<DashboardLayout> {
    return this.repo.save(layout);
  }

  update(id: string, layout: DashboardLayout): Observable<DashboardLayout> {
    return this.repo.update(id, layout);
  }

  delete(id: string): Observable<void> {
    return this.repo.delete(id);
  }

  /**
   * Export the given dashboard as a signed (ML-DSA) + blockchain-anchored PDF
   * with a verification QR code (§7.3/§7.4). Widget snapshots are built from the
   * current layout so the exported PDF mirrors what's on screen.
   */
  exportPdf(layout: DashboardLayout): Observable<DashboardExportResult> {
    if (!layout.id) {
      throw new Error('dashboard must be saved before export');
    }
    return this.repo.exportPdf(layout.id, this.toExportSnapshots(layout));
  }

  /** Builds printable, human-readable snapshots from the layout's widgets. */
  toExportSnapshots(layout: DashboardLayout): ExportWidgetSnapshot[] {
    return layout.widgets.map(w => ({
      title: w.title,
      type: w.type,
      dataLines: Object.entries(w.config ?? {})
        .map(([k, v]) => `${k}: ${this.formatValue(v)}`)
    }));
  }

  private formatValue(value: unknown): string {
    if (value === null || value === undefined) {
      return '-';
    }
    if (typeof value === 'object') {
      return JSON.stringify(value);
    }
    return String(value);
  }

  /** Emit a cross-filter to all subscribed widgets. */
  emitFilter(filter: CrossFilter | null): void {
    this.crossFilter$.next(filter);
  }

  onFilter(): Observable<CrossFilter | null> {
    return this.crossFilter$.asObservable();
  }

  /**
   * Builds a default new layout with a single KPI widget.
   */
  newDefaultLayout(name: string): DashboardLayout {
    const widget: Widget = {
      id: this.generateId(),
      type: 'kpi',
      title: 'CAPA closure rate',
      position: { x: 0, y: 0, cols: 3, rows: 2 },
      config: { kpiId: 'capa_closure_time_avg', threshold: 30 }
    };
    return {
      name,
      widgets: [widget],
      shared: false
    };
  }

  generateId(): string {
    return 'w_' + Math.random().toString(36).slice(2, 11);
  }
}
