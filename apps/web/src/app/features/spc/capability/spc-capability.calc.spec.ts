import {
  DEFAULT_SUBGROUPS, MAX_SUBGROUPS, MAX_SUBGROUP_SIZE, RANGE_CHART_MAX, SAMPLE_SIZES,
  STDEV_CONSTANT_ROWS, SUBGROUP_COUNTS, c4, chartFactors, classify, computeCapability, dispersionOf,
  displayDigits, emptyGrid, gammaHalf, measurementDecimals, parseMeasurement, pasteBlock, resizeGrid,
  stdevConstants
} from './spc-capability.calc';
import { GOLDEN, GOLDEN_GRID, GOLDEN_LSL, GOLDEN_USL } from './spc-capability.fixtures';
import { Measurement } from './spc-capability.types';

/**
 * Le calcul de l'écran doit rendre, formule par formule, ce que rend le
 * classeur de référence et le serveur : c'est le test doré.
 */
describe('spc-capability.calc', () => {

  describe('computeCapability — test doré du classeur', () => {
    const c = computeCapability(GOLDEN_LSL, GOLDEN_USL, 5, GOLDEN_GRID);

    it('compte les 20 sous-groupes complets', () => {
      expect(c.completeSubgroups).toBe(20);
      expect(c.subgroupMeans.length).toBe(DEFAULT_SUBGROUPS);
      expect(c.subgroupMeans[0]).toBeCloseTo(498.6, 6);
      expect(c.subgroupRanges[0]).toBe(7);
    });

    it('rend X̿, R̄, σ', () => {
      expect(c.sumOfMeans!).toBeCloseTo(GOLDEN.sumOfMeans, 6);
      expect(c.grandMean!).toBeCloseTo(GOLDEN.grandMean, 3);
      expect(c.meanRange!).toBeCloseTo(GOLDEN.meanRange, 3);
      expect(c.sigma!).toBeCloseTo(GOLDEN.sigma, 4);
    });

    it('rend Cp, CPU, CPL, Cpk', () => {
      expect(c.cp!).toBeCloseTo(GOLDEN.cp, 3);
      expect(c.cpu!).toBeCloseTo(GOLDEN.cpu, 3);
      expect(c.cpl!).toBeCloseTo(GOLDEN.cpl, 3);
      expect(c.cpk!).toBeCloseTo(GOLDEN.cpk, 3);
    });

    it('rend les limites des cartes X̄ et R', () => {
      expect(c.meanUcl!).toBeCloseTo(GOLDEN.meanUcl, 3);
      expect(c.meanLcl!).toBeCloseTo(GOLDEN.meanLcl, 3);
      expect(c.rangeUcl!).toBeCloseTo(GOLDEN.rangeUcl, 2);
      expect(c.rangeLcl!).toBe(0);
      expect(c.dispersionChart).toBe('RANGE');
      expect([c.sigmaDivisor, c.meanFactor, c.dispersionLower, c.dispersionUpper])
        .toEqual([2.33, 0.58, 0, 2.11]);
    });

    it('rend la dispersion et le centrage', () => {
      expect(c.dispersionPercent!).toBeCloseTo(GOLDEN.dispersionPercent, 2);
      expect(c.centerOffset!).toBeCloseTo(GOLDEN.centerOffset, 3);
      expect(c.centeringPercent!).toBeCloseTo(GOLDEN.centeringPercent, 2);
    });
  });

  it('ignore les sous-groupes incomplets et divise par le nombre de sous-groupes complets', () => {
    const grid = GOLDEN_GRID.map(r => [...r]);
    grid[2][0] = null;
    const c = computeCapability(GOLDEN_LSL, GOLDEN_USL, 5, grid);
    expect(c.completeSubgroups).toBe(19);
    expect(c.subgroupMeans[0]).toBeNull();
    expect(c.subgroupRanges[0]).toBeNull();
    const sum = GOLDEN.sumOfMeans - 498.6;
    expect(c.grandMean!).toBeCloseTo(sum / 19, 6);
  });

  it('ignore une valeur non finie', () => {
    const grid = GOLDEN_GRID.map(r => [...r]);
    grid[0][0] = Number.NaN;
    expect(computeCapability(GOLDEN_LSL, GOLDEN_USL, 5, grid).completeSubgroups).toBe(19);
  });

  it('laisse tout nul sans sous-groupe complet', () => {
    const c = computeCapability(GOLDEN_LSL, GOLDEN_USL, 5, emptyGrid(5));
    expect(c.completeSubgroups).toBe(0);
    expect(c.grandMean).toBeNull();
    expect(c.cpk).toBeNull();
    expect(c.meanUcl).toBeNull();
    expect(c.sigmaDivisor).toBe(2.33);
  });

  it('laisse les indices nuls sans dispersion', () => {
    const plat: Measurement[][] = Array.from({ length: 2 }, () => Array<Measurement>(DEFAULT_SUBGROUPS).fill(500));
    const c = computeCapability(GOLDEN_LSL, GOLDEN_USL, 2, plat);
    expect(c.sigma).toBe(0);
    expect(c.cp).toBeNull();
    expect(c.cpk).toBeNull();
    expect(c.dispersionPercent).toBe(0);
    expect(c.meanUcl).toBe(500);
  });

  it('laisse les indices nuls sans limites', () => {
    const c = computeCapability(null, null, 5, GOLDEN_GRID);
    expect(c.grandMean!).toBeCloseTo(GOLDEN.grandMean, 3);
    expect(c.cp).toBeNull();
    expect(c.cpk).toBeNull();
    expect(c.dispersionPercent).toBeNull();
    expect(c.centerOffset).toBeNull();
    expect(c.centeringPercent).toBeNull();
  });

  it('laisse dispersion et centrage nuls pour une tolérance négative', () => {
    const c = computeCapability(GOLDEN_USL, GOLDEN_LSL, 5, GOLDEN_GRID);
    expect(c.dispersionPercent).toBeNull();
    expect(c.centeringPercent).toBeNull();
  });

  it('propose les tailles d\u2019\u00e9chantillon de 2 \u00e0 25', () => {
    expect(SAMPLE_SIZES.length).toBe(24);
    expect(SAMPLE_SIZES[0]).toBe(2);
    expect(SAMPLE_SIZES[SAMPLE_SIZES.length - 1]).toBe(MAX_SUBGROUP_SIZE);
    expect(MAX_SUBGROUP_SIZE).toBe(25);
    expect(RANGE_CHART_MAX).toBe(10);
  });

  it('propose les sous-groupes un \u00e0 un jusqu\u2019\u00e0 20, puis par paliers jusqu\u2019\u00e0 100', () => {
    expect(SUBGROUP_COUNTS.slice(0, 19)).toEqual([
      2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20
    ]);
    expect(SUBGROUP_COUNTS.slice(19)).toEqual([25, 30, 40, 50, 60, 80, 100]);
    expect(MAX_SUBGROUPS).toBe(100);
  });

  it('choisit la carte X\u0304 \u2013 R jusqu\u2019\u00e0 dix mesures, la carte X\u0304 \u2013 S au-del\u00e0', () => {
    for (let n = 2; n <= RANGE_CHART_MAX; n++) {
      expect(chartFactors(n).chart).toBe('RANGE');
    }
    for (let n = RANGE_CHART_MAX + 1; n <= MAX_SUBGROUP_SIZE; n++) {
      expect(chartFactors(n).chart).toBe('STDEV');
    }
    expect(chartFactors(2)).toEqual({
      n: 2, chart: 'RANGE', sigmaDivisor: 1.13, meanFactor: 1.88,
      dispersionLower: 0, dispersionUpper: 3.27
    });
    expect(chartFactors(10)).toEqual({
      n: 10, chart: 'RANGE', sigmaDivisor: 3.08, meanFactor: 0.31,
      dispersionLower: 0.223, dispersionUpper: 1.78
    });
  });

  it('refuse une taille hors du domaine des deux tables, sans rien calculer', () => {
    expect(() => chartFactors(1)).toThrowError(RangeError);
    expect(() => chartFactors(26)).toThrowError(RangeError);
    expect(() => chartFactors(5.5)).toThrowError(RangeError);
  });

  /**
   * Le banc des constantes de la carte X\u0304 \u2013 S : les valeurs CALCUL\u00c9ES sont
   * confront\u00e9es aux valeurs PUBLI\u00c9ES (Montgomery, « Introduction to Statistical
   * Quality Control », annexe VI ; ASTM STP-15D), imprim\u00e9es \u00e0 trois d\u00e9cimales.
   */
  it('calcule c4, A3, B3 et B4 conform\u00e9ment \u00e0 la table publi\u00e9e', () => {
    const publiee: [number, number, number, number, number][] = [
      [2, 0.7979, 2.659, 0, 3.267],
      [5, 0.94, 1.427, 0, 2.089],
      [10, 0.9727, 0.975, 0.284, 1.716],
      [11, 0.9754, 0.927, 0.321, 1.679],
      [12, 0.9776, 0.886, 0.354, 1.646],
      [15, 0.9823, 0.789, 0.428, 1.572],
      [20, 0.9869, 0.68, 0.51, 1.49],
      [25, 0.9896, 0.606, 0.565, 1.435]
    ];
    for (const [n, c, a3, b3, b4] of publiee) {
      const k = stdevConstants(n);
      expect(k.c4).toBeCloseTo(c, 3);
      expect(k.a3).toBeCloseTo(a3, 3);
      expect(k.b3).toBeCloseTo(b3, 3);
      expect(k.b4).toBeCloseTo(b4, 3);
    }
    expect(STDEV_CONSTANT_ROWS.map(r => r.n)).toEqual([...SAMPLE_SIZES]);
  });

  it('d\u00e9roule \u0393(k / 2) depuis \u0393(1) = 1 et \u0393(1/2) = \u221a\u03c0', () => {
    expect(gammaHalf(1)).toBeCloseTo(Math.sqrt(Math.PI), 12);
    expect(gammaHalf(2)).toBe(1);
    expect(gammaHalf(10)).toBeCloseTo(24, 9);
    expect(gammaHalf(11)).toBeCloseTo(52.34277778, 6);
    expect(c4(2)).toBeCloseTo(0.797884561, 9);
  });

  it('mesure la dispersion par l\u2019\u00e9tendue ou par l\u2019\u00e9cart-type, selon la carte', () => {
    expect(dispersionOf([10, 11, 12, 13, 14], 'RANGE')).toBe(4);
    // Onze mesures de moyenne 20 : \u03a3(x \u2212 x\u0304)\u00b2 = 10, donc s = \u221a(10 / 10) = 1.
    expect(dispersionOf([21, 21, 21, 21, 21, 19, 19, 19, 19, 19, 20], 'STDEV')).toBeCloseTo(1, 12);
    expect(dispersionOf([7, 7, 7], 'RANGE')).toBe(0);
    expect(dispersionOf([7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7], 'STDEV')).toBe(0);
  });

  /**
   * Le chemin dor\u00e9 de la carte X\u0304 \u2013 S, calcul\u00e9 \u00e0 la main (m\u00eames valeurs que
   * le banc serveur `CapabilityCalculationTest#cheminDoreCarteS`) : deux
   * sous-groupes de onze mesures d\u2019\u00e9cart-type exactement 1, X\u0304\u0304 = 21,
   * c4(11) = 0,975350, A3 = 0,927394, B3 = 0,321280, B4 = 1,678720, donc
   * \u03c3 = 1 / c4 = 1,0252729 et Cp = Cpk = 12 / (6 \u03c3) = 1,9507002.
   */
  it('rend, sur la carte X\u0304 \u2013 S, les valeurs calcul\u00e9es \u00e0 la main', () => {
    const ecarts = [1, 1, 1, 1, 1, -1, -1, -1, -1, -1, 0];
    const grille: Measurement[][] = ecarts.map(e => [20 + e, 22 + e]);

    const c = computeCapability(15, 27, 11, grille);

    expect(c.dispersionChart).toBe('STDEV');
    expect(c.completeSubgroups).toBe(2);
    expect(c.subgroupMeans).toEqual([20, 22]);
    expect(c.subgroupRanges[0]!).toBeCloseTo(1, 12);
    expect(c.meanRange!).toBeCloseTo(1, 12);
    expect(c.sigma!).toBeCloseTo(1.0252729, 6);
    expect(c.cp!).toBeCloseTo(1.9507002, 6);
    expect(c.cpk!).toBeCloseTo(1.9507002, 6);
    expect(c.meanUcl!).toBeCloseTo(21.9273942, 6);
    expect(c.meanLcl!).toBeCloseTo(20.0726058, 6);
    expect(c.rangeUcl!).toBeCloseTo(1.6787199, 6);
    expect(c.rangeLcl!).toBeCloseTo(0.3212801, 6);
    expect(c.dispersionPercent!).toBeCloseTo(51.2636449, 6);
    expect(c.centerOffset!).toBeCloseTo(0, 12);
  });

  it('calcule cent sous-groupes comme vingt', () => {
    const grille: Measurement[][] = [
      Array<Measurement>(MAX_SUBGROUPS).fill(9),
      Array<Measurement>(MAX_SUBGROUPS).fill(11)
    ];

    const c = computeCapability(5, 15, 2, grille);

    expect(c.completeSubgroups).toBe(100);
    expect(c.grandMean!).toBeCloseTo(10, 12);
    expect(c.meanRange!).toBeCloseTo(2, 12);
    expect(c.sigma!).toBeCloseTo(2 / 1.13, 9);
  });

  it('classe un Cpk au regard des seuils', () => {
    expect(classify(1.67, 1.67, 1.33)).toBe('CAPABLE');
    expect(classify(1.669, 1.67, 1.33)).toBe('WATCH');
    expect(classify(1.33, 1.67, 1.33)).toBe('WATCH');
    expect(classify(1.329, 1.67, 1.33)).toBe('NOT_CAPABLE');
  });

  it('crée une grille vide et la redimensionne sans perdre ce qui reste', () => {
    const g = emptyGrid(3);
    expect(g.length).toBe(3);
    expect(g.every(r => r.length === DEFAULT_SUBGROUPS && r.every(v => v === null))).toBeTrue();
    const r = resizeGrid(GOLDEN_GRID, 3);
    expect(r.length).toBe(3);
    expect(r[2]).toEqual(GOLDEN_GRID[2]);
    const r7 = resizeGrid(GOLDEN_GRID, 7);
    expect(r7[4]).toEqual(GOLDEN_GRID[4]);
    expect(r7[6].every(v => v === null)).toBeTrue();
  });

  it('lit une mesure saisie', () => {
    expect(parseMeasurement('499,5')).toBe(499.5);
    expect(parseMeasurement(' 1 000.25 ')).toBe(1000.25);
    expect(parseMeasurement('-2')).toBe(-2);
    expect(parseMeasurement('')).toBeNull();
    expect(parseMeasurement('   ')).toBeNull();
    expect(parseMeasurement('4a9')).toBeNull();
    expect(parseMeasurement('Infinity')).toBeNull();
  });

  it('colle un bloc de tableur à partir d\'une cellule, sans déborder', () => {
    const raw = Array.from({ length: 2 }, () => Array<string>(DEFAULT_SUBGROUPS).fill(''));
    const next = pasteBlock(raw, 0, 18, '1\t2\t3\r\n4\t 5 \t6\n\n');
    expect(next[0][18]).toBe('1');
    expect(next[0][19]).toBe('2');
    expect(next[1][18]).toBe('4');
    expect(next[1][19]).toBe('5');
    expect(raw[0][18]).toBe('');
    const deborde = pasteBlock(raw, 1, 0, '7\n8\n9');
    expect(deborde[1][0]).toBe('7');
    expect(deborde.length).toBe(2);
  });

  describe('décimales d’affichage', () => {
    it('suivent la résolution des mesures saisies, plafonnée à 4', () => {
      expect(measurementDecimals([['500', '498'], ['', 'x']])).toBe(0);
      expect(measurementDecimals([['12,05', '12.095'], [' 12.1 ', '']])).toBe(3);
      expect(measurementDecimals([['1.1234567']])).toBe(4);
      expect(measurementDecimals([])).toBe(0);
    });

    it('donnent l’étendue à la résolution et la moyenne une décimale plus fine, au moins deux', () => {
      expect(displayDigits(0)).toEqual({ mean: 2, range: 0 });
      expect(displayDigits(3)).toEqual({ mean: 4, range: 3 });
      expect(displayDigits(4)).toEqual({ mean: 4, range: 4 });
    });
  });
});
