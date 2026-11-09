/**
 * Types de la détection d'anomalies SPC (§3.4, §12.1). Le SPA envoie une série de
 * mesures à l'engine (`POST /api/v1/ai/spc/analyze`) qui relaie vers ai-service
 * (limites de contrôle + 8 règles de Nelson, NumPy). Le tenant vient du JWT.
 */

export interface SpcAnalyzeRequest {
  values: number[];
  /** Baseline processus connue (center + sigma fournis ensemble), sinon limites estimées. */
  center?: number;
  sigma?: number;
}

export interface SpcLimits {
  centerLine: number;
  sigma: number;
  ucl: number;
  lcl: number;
  estimated: boolean;
}

export interface SpcViolation {
  rule: string;
  title: string;
  description: string;
  pointIndices: number[];
  severity: string;
}

export interface SpcAnalyzeResponse {
  n: number;
  outOfControl: boolean;
  limits: SpcLimits;
  violations: SpcViolation[];
}

/** Option de la liste déroulante des KPI (mode « depuis un KPI »). */
export interface KpiOption {
  id: string;
  code: string;
  name: string;
  unit?: string;
}

/**
 * Réponse SPC d'un KPI : série tirée de kpi_measurements + analyse + éventuelle
 * CAPA ouverte sur dérive (capaId non nul si une CAPA a été créée).
 */
export interface KpiSpcResponse {
  kpiId: string;
  kpiCode: string;
  kpiName: string;
  unit?: string;
  periods: string[];
  values: number[];
  analysis: SpcAnalyzeResponse;
  capaId?: string | null;
}

// --- prévision de sortie des limites (§6.5) ----------------------------------

/**
 * Une période à venir : où le procédé se dirige, et le risque qu'il sorte.
 *
 * <p>`pBreach` additionne les deux côtés — ils sont exclusifs, puisque la limite
 * supérieure est au-dessus de l'inférieure.
 */
export interface SpcForecastPoint {
  step: number;
  value: number;
  low: number;
  high: number;
  pAboveUcl: number;
  pBelowLcl: number;
  pBreach: number;
}

/**
 * Prévision de dérive.
 *
 * <p>`firstBreachStep` est nul tant que le procédé reste dans ses limites sur
 * tout l'horizon. `confidence`, `r2` et `trend` accompagnent le chiffre : un
 * risque élevé sur une série courte n'est pas une prédiction, et l'écran doit
 * pouvoir le dire (§12.3).
 */
export interface SpcForecastResponse {
  n: number;
  horizon: number;
  threshold: number;
  willBreach: boolean;
  firstBreachStep?: number | null;
  side?: 'upper' | 'lower' | null;
  trend: number;
  residualSigma: number;
  r2: number;
  confidence: 'low' | 'medium' | 'high';
  model: string;
  limits: SpcLimits;
  points: SpcForecastPoint[];
}

/** Prévision SPC d'un KPI : la série réelle, la prévision, et l'éventuelle CAPA préventive. */
export interface KpiSpcForecastResponse {
  kpiId: string;
  kpiCode: string;
  kpiName: string;
  unit?: string;
  periods: string[];
  values: number[];
  forecast: SpcForecastResponse;
  capaId?: string | null;
}
