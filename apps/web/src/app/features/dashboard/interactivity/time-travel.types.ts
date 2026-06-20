/** État as-of d'un KPI à une date donnée (§7.3 time-travel). */
export interface KpiAsOfSnapshot {
  kpiId: string;
  code: string;
  name: string;
  unit?: string | null;
  value?: number | null;
  measuredPeriodStart?: string | null;
  present: boolean;
}

/** Réponse time-travel : snapshot de tous les KPIs du tenant à l'instant `asOf`. */
export interface DashboardSnapshot {
  asOf: string;
  /** True si aucune donnée à cette date (état vide soigné côté UI). */
  empty: boolean;
  kpis: KpiAsOfSnapshot[];
}
