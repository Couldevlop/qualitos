import {
  AnovaRow, AnovaView, GageRrCalculationView, GageRrDecision, VariationComponent
} from './gage-rr.types';

/**
 * Le calcul R&R côté écran, pour suivre la frappe : la méthode « moyenne et
 * étendue » du classeur `docs/spc/Etude_RR_Gage_1.xlsx` et l'ANOVA à deux
 * facteurs avec interaction (AIAG). Mêmes constantes, mêmes formules que
 * `GageRrCalculation` côté serveur ; un test doré commun les tient ensemble.
 * Ce qui est enregistré vient toujours du serveur.
 *
 * La grille se lit `grid[opérateur][pièce][essai]`.
 */

export const REFERENCE_MULTIPLIER = 5.15;
export const SIGMA_MULTIPLIERS: readonly number[] = [5.15, 6];
export const OPERATOR_COUNTS: readonly number[] = [2, 3];
export const TRIAL_COUNTS: readonly number[] = [2, 3];
export const PART_COUNTS: readonly number[] = [2, 3, 4, 5, 6, 7, 8, 9, 10];
export const MAX_OPERATORS = 3;
export const MAX_PARTS = 10;
export const MAX_TRIALS = 3;
export const NDC_FACTOR = 1.41;
export const NDC_SUFFICIENT = 5;
export const POOLING_P = 0.25;
const RESIDUAL = 1e-12;

/** L'onglet « Constantes » : valeurs pour 5,15 σ (AIAG, MSA 4e éd.). */
export const K1: Readonly<Record<number, number>> = { 2: 4.56, 3: 3.05 };
export const K2: Readonly<Record<number, number>> = { 2: 3.65, 3: 2.70 };
export const K3: Readonly<Record<number, number>> = {
  2: 3.65, 3: 2.70, 4: 2.30, 5: 2.08, 6: 1.93, 7: 1.82, 8: 1.74, 9: 1.67, 10: 1.62
};
export const D4: Readonly<Record<number, number>> = { 2: 3.27, 3: 2.58 };
/** A2 des cartes X̄ – R, pour les limites de la carte des moyennes par opérateur. */
export const A2: Readonly<Record<number, number>> = { 2: 1.88, 3: 1.02 };

export interface GageRrConstants { k1: number; k2: number; k3: number; d4: number }

/** Les constantes d'un plan admis, mises à l'échelle du multiplicateur ; `null` hors table. */
export function constantsFor(k: number, n: number, r: number, multiplier: number): GageRrConstants | null {
  if (!(k in K2) || !(n in K3) || !(r in K1) || !SIGMA_MULTIPLIERS.includes(multiplier)) return null;
  const s = multiplier / REFERENCE_MULTIPLIER;
  return { k1: K1[r] * s, k2: K2[k] * s, k3: K3[n] * s, d4: D4[r] };
}

/** La règle de la cellule C38 : < 10 % acceptable, 10 – 30 % sous conditions, > 30 % non acceptable. */
export function decisionOf(ratio: number): GageRrDecision {
  if (ratio < 0.10) return 'ACCEPTABLE';
  return ratio <= 0.30 ? 'CONDITIONAL' : 'NOT_ACCEPTABLE';
}

/** INT(1,41 × PV / GRR), 0 quand le système de mesure ne varie pas. */
export function ndcOf(pv: number, grr: number): number {
  return grr > 0 ? Math.floor(NDC_FACTOR * pv / grr) : 0;
}

/** La grille est complète et finie, aux dimensions d'un plan admis. */
export function isComplete(grid: (number | null)[][][]): grid is number[][][] {
  const k = grid.length;
  const n = grid[0]?.length ?? 0;
  const r = grid[0]?.[0]?.length ?? 0;
  if (!(k in K2) || !(n in K3) || !(r in K1)) return false;
  return grid.every(bloc => bloc.length === n
    && bloc.every(essais => essais.length === r && essais.every(v => v !== null && Number.isFinite(v))));
}

