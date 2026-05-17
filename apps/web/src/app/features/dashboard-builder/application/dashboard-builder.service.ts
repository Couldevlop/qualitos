/**
 * Application layer use case — orchestrates port calls.
 * Inject only the port via InjectionToken; never the HTTP adapter directly.
 */
import { Inject, Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

import {
  CrossFilter,
  DashboardLayout,
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
