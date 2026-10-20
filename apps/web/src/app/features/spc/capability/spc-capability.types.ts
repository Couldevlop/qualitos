/**
 * Le contrat des études de capabilité (`/api/v1/spc/studies`, ADR 0096).
 *
 * Cp et Cpk viennent toujours du serveur pour ce qui est enregistré ; l'écran
 * les recalcule seulement pour suivre la saisie en direct.
 */

export type CapabilityStatus = 'CAPABLE' | 'WATCH' | 'NOT_CAPABLE';

/** Une cellule de la grille : une mesure, ou rien. */
export type Measurement = number | null;

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
  d2: number;
  a2: number;
  d3: number;
  d4: number;
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