function component(value: number, total: number, tolerance: number | null): VariationComponent {
  const ratio = total > 0 ? value / total : 0;
  return {
    value,
    percentOfTotal: ratio,
    percentContribution: ratio * ratio,
    percentTolerance: tolerance !== null && tolerance > 0 ? value / tolerance : null
  };
}

const mean = (v: number[]): number => v.reduce((a, b) => a + b, 0) / v.length;
const spread = (v: number[]): number => Math.max(...v) - Math.min(...v);

/**
 * Le calcul complet. `null` si le plan est hors table ou le multiplicateur
 * inconnu. `lsl`/`usl` invalides laissent les pourcentages de tolérance nuls.
 */
export function computeGageRr(lsl: number | null, usl: number | null, multiplier: number,
                              grid: number[][][]): GageRrCalculationView | null {
  const k = grid.length;
  const n = grid[0]?.length ?? 0;
  const r = grid[0]?.[0]?.length ?? 0;
  const c = constantsFor(k, n, r, multiplier);
  if (!c) return null;
  const tolerance = lsl !== null && usl !== null && usl > lsl ? usl - lsl : null;

  // ---------- étape 1 ----------
  const cellMeans = grid.map(bloc => bloc.map(mean));
  const cellRanges = grid.map(bloc => bloc.map(spread));
  const trialMeans = grid.map(bloc => Array.from({ length: r }, (_, t) => mean(bloc.map(e => e[t]))));
  const operatorMeans = cellMeans.map(mean);
  const operatorRanges = cellRanges.map(mean);
  const partMeans = Array.from({ length: n }, (_, p) => mean(grid.flatMap(bloc => bloc[p])));
  const grandMean = mean(grid.flat(2));
  const xDiff = spread(operatorMeans);
  const meanRange = mean(operatorRanges);
  const partRange = spread(partMeans);
  const rangeUcl = c.d4 * meanRange;
  const allRanges = cellRanges.flat();
  const maxRange = Math.max(...allRanges);
  const rangesBeyondLimit = allRanges.filter(e => e > rangeUcl).length;

  // ---------- étape 3 ----------
  const ev = meanRange * c.k1;
  const av = Math.sqrt(Math.max(0, (xDiff * c.k2) ** 2 - ev * ev / (n * r)));
  const grr = Math.hypot(ev, av);
  const pv = partRange * c.k3;
  const tv = Math.hypot(grr, pv);
  const ndc = ndcOf(pv, grr);

  return {
    cellMeans, cellRanges, trialMeans, operatorMeans, operatorRanges, partMeans, grandMean,
    xDiff, meanRange, partRange, rangeUcl, maxRange, rangesBeyondLimit,
    tolerance: tolerance ?? 0,
    k1: c.k1, k2: c.k2, k3: c.k3, d4: c.d4, multiplier,
    ev: component(ev, tv, tolerance), av: component(av, tv, tolerance), grr: component(grr, tv, tolerance),
    pv: component(pv, tv, tolerance), tv: component(tv, tv, tolerance),
    ndc, ndcSufficient: ndc >= NDC_SUFFICIENT,
    decision: decisionOf(tv > 0 ? grr / tv : 0),
    repeatabilityDominant: ev > av,
    anova: anova(grid, cellMeans, partMeans, operatorMeans, grandMean, multiplier, tolerance)
  };
}

