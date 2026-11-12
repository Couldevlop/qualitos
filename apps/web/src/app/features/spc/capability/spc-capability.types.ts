/**
 * Le contrat des études de capabilité (`/api/v1/spc/studies`, ADR 0096).
 *
 * Cp et Cpk viennent toujours du serveur pour ce qui est enregistré ; l'écran
 * les recalcule seulement pour suivre la saisie en direct.
 */

export type CapabilityStatus = 'CAPABLE' | 'WATCH' | 'NOT_CAPABLE';

/** Une cellule de la grille : une mesure, ou rien. */
export type Measurement = number | null;

/**
 * La carte qui mesure la dispersion d'un sous-groupe (ADR 0107).
 *
 * - `RANGE` : carte X̄ – R, dispersion = étendue max − min, σ = R̄ / d2.
 *   Valide jusqu'à dix mesures par sous-groupe, là où s'arrêtent les tables de
 *   d2, A2, D3 et D4.
 * - `STDEV` : carte X̄ – S, dispersion = écart-type d'échantillon s
 *   (diviseur n − 1), σ = s̄ / c4. De onze à vingt-cinq mesures.
 */
export type DispersionChart = 'RANGE' | 'STDEV';

export interface StudySummary {
  id: string;
  partReference: string;
  characteristic: string | null;
  specification: string | null;
  machine: string | null;
  leaderUserId: string | null;
  leaderName: string | null;
  studyDate: string;
  actionPlan: boolean;
  cp: number;
  cpk: number;
  status: CapabilityStatus;
  capableThreshold: number;
  watchThreshold: number;
  updatedAt: string;
}

/**
 * Les valeurs du tableau des formules.
 *
 * `subgroupRanges`, `meanRange`, `rangeUcl` et `rangeLcl` portent la dispersion
 * de la carte retenue : l'étendue sur une carte X̄ – R, l'écart-type
 * d'échantillon sur une carte X̄ – S. Les noms sont ceux du contrat d'origine,
 * gardés pour ne rien casser ; `dispersionChart` dit lequel des deux s'applique.
 */
export interface CalculationView {
  subgroupMeans: (number | null)[];
  subgroupRanges: (number | null)[];
  completeSubgroups: number;
  sumOfMeans: number | null;
  grandMean: number | null;
  meanRange: number | null;
  sigma: number | null;
  cp: number | null;
  cpu: number | null;
  cpl: number | null;
  cpk: number | null;
  meanUcl: number | null;
  meanLcl: number | null;
  rangeUcl: number | null;
  rangeLcl: number | null;
  dispersionChart: DispersionChart;
  /** d2 (carte R) ou c4 (carte S). */
  sigmaDivisor: number;
  /** A2 (carte R) ou A3 (carte S). */
  meanFactor: number;
  /** D3 (carte R) ou B3 (carte S). */
  dispersionLower: number;
  /** D4 (carte R) ou B4 (carte S). */
  dispersionUpper: number;
  dispersionPercent: number | null;
  centerOffset: number | null;
  centeringPercent: number | null;
}

export interface StudySheet {
  study: StudySummary;
  lsl: number;
  usl: number;
  subgroupSize: number;
  measurements: Measurement[][];
  calculation: CalculationView;
}

/** Le corps d'écriture : ni tenant, ni auteur, ni Cp, ni Cpk. */
export interface StudyRequest {
  partReference: string;
  characteristic: string | null;
  specification: string | null;
  machine: string | null;
  leaderUserId: string | null;
  leaderName: string | null;
  studyDate: string;
  actionPlan: boolean;
  lsl: number;
  usl: number;
  subgroupSize: number;
  measurements: Measurement[][];
}

export interface SpcSettings {
  capableThreshold: number;
  watchThreshold: number;
  /** Rien n'a encore été réglé : ce sont les seuils de la plateforme. */
  defaults: boolean;
}

/**
 * La grille lue dans un classeur Excel versé (import des mesures).
 *
 * <p>Les valeurs restent du TEXTE : elles se posent dans la grille de saisie
 * telle quelle, et c'est l'enregistrement de l'étude qui convertit et recalcule.
 * `warnings` dit ce que la lecture a écarté — une cellule de texte, des lignes
 * en trop — parce qu'un import silencieux laisse croire que tout est passé.
 */
export interface ImportedGrid {
  sampleSize: number;
  subgroupCount: number;
  rows: string[][];
  warnings: string[];
}
