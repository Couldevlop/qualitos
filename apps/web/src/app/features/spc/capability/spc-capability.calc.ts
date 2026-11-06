import { CalculationView, CapabilityStatus, DispersionChart, Measurement } from './spc-capability.types';

/**
 * Les sous-groupes d'une étude : les colonnes « SG 01 », « SG 02 »… de la grille.
 *
 * <p>Le classeur de référence en comptait vingt, et vingt était devenu une
 * contrainte : une étude s'ouvrait avec vingt colonnes, ni plus ni moins, et le
 * serveur refusait tout le reste. Or la règle de l'art — vingt à vingt-cinq
 * sous-groupes pour asseoir des limites de contrôle — est un PLANCHER de
 * confiance, pas une taille imposée : on relève souvent douze sous-groupes
 * pendant la collecte, et trente sur une campagne longue. Le plancher reste
 * donc dit à l'écran ({@link RELIABLE_SUBGROUPS}) au lieu d'être imposé.
 *
 * <p>Le plafond n'a rien de statistique : plus de relevés ne dégrade aucune
 * formule, au contraire. Il ne borne que la largeur de la grille affichable et
 * la taille du corps de requête.
 */
export const DEFAULT_SUBGROUPS = 20;
export const MIN_SUBGROUPS = 2;
export const MAX_SUBGROUPS = 100;

/**
 * Les nombres de sous-groupes proposés dans la liste déroulante.
 *
 * <p>Un par un jusqu'à vingt — le plancher de la règle de l'art, donc la zone
 * où choisir exactement son nombre a un sens — puis par paliers jusqu'à cent,
 * pour qu'une campagne longue se règle d'un clic au lieu de quatre-vingts. Un
 * nombre intermédiaire reste atteignable : un bloc collé depuis un tableur
 * apporte sa largeur, et l'écran l'ajoute alors à la liste.
 */
export const SUBGROUP_COUNTS: readonly number[] = [
  ...Array.from({ length: 19 }, (_, i) => i + MIN_SUBGROUPS), 25, 30, 40, 50, 60, 80, 100
];

/** En dessous, les limites de contrôle restent indicatives : on le dit, on n'interdit pas. */
export const RELIABLE_SUBGROUPS = 20;
export const MIN_SUBGROUP_SIZE = 2;

/** La dernière taille d'échantillon où l'étendue reste un estimateur recommandé de σ. */
export const RANGE_CHART_MAX = 10;

/** La dernière taille d'échantillon tabulée, cartes X̄ – R et X̄ – S confondues. */
export const MAX_SUBGROUP_SIZE = 25;

export interface RangeConstants { d2: number; a2: number; d3: number; d4: number }
export interface StdevConstantsRow { c4: number; a3: number; b3: number; b4: number }

/**
 * Les facteurs de la carte retenue, sous un nom neutre : la même structure que
 * `ChartFactors` côté serveur, pour que les formules de capabilité ne soient
 * écrites qu'une fois, quelle que soit la carte.
 */
export interface ChartFactors {
  n: number;
  chart: DispersionChart;
  /** d2 (carte R) ou c4 (carte S) : σ = dispersion moyenne / ce diviseur. */
  sigmaDivisor: number;
  /** A2 (carte R) ou A3 (carte S) : limites X̿ ± facteur × dispersion moyenne. */
  meanFactor: number;
  /** D3 (carte R) ou B3 (carte S). */
  dispersionLower: number;
  /** D4 (carte R) ou B4 (carte S). */
  dispersionUpper: number;
}

/**
 * Les constantes de la carte X̄ – R. Données brutes de tables statistiques :
 * n = 2 à 5 comme le classeur de référence, les tables usuelles au-delà. La
 * même table que le serveur (`SpcConstants`) — c'est ce qui fait que le Cpk
 * affiché pendant la saisie est celui qui sera enregistré.
 *
 * <p>Elle s'arrête à dix parce que l'étendue n'utilise que deux des n mesures :
 * au-delà, elle estime σ trop mal pour qu'une table soit publiée. Un échantillon
 * plus large se conduit sur la carte X̄ – S ({@link stdevConstants}).
 */