function anova(grid: number[][][], cellMeans: number[][], partMeans: number[], operatorMeans: number[],
               grandMean: number, multiplier: number, tolerance: number | null): AnovaView {
  const k = grid.length;
  const n = partMeans.length;
  const r = grid[0][0].length;
  const sq = (v: number) => (v - grandMean) * (v - grandMean);
  const ssParts = k * r * partMeans.reduce((a, pm) => a + sq(pm), 0);
  const ssOperators = n * r * operatorMeans.reduce((a, om) => a + sq(om), 0);
  let ssTotal = 0;
  let ssRepeat = 0;
  grid.forEach((bloc, o) => bloc.forEach((essais, p) => essais.forEach(v => {
    ssTotal += sq(v);
    ssRepeat += (v - cellMeans[o][p]) ** 2;
  })));
  const reste = ssTotal - ssParts - ssOperators - ssRepeat;
  const ssInteraction = reste <= RESIDUAL * ssTotal ? 0 : reste;

  const dfParts = n - 1;
  const dfOperators = k - 1;
  const dfInteraction = (n - 1) * (k - 1);
  const dfRepeat = n * k * (r - 1);
  const dfTotal = n * k * r - 1;
  const msParts = ssParts / dfParts;
  const msOperators = ssOperators / dfOperators;
  const msInteraction = ssInteraction / dfInteraction;
  const msRepeat = ssRepeat / dfRepeat;

  const fInteraction = ratio(msInteraction, msRepeat);
  const pInteraction = fInteraction === null ? null : fSurvival(fInteraction, dfInteraction, dfRepeat);
  const fullTable: AnovaRow[] = [
    row('PART', dfParts, ssParts, msParts, msInteraction, dfInteraction),
    row('OPERATOR', dfOperators, ssOperators, msOperators, msInteraction, dfInteraction),
    { source: 'INTERACTION', degreesOfFreedom: dfInteraction, sumOfSquares: ssInteraction,
      meanSquare: msInteraction, f: fInteraction, p: pInteraction },
    { source: 'REPEATABILITY', degreesOfFreedom: dfRepeat, sumOfSquares: ssRepeat, meanSquare: msRepeat, f: null, p: null },
    { source: 'TOTAL', degreesOfFreedom: dfTotal, sumOfSquares: ssTotal, meanSquare: null, f: null, p: null }
  ];

  const pooled = pInteraction === null ? msInteraction === 0 : pInteraction > POOLING_P;
  let pooledTable: AnovaRow[] | null = null;
  let repeatability: number;
  let interaction: number;
  let operator: number;
  let part: number;
  if (pooled) {
    const dfPool = dfInteraction + dfRepeat;
    const msPool = (ssInteraction + ssRepeat) / dfPool;
    pooledTable = [
      row('PART', dfParts, ssParts, msParts, msPool, dfPool),
      row('OPERATOR', dfOperators, ssOperators, msOperators, msPool, dfPool),
      { source: 'REPEATABILITY', degreesOfFreedom: dfPool, sumOfSquares: ssInteraction + ssRepeat,
        meanSquare: msPool, f: null, p: null },
      { source: 'TOTAL', degreesOfFreedom: dfTotal, sumOfSquares: ssTotal, meanSquare: null, f: null, p: null }
    ];
    repeatability = msPool;
    interaction = 0;
    operator = Math.max(0, (msOperators - msPool) / (n * r));
    part = Math.max(0, (msParts - msPool) / (k * r));
  } else {
    repeatability = msRepeat;
    interaction = Math.max(0, (msInteraction - msRepeat) / r);
    operator = Math.max(0, (msOperators - msInteraction) / (n * r));
    part = Math.max(0, (msParts - msInteraction) / (k * r));
  }
  const reproducibility = operator + interaction;
  const grrVar = repeatability + reproducibility;
  const total = grrVar + part;
  const tv = multiplier * Math.sqrt(total);
  const grr = multiplier * Math.sqrt(grrVar);
  const pv = multiplier * Math.sqrt(part);
  const ndc = ndcOf(pv, grr);
  return {
    fullTable, pooledTable, interactionPooled: pooled,
    variance: { repeatability, operator, interaction, reproducibility, grr: grrVar, part, total },
    ev: component(multiplier * Math.sqrt(repeatability), tv, tolerance),
    av: component(multiplier * Math.sqrt(reproducibility), tv, tolerance),
    operator: component(multiplier * Math.sqrt(operator), tv, tolerance),
    interaction: component(multiplier * Math.sqrt(interaction), tv, tolerance),
    grr: component(grr, tv, tolerance),
    pv: component(pv, tv, tolerance),
    tv: component(tv, tv, tolerance),
    ndc, ndcSufficient: ndc >= NDC_SUFFICIENT,
    decision: decisionOf(tv > 0 ? grr / tv : 0)
  };
}

