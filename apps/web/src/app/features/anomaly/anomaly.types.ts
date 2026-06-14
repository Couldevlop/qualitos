/**
 * Types de la détection d'anomalies non-supervisée multivariée (§3.4, §12.1).
 * Le SPA envoie une matrice (échantillons × features) à l'engine
 * (`POST /api/v1/ai/anomaly/detect`) qui applique les garde-fous IA (OWASP LLM04)
 * et relaie vers ai-service (Isolation Forest ou reconstruction par ACP, NumPy pur).
 * Le tenant vient du JWT côté serveur (jamais envoyé dans le body).
 */

/** Méthode de détection supportée par l'engine/ai-service. */
export type AnomalyMethod = 'isolation_forest' | 'reconstruction';

export interface AnomalyDetectRequest {
  samples: number[][];
  /** Défaut côté serveur : isolation_forest. */
  method?: AnomalyMethod;
  /** Fraction d'anomalies attendue ∈ (0, 0.5]. Ignorée si threshold est fourni. */
  contamination?: number;
  /** Seuil explicite sur le score (optionnel) ; sinon quantile de contamination. */
  threshold?: number;
}

/** Score d'anomalie d'un échantillon (index 0-based dans la matrice d'entrée). */
export interface AnomalyPoint {
  index: number;
  score: number;
  isAnomaly: boolean;
  /** Feature contribuant le plus à l'erreur (mode reconstruction) ; null sinon. */
  topFeature: number | null;
}

export interface AnomalyDetectResponse {
  n: number;
  nFeatures: number;
  method: AnomalyMethod;
  contamination: number;
  threshold: number;
  anomalyCount: number;
  hasAnomalies: boolean;
  points: AnomalyPoint[];
}
