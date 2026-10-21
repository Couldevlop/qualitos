import { CalculationView, CapabilityStatus, Measurement } from './spc-capability.types';

/** Les sous-groupes d'une étude : colonnes « SG 01 » à « SG 20 » du classeur. */
export const SUBGROUPS = 20;
export const MIN_SUBGROUP_SIZE = 2;
export const MAX_SUBGROUP_SIZE = 10;

interface Constants { d2: number; a2: number; d3: number; d4: number }

/**
 * Les constantes des cartes X̄ – R. Données brutes de tables statistiques :
 * n = 2 à 5 comme le classeur de référence, les tables usuelles au-delà. La
 * même table que le serveur (`SpcConstants`) — c'est ce qui fait que le Cpk
 * affiché pendant la saisie est celui qui sera enregistré.
 */
const CONSTANTS: Record<number, Constants> = {
  2: { d2: 1.13, a2: 1.88, d3: 0, d4: 3.27 },
  3: { d2: 1.69, a2: 1.02, d3: 0, d4: 2.57 },
  4: { d2: 2.06, a2: 0.73, d3: 0, d4: 2.29 },
  5: { d2: 2.33, a2: 0.58, d3: 0, d4: 2.11 },
  6: { d2: 2.53, a2: 0.48, d3: 0, d4: 2.0 },
  7: { d2: 2.7, a2: 0.42, d3: 0.076, d4: 1.92 },
  8: { d2: 2.85, a2: 0.37, d3: 0.136, d4: 1.86 },
  9: { d2: 2.97, a2: 0.34, d3: 0.184, d4: 1.82 },
  10: { d2: 3.08, a2: 0.31, d3: 0.223, d4: 1.78 }
};

export const SUBGROUP_SIZES = Object.keys(CONSTANTS).map(Number);

/** La table des constantes, telle que le classeur l'affiche à droite de la grille. */
export const CONSTANT_ROWS: readonly ({ n: number } & Constants)[] =
  SUBGROUP_SIZES.map(n => ({ n, ...CONSTANTS[n] }));

export function constantsFor(n: number): Constants {
  const k = CONSTANTS[n];
  if (!k) throw new RangeError(`subgroup size ${n}`);
  return k;
}

/**
 * Le calcul d'une étude, formule par formule, comme le serveur
 * (`CapabilityCalculation`) : seuls les sous-groupes complets comptent, et
 * les indices restent nuls sans sous-groupe complet ou sans dispersion.
 */
export function computeCapability(lsl: number | null, usl: number | null, n: number,
                                  grid: Measurement[][]): CalculationView {
  const k = constantsFor(n);
  const means: (number | null)[] = [];
  const ranges: (number | null)[] = [];
  let sumMeans = 0;
  let sumRanges = 0;
  let m = 0;
  for (let c = 0; c < SUBGROUPS; c++) {
    const col: number[] = [];
    for (let r = 0; r < n; r++) {
      const v = grid[r]?.[c];
      if (v !== null && v !== undefined && Number.isFinite(v)) col.push(v);
    }
    if (col.length === n) {
      const mean = col.reduce((a, b) => a + b, 0) / n;
      const range = Math.max(...col) - Math.min(...col);
      means.push(mean);
      ranges.push(range);
      sumMeans += mean;
      sumRanges += range;
      m++;
    } else {
      means.push(null);
      ranges.push(null);
    }
  }
  const base: CalculationView = {
    subgroupMeans: means, subgroupRanges: ranges, completeSubgroups: m,
    sumOfMeans: null, grandMean: null, meanRange: null, sigma: null,
    cp: null, cpu: null, cpl: null, cpk: null,
    meanUcl: null, meanLcl: null, rangeUcl: null, rangeLcl: null,
    d2: k.d2, a2: k.a2, d3: k.d3, d4: k.d4,
    dispersionPercent: null, centerOffset: null, centeringPercent: null
  };
  if (m === 0) return base;

  const xbb = sumMeans / m;
  const rb = sumRanges / m;
  const sigma = rb / k.d2;
  const limits = lsl !== null && usl !== null && Number.isFinite(lsl) && Number.isFinite(usl);
  const tol = limits ? (usl as number) - (lsl as number) : NaN;
  const spread = sigma > 0;
  const cpu = spread && limits ? ((usl as number) - xbb) / (3 * sigma) : null;
  const cpl = spread && limits ? (xbb - (lsl as number)) / (3 * sigma) : null;
  const offset = limits ? xbb - ((usl as number) + (lsl as number)) / 2 : null;
  return {
    ...base,
    sumOfMeans: sumMeans, grandMean: xbb, meanRange: rb, sigma,
    cp: spread && limits ? tol / (6 * sigma) : null,
    cpu, cpl,
    cpk: cpu !== null && cpl !== null ? Math.min(cpu, cpl) : null,
    meanUcl: xbb + k.a2 * rb, meanLcl: xbb - k.a2 * rb,
    rangeUcl: k.d4 * rb, rangeLcl: k.d3 * rb,
    dispersionPercent: limits && tol > 0 ? 6 * sigma / tol * 100 : null,
    centerOffset: offset,
    centeringPercent: offset !== null && tol > 0 ? Math.abs(offset) / (tol / 2) * 100 : null
  };
}

/** Le rang d'un Cpk au regard des seuils : la pastille du registre. */
export function classify(cpk: number, capable: number, watch: number): CapabilityStatus {
  if (cpk >= capable) return 'CAPABLE';
  return cpk >= watch ? 'WATCH' : 'NOT_CAPABLE';
}

/** Une grille vide de `n` lignes. */
export function emptyGrid(n: number): Measurement[][] {
  return Array.from({ length: n }, () => Array<Measurement>(SUBGROUPS).fill(null));
}

/**
 * La grille à `n` lignes : les lignes en trop sont retirées, les manquantes
 * ajoutées vides. Changer la taille du sous-groupe ne perd pas ce qui reste.
 */
export function resizeGrid(grid: Measurement[][], n: number): Measurement[][] {
  return Array.from({ length: n }, (_, r) =>
    Array.from({ length: SUBGROUPS }, (_, c) => grid[r]?.[c] ?? null));
}

/** Lit une cellule saisie : virgule ou point décimal ; vide ou illisible vaut `null`. */
export function parseMeasurement(text: string): Measurement {
  const t = text.trim().replace(/\s/g, '').replace(',', '.');
  if (!t) return null;
  const v = Number(t);
  return Number.isFinite(v) ? v : null;
}

/**
 * Colle un bloc copié depuis un tableur (tabulations, retours à la ligne) à
 * partir de la cellule (r0, c0), dans la grille de texte saisi. Ce qui déborde
 * de la grille est ignoré ; chaque cellule se relit ensuite par `parseMeasurement`.
 */
export function pasteBlock(grid: string[][], r0: number, c0: number, text: string): string[][] {
  const next = grid.map(row => [...row]);
  text.replace(/\r/g, '').replace(/\n+$/, '').split('\n').forEach((line, i) =>
    line.split('\t').forEach((cell, j) => {
      const r = r0 + i;
      const c = c0 + j;
      if (r < next.length && c < SUBGROUPS) next[r][c] = cell.trim();
    }));
  return next;
}