function row(source: AnovaRow['source'], df: number, ss: number, ms: number, denominator: number,
             dfDenominator: number): AnovaRow {
  const f = ratio(ms, denominator);
  return { source, degreesOfFreedom: df, sumOfSquares: ss, meanSquare: ms, f,
    p: f === null ? null : fSurvival(f, df, dfDenominator) };
}

function ratio(numerator: number, denominator: number): number | null {
  return denominator > 0 ? numerator / denominator : null;
}

// ---------- loi F ----------

const LANCZOS = [
  0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
  -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
  1.5056327351493116e-7
];

/** ln Γ(z), z > 0 (Lanczos, g = 7 ; formule des compléments sous 1/2). */
export function lnGamma(z: number): number {
  if (z < 0.5) return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * z))) - lnGamma(1 - z);
  const zz = z - 1;
  let s = LANCZOS[0];
  for (let i = 1; i < LANCZOS.length; i++) s += LANCZOS[i] / (zz + i);
  const t = zz + 7.5;
  return 0.5 * Math.log(2 * Math.PI) + (zz + 0.5) * Math.log(t) - t + Math.log(s);
}

function continuedFraction(x: number, a: number, b: number): number {
  const tiny = 1e-300;
  const guard = (v: number) => (Math.abs(v) < tiny ? tiny : v);
  const qab = a + b;
  const qap = a + 1;
  const qam = a - 1;
  let c = 1;
  let d = 1 / guard(1 - qab * x / qap);
  let h = d;
  let converge = false;
  for (let m = 1; m <= 500 && !converge; m++) {
    const m2 = 2 * m;
    let aa = m * (b - m) * x / ((qam + m2) * (a + m2));
    d = 1 / guard(1 + aa * d);
    c = guard(1 + aa / c);
    h *= d * c;
    aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
    d = 1 / guard(1 + aa * d);
    c = guard(1 + aa / c);
    const del = d * c;
    h *= del;
    converge = Math.abs(del - 1) < 1e-15;
  }
  return h;
}

/** I_x(a, b), la bêta incomplète régularisée. */
export function regularizedBeta(x: number, a: number, b: number): number {
  if (x <= 0) return 0;
  if (x >= 1) return 1;
  const front = Math.exp(lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a * Math.log(x) + b * Math.log1p(-x));
  return x < (a + 1) / (a + b + 2)
    ? front * continuedFraction(x, a, b) / a
    : 1 - front * continuedFraction(1 - x, b, a) / b;
}

/** P(F > f) pour une loi F à (d1, d2) degrés de liberté. */
export function fSurvival(f: number, d1: number, d2: number): number {
  if (f <= 0) return 1;
  if (!Number.isFinite(f)) return 0;
  return regularizedBeta(d2 / (d2 + d1 * f), d2 / 2, d1 / 2);
}

// ---------- saisie ----------

/** Une cellule saisie : virgule ou point décimal, espaces tolérés ; `null` si vide ou illisible. */
export function parseMeasurement(text: string): number | null {
  const t = text.trim().replace(/\s/g, '').replace(',', '.');
  if (!t) return null;
  const v = Number(t);
  return Number.isFinite(v) ? v : null;
}

/**
 * Colle un bloc copié depuis un tableur à partir de la colonne `c0` de la
 * pièce `p0`. Les colonnes se lisent comme la grille affichée : pour chaque
 * opérateur, ses `r` essais à la suite. Ce qui déborde est ignoré.
 */
export function pasteBlock(raw: string[][][], p0: number, c0: number, text: string, k: number, n: number,
                           r: number): string[][][] {
  const next = raw.map(bloc => bloc.map(essais => [...essais]));
  text.replace(/\r/g, '').replace(/\n+$/, '').split('\n').forEach((line, i) =>
    line.split('\t').forEach((cell, j) => {
      const p = p0 + i;
      const col = c0 + j;
      if (p < n && col < k * r) next[Math.floor(col / r)][p][col % r] = cell.trim();
    }));
  return next;
}
