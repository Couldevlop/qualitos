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
