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
  /**
   * Ce que décrit la matrice — une ligne, un équipement, un lot. Exigé pour
   * ouvrir une CAPA : une action corrective qui ne dit pas sur quoi elle porte
   * n'est pas exploitable, et ce libellé sert aussi de clé anti-doublon.
   */
  subject?: string;
  /** Ouvre une CAPA corrective si des anomalies ressortent (ADR 0022). */
  openCapa?: boolean;
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
  /**
   * CAPA ouverte sur cette détection, ou null : ouverture non demandée, aucune
   * anomalie, sujet absent, ou dossier déjà ouvert sur le même sujet.
   */
  capaId: string | null;
}

export interface AnomalyExplainRequest {
  samples: number[][];
  index: number;
}

/** Attribution Shapley signée d'une feature (positive = pousse vers l'anormalité). */
export interface FeatureContribution {
  feature: number;
  value: number;
  contribution: number;
}

export interface AnomalyExplainResponse {
  index: number;
  method: string;
  score: number;
  baseValue: number;       // E[score] sur l'arrière-plan
  contributions: FeatureContribution[];
}
