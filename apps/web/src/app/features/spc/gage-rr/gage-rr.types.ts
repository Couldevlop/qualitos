/**
 * Le contrat des études Gage R&R (`/api/v1/spc/gage-rr`, ADR 0097).
 *
 * Le %GRR, le ndc et le verdict enregistrés viennent toujours du serveur ;
 * l'écran refait le calcul (même table, mêmes formules) pour suivre la saisie.
 * Les pourcentages du calcul sont des fractions (0,21 pour 21 %), ceux du
 * registre des pourcentages à deux décimales (21,00).
 */

export type GageRrDecision = 'ACCEPTABLE' | 'CONDITIONAL' | 'NOT_ACCEPTABLE';

/** Un opérateur : une personne de l'annuaire, son compte et son nom recopié. */
export interface GageRrOperator {
  accountId: string;
  name: string;
}

/** Une source de variation et ses trois lectures. */
export interface VariationComponent {
  value: number;
  percentOfTotal: number;
  percentContribution: number;
  /** `null` à l'écran tant que la tolérance (USL − LSL) n'est pas valide. */
  percentTolerance: number | null;
}

export type AnovaSource = 'PART' | 'OPERATOR' | 'INTERACTION' | 'REPEATABILITY' | 'TOTAL';

export interface AnovaRow {
  source: AnovaSource;
  degreesOfFreedom: number;
  sumOfSquares: number;
  meanSquare: number | null;
  f: number | null;
  p: number | null;
}

export interface AnovaVariance {
  repeatability: number;
  operator: number;
  interaction: number;
  reproducibility: number;
  grr: number;
  part: number;
  total: number;
}

export interface AnovaView {
  fullTable: AnovaRow[];
  pooledTable: AnovaRow[] | null;
  interactionPooled: boolean;
  variance: AnovaVariance;
  ev: VariationComponent;
  av: VariationComponent;
  operator: VariationComponent;
  interaction: VariationComponent;
  grr: VariationComponent;
  pv: VariationComponent;
  tv: VariationComponent;
  ndc: number;
  ndcSufficient: boolean;
  decision: GageRrDecision;
}

export interface GageRrCalculationView {
  /** [opérateur][pièce] */
  cellMeans: number[][];
  cellRanges: number[][];
  /** [opérateur][essai] : la ligne « Moyenne » du classeur. */
  trialMeans: number[][];
  operatorMeans: number[];
  operatorRanges: number[];
  partMeans: number[];
  grandMean: number;
  xDiff: number;
  meanRange: number;
  partRange: number;
  rangeUcl: number;
  maxRange: number;
  rangesBeyondLimit: number;
  tolerance: number;
  k1: number;
  k2: number;
  k3: number;
  d4: number;
  multiplier: number;
  ev: VariationComponent;
  av: VariationComponent;
  grr: VariationComponent;
  pv: VariationComponent;
  tv: VariationComponent;
  ndc: number;
  ndcSufficient: boolean;
  decision: GageRrDecision;
  repeatabilityDominant: boolean;
  anova: AnovaView;
}

export interface GageRrSummary {
  id: string;
  partReference: string;
  characteristic: string | null;
  instrument: string | null;
  studyDate: string;
  responsibleUserId: string | null;
  responsibleName: string | null;
  operators: GageRrOperator[];
  operatorCount: number;
  partCount: number;
  trialCount: number;
  sigmaMultiplier: number;
  grrPercent: number;
  grrTolerancePercent: number;
  ndc: number;
  decision: GageRrDecision;
  anovaGrrPercent: number;
  anovaNdc: number;
  updatedAt: string;
}

export interface GageRrSheet {
  study: GageRrSummary;
  lsl: number;
  usl: number;
  /** [opérateur][pièce][essai] */
  measurements: number[][][];
  calculation: GageRrCalculationView;
}

/** Le corps d'écriture : ni tenant, ni auteur, ni %GRR, ni verdict. */
export interface GageRrRequest {
  partReference: string;
  characteristic: string | null;
  instrument: string | null;
  lsl: number;
  usl: number;
  operatorCount: number;
  partCount: number;
  trialCount: number;
  sigmaMultiplier: number;
  studyDate: string;
  responsibleUserId: string | null;
  responsibleName: string | null;
  operators: GageRrOperator[];
  measurements: number[][][];
}