const CONSTANTS: Record<number, RangeConstants> = {
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

/**
 * Γ(k / 2) pour un entier k ≥ 1, par récurrence exacte depuis Γ(1) = 1 et
 * Γ(1/2) = √π. Aucune bibliothèque : n est entier, donc n/2 est entier ou
 * demi-entier.
 */
export function gammaHalf(k: number): number {
  const entier = k % 2 === 0;
  let valeur = entier ? 1 : Math.sqrt(Math.PI);
  const cible = k / 2;
  for (let x = entier ? 1 : 0.5; x < cible; x += 1) valeur *= x;
  return valeur;
}

/** c4(n) = √(2 / (n − 1)) × Γ(n / 2) / Γ((n − 1) / 2). */
export function c4(n: number): number {
  return Math.sqrt(2 / (n - 1)) * gammaHalf(n) / gammaHalf(n - 1);
}

/**
 * Les constantes de la carte X̄ – S, CALCULÉES et non recopiées : c4, A3, B3 et
 * B4 ont une définition fermée exacte, et les tables publiées (Montgomery,
 * <i>Introduction to Statistical Quality Control</i>, annexe VI ; ASTM STP-15D)
 * n'en sont que l'impression arrondie. Les calculer supprime le risque de
 * transcription sur quinze lignes — et c'est la même formule que le serveur
 * (`StdevConstants`), donc le même Cpk de part et d'autre.
 */
export function stdevConstants(n: number): StdevConstantsRow {
  const c = c4(n);
  const spread = 3 * Math.sqrt(1 - c * c) / c;
  return { c4: c, a3: 3 / (c * Math.sqrt(n)), b3: Math.max(0, 1 - spread), b4: 1 + spread };
}

/** Les tailles d'échantillon admises : 2 à 25, les deux cartes réunies. */
export const SAMPLE_SIZES: readonly number[] =
  Array.from({ length: MAX_SUBGROUP_SIZE - MIN_SUBGROUP_SIZE + 1 }, (_, i) => i + MIN_SUBGROUP_SIZE);

/** La table de la carte X̄ – R, telle que le classeur l'affiche à droite de la grille. */
export const RANGE_CONSTANT_ROWS: readonly ({ n: number } & RangeConstants)[] =
  Object.keys(CONSTANTS).map(Number).map(n => ({ n, ...CONSTANTS[n] }));

/** La table de la carte X̄ – S, affichée à la place de la précédente quand elle s'applique. */
export const STDEV_CONSTANT_ROWS: readonly ({ n: number } & StdevConstantsRow)[] =
  SAMPLE_SIZES.map(n => ({ n, ...stdevConstants(n) }));

/**
 * La carte qui convient à une taille d'échantillon.
 *
 * <p>C'est le seul endroit qui tranche, et il n'improvise pas : hors du domaine
 * des deux tables, il lève — mieux vaut un écran qui dit « pas calculable »
 * qu'un Cpk tiré d'un d2 qui n'existe pas.
 */
export function chartFactors(n: number): ChartFactors {
  if (!Number.isInteger(n) || n < MIN_SUBGROUP_SIZE || n > MAX_SUBGROUP_SIZE) {
    throw new RangeError(`sample size ${n}`);
  }
  if (n <= RANGE_CHART_MAX) {
    const k = CONSTANTS[n];
    return {
      n, chart: 'RANGE', sigmaDivisor: k.d2, meanFactor: k.a2,
      dispersionLower: k.d3, dispersionUpper: k.d4
    };
  }
  const k = stdevConstants(n);
  return {
    n, chart: 'STDEV', sigmaDivisor: k.c4, meanFactor: k.a3,
    dispersionLower: k.b3, dispersionUpper: k.b4
  };
}

/**
 * La dispersion d'un sous-groupe complet, dans l'unité de la carte : l'étendue
 * max − min (carte R), ou l'écart-type d'échantillon s, diviseur n − 1 (carte S).
 */
export function dispersionOf(values: readonly number[], chart: DispersionChart): number {
  if (chart === 'RANGE') return Math.max(...values) - Math.min(...values);
  const moyenne = values.reduce((a, b) => a + b, 0) / values.length;
  const carres = values.reduce((a, v) => a + (v - moyenne) ** 2, 0);
  return Math.sqrt(carres / (values.length - 1));
}

/**
 * Le calcul d'une étude, formule par formule, comme le serveur
 * (`CapabilityCalculation`) : seuls les sous-groupes complets comptent, et
 * les indices restent nuls sans sous-groupe complet ou sans dispersion.
 *
 * <p>`subgroupRanges` et `meanRange` portent la dispersion de la carte retenue :
 * l'étendue sur une carte R, l'écart-type d'échantillon sur une carte S. Les
 * noms sont ceux du contrat d'origine ; `chart` dit lequel des deux.
 */
export function computeCapability(lsl: number | null, usl: number | null, n: number,
                                  grid: Measurement[][]): CalculationView {
  const k = chartFactors(n);
  const means: (number | null)[] = [];
  const ranges: (number | null)[] = [];
  let sumMeans = 0;
  let sumRanges = 0;
  let m = 0;
  // La largeur est celle de la grille reçue : c'est l'étude qui dit combien de
  // sous-groupes elle porte, pas une constante.
  const columns = grid[0]?.length ?? 0;
  for (let c = 0; c < columns; c++) {
    const col: number[] = [];
    for (let r = 0; r < n; r++) {
      const v = grid[r]?.[c];
      if (v !== null && v !== undefined && Number.isFinite(v)) col.push(v);
    }
    if (col.length === n) {
      const mean = col.reduce((a, b) => a + b, 0) / n;
      const range = dispersionOf(col, k.chart);
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
    dispersionChart: k.chart, sigmaDivisor: k.sigmaDivisor, meanFactor: k.meanFactor,
    dispersionLower: k.dispersionLower, dispersionUpper: k.dispersionUpper,
    dispersionPercent: null, centerOffset: null, centeringPercent: null
  };
  if (m === 0) return base;

  const xbb = sumMeans / m;
  const rb = sumRanges / m;
  const sigma = rb / k.sigmaDivisor;
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
    meanUcl: xbb + k.meanFactor * rb, meanLcl: xbb - k.meanFactor * rb,
    rangeUcl: k.dispersionUpper * rb, rangeLcl: k.dispersionLower * rb,
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

/** Une grille vide de `n` lignes sur `columns` sous-groupes. */
export function emptyGrid(n: number, columns: number = DEFAULT_SUBGROUPS): Measurement[][] {
  return Array.from({ length: n }, () => Array<Measurement>(columns).fill(null));
}

/**
 * La grille à `n` lignes : les lignes en trop sont retirées, les manquantes
 * ajoutées vides. Changer la taille d'échantillon ne perd pas ce qui reste.
 */
export function resizeGrid(grid: Measurement[][], n: number,
                           columns: number = grid[0]?.length ?? DEFAULT_SUBGROUPS): Measurement[][] {
  return Array.from({ length: n }, (_, r) =>
    Array.from({ length: columns }, (_, c) => grid[r]?.[c] ?? null));
}

/** Lit une cellule saisie : virgule ou point décimal ; vide ou illisible vaut `null`. */
export function parseMeasurement(text: string): Measurement {
  const t = text.trim().replace(/\s/g, '').replace(',', '.');
  if (!t) return null;
  const v = Number(t);
  return Number.isFinite(v) ? v : null;
}

/** Au-delà, l'affichage n'apporte plus rien qu'un instrument de terrain puisse garantir. */
const MAX_DISPLAY_DECIMALS = 4;

/**
 * Le nombre de décimales des mesures saisies — la résolution de l'instrument,
 * plafonnée à 4. Un diamètre relevé au millième ne s'affiche pas comme un poids
 * en grammes : une étendue de 0,045 mm arrondie à l'unité devenait « 0 ».
 */
export function measurementDecimals(cells: readonly (readonly string[])[]): number {
  let decimals = 0;
  for (const row of cells) {
    for (const text of row) {
      if (parseMeasurement(text) === null) continue;
      const t = text.trim().replace(/\s/g, '').replace(',', '.');
      const point = t.indexOf('.');
      if (point >= 0) decimals = Math.max(decimals, t.length - point - 1);
    }
  }
  return Math.min(decimals, MAX_DISPLAY_DECIMALS);
}

/**
 * Les décimales d'affichage déduites de la résolution : l'étendue à la résolution
 * des mesures, une moyenne une décimale plus fine (au moins deux, l'affichage
 * d'origine pour des mesures entières).
 */
export function displayDigits(decimals: number): { mean: number; range: number } {
  return {
    mean: Math.min(MAX_DISPLAY_DECIMALS, Math.max(2, decimals + 1)),
    range: Math.min(MAX_DISPLAY_DECIMALS, decimals)
  };
}

/**
 * Colle un bloc copié depuis un tableur (tabulations, retours à la ligne) à
 * partir de la cellule (r0, c0), dans la grille de texte saisi. Ce qui déborde
 * de la grille est ignoré ; chaque cellule se relit ensuite par `parseMeasurement`.
 */
export function pasteBlock(grid: string[][], r0: number, c0: number, text: string): string[][] {
  // Un bloc collé depuis un tableur apporte SA largeur. L'ignorer silencieusement
  // perdait des colonnes entières ; on étend donc la grille jusqu'à ce qu'elle
  // tienne le bloc, dans la limite admise.
  const lines = text.replace(/\r/g, '').replace(/\n+$/, '').split('\n');
  const widest = lines.reduce((w, line) => Math.max(w, line.split('\t').length), 0);
  const wanted = Math.min(MAX_SUBGROUPS, Math.max(grid[0]?.length ?? 0, c0 + widest));
  const next = grid.map(row => Array.from({ length: wanted }, (_, c) => row[c] ?? ''));
  lines.forEach((line, i) =>
    line.split('\t').forEach((cell, j) => {
      const r = r0 + i;
      const c = c0 + j;
      if (r < next.length && c < (next[r]?.length ?? 0)) next[r][c] = cell.trim();
    }));
  return next;
}
