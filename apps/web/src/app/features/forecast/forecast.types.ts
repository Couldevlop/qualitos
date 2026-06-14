/**
 * Types de la prévision KPI (§6.5, §12.1). Le SPA envoie une série chronologique à
 * l'engine (`POST /api/v1/ai/forecast/kpi`) qui applique les garde-fous IA (OWASP LLM04)
 * et relaie vers ai-service (lissage exponentiel Holt-Winters, NumPy pur). Le tenant vient
 * du JWT côté serveur (jamais envoyé dans le body).
 */

export type ForecastDirection = 'at_least' | 'at_most';

export interface ForecastRequest {
  values: number[];
  target: number;
  horizon?: number;
  direction?: ForecastDirection;
  /** Période saisonnière (≥ 2). Omise = pas de saisonnalité (Holt linéaire). */
  seasonalPeriod?: number;
}

export interface ForecastPoint {
  step: number;   // 1..horizon (périodes après la dernière observation)
  value: number;  // prévision ponctuelle
  low: number;    // intervalle de prédiction 95 %
  high: number;
}

export interface ForecastResponse {
  n: number;
  slope: number;          // tendance finale (explicabilité)
  intercept: number;      // niveau final
  residualSigma: number;
  r2: number;
  horizon: number;
  target: number;
  direction: ForecastDirection;
  probability: number;    // P(cible atteinte à l'horizon), 0..1
  confidence: string;     // "low" | "medium" | "high"
  model: string;          // "holt_linear" | "holt_winters_additive"
  seasonalPeriod: number; // 0 = aucune
  points: ForecastPoint[];
}
