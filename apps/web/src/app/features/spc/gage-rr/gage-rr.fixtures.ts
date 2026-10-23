import { computeGageRr } from './gage-rr.calc';
import { WORKBOOK_EXAMPLE } from './gage-rr.examples';
import { GageRrCalculationView, GageRrSheet, GageRrSummary } from './gage-rr.types';

/**
 * Les jeux des tests de l'écran : la grille du classeur
 * `docs/spc/Etude_RR_Gage_1.xlsx` et les valeurs qu'il donne, reprises du test
 * doré du serveur (`GageRrCalculationTest`).
 */
export const GOLDEN_GRID: number[][][] = WORKBOOK_EXAMPLE.grid;
export const GOLDEN_LSL = 9.9;
export const GOLDEN_USL = 10.1;

export const GOLDEN = {
  operatorMeans: [10.010167, 10.018433, 10.008],
  operatorRanges: [0.0117, 0.0154, 0.0136],
  xDiff: 0.0104333,
  meanRange: 0.0135667,
  partRange: 0.1422222,
  rangeUcl: 0.035002,
  maxRange: 0.028,
  grandMean: 10.0122,
  ev: 0.0413783,
  av: 0.0271381,
  grr: 0.0494838,
  pv: 0.2304,
  tv: 0.235654,
  grrPercent: 0.2100,
  grrTolerance: 0.2474,
  ndc: 6,
  anova: {
    ssParts: 0.15117262,
    ssOperators: 0.00181887,
    ssInteraction: 0.00115158,
    ssRepeatability: 0.00343333,
    ssTotal: 0.1575764,
    fInteraction: 1.118,
    pInteraction: 0.35839,
    fPartsPooled: 285.755,
    fOperatorsPooled: 15.4716,
    pOperatorsPooled: 2.19e-6,
    msPool: 5.87809e-5,
    varOperator: 2.83551e-5,
    varPart: 0.0018597975,
    varTotal: 0.0019469334,
    ev: 0.0394844,
    av: 0.0274235,
    grr: 0.0480735,
    pv: 0.2220957,
    tv: 0.227239,
    grrPercent: 0.21155,
    grrTolerance: 0.24037,
    ndc: 6
  }
};

export const OPERATORS = [
  { accountId: 'op-a', name: 'Alice Martin' },
  { accountId: 'op-b', name: 'Bruno Petit' },
  { accountId: 'op-c', name: 'Chloé Durand' }
];

export function goldenCalculation(): GageRrCalculationView {
  return computeGageRr(GOLDEN_LSL, GOLDEN_USL, 5.15, GOLDEN_GRID) as GageRrCalculationView;
}

export function summary(over: Partial<GageRrSummary> = {}): GageRrSummary {
  return {
    id: 'g1',
    partReference: 'Arbre AX-210',
    characteristic: 'Diamètre extérieur (mm)',
    instrument: 'Micromètre',
    studyDate: '2026-10-19',
    responsibleUserId: 'resp-1',
    responsibleName: 'Jeanne Leroy',
    operators: OPERATORS,
    operatorCount: 3,
    partCount: 10,
    trialCount: 3,
    sigmaMultiplier: 5.15,
    grrPercent: 21,
    grrTolerancePercent: 24.74,
    ndc: 6,
    decision: 'CONDITIONAL',
    anovaGrrPercent: 21.16,
    anovaNdc: 6,
    updatedAt: '2026-10-19T10:00:00Z',
    ...over
  };
}

export function sheet(over: Partial<GageRrSummary> = {}): GageRrSheet {
  return {
    study: summary(over),
    lsl: GOLDEN_LSL,
    usl: GOLDEN_USL,
    measurements: GOLDEN_GRID,
    calculation: goldenCalculation()
  };
}
