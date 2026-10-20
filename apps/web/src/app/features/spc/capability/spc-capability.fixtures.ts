import { Measurement, StudySheet, StudySummary } from './spc-capability.types';

/**
 * Jeu d'essai des tests : la grille du classeur de référence (« SPC Cp Cpk
 * Study Template.xlsx », poids 500 ± 25 g, n = 5), et les valeurs que le
 * classeur en tire — les mêmes que le test doré du serveur.
 */
export const GOLDEN_GRID: Measurement[][] = [
  [504, 508, 497, 504, 496, 493, 494, 494, 507, 499, 500, 496, 494, 505, 499, 498, 508, 495, 493, 495],
  [497, 505, 508, 497, 498, 507, 498, 502, 501, 507, 504, 505, 494, 495, 495, 498, 506, 497, 499, 497],
  [497, 492, 496, 494, 502, 496, 498, 502, 497, 498, 496, 493, 502, 496, 496, 499, 506, 492, 506, 500],
  [497, 494, 501, 498, 493, 504, 506, 498, 494, 498, 494, 500, 503, 502, 498, 507, 493, 500, 504, 497],
  [498, 492, 496, 500, 494, 507, 497, 502, 500, 499, 500, 499, 494, 502, 500, 500, 503, 502, 498, 492]
];

export const GOLDEN_LSL = 475;
export const GOLDEN_USL = 525;

export const GOLDEN = {
  sumOfMeans: 9980.6,
  grandMean: 499.03,
  meanRange: 10.55,
  sigma: 4.5279,
  cp: 1.84,
  cpu: 1.912,
  cpl: 1.769,
  cpk: 1.769,
  meanUcl: 505.149,
  meanLcl: 492.911,
  rangeUcl: 22.261,
  rangeLcl: 0,
  dispersionPercent: 54.33,
  centerOffset: -0.97,
  centeringPercent: 3.88
};

export function summary(over: Partial<StudySummary> = {}): StudySummary {
  return {
    id: 's1', partReference: 'Shaft AX', characteristic: 'Weight', specification: '500 ± 25 gram',
    machine: 'M-12', leaderUserId: 'u1', leaderName: 'Jean Dupont', studyDate: '2026-10-01',
    actionPlan: false, cp: 1.84, cpk: 1.769, status: 'CAPABLE',
    capableThreshold: 1.67, watchThreshold: 1.33, updatedAt: '2026-10-01T10:00:00Z',
    ...over
  };
}

export function sheet(over: Partial<StudySummary> = {}): StudySheet {
  return {
    study: summary(over),
    lsl: GOLDEN_LSL,
    usl: GOLDEN_USL,
    subgroupSize: 5,
    measurements: GOLDEN_GRID.map(r => [...r]),
    calculation: {
      subgroupMeans: [], subgroupRanges: [], completeSubgroups: 20,
      ...GOLDEN, d2: 2.33, a2: 0.58, d3: 0, d4: 2.11
    }
  };
}
