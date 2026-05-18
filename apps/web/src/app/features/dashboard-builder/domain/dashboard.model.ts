/**
 * Dashboard-builder domain models — pure TS, NO Angular imports.
 * CLAUDE.md §7.1 / §7.3.
 */

export type WidgetType =
  | 'kpi'
  | 'line'
  | 'bar'
  | 'pie'
  | 'gauge'
  | 'table'
  | 'heatmap'
  | 'narrative';

export interface WidgetPosition {
  readonly x: number;
  readonly y: number;
  readonly cols: number;
  readonly rows: number;
}

export interface Widget {
  readonly id: string;
  readonly type: WidgetType;
  readonly title: string;
  readonly position: WidgetPosition;
  /** Widget-specific config (data source, ECharts option, KPI ref...). */
  readonly config: Record<string, unknown>;
}

export interface DashboardLayout {
  readonly id?: string;
  readonly tenantId?: string;
  readonly userId?: string;
  readonly name: string;
  readonly description?: string;
  readonly widgets: ReadonlyArray<Widget>;
  readonly shared: boolean;
  readonly signatureHash?: string;
  readonly version?: number;
}

export interface CrossFilter {
  readonly sourceWidgetId: string;
  readonly field: string;
  readonly value: string | number | null;
}
