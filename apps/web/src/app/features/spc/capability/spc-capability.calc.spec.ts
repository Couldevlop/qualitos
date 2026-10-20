import {
  SUBGROUPS, SUBGROUP_SIZES, classify, computeCapability, constantsFor, emptyGrid, parseMeasurement,
  pasteBlock, resizeGrid
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
      expect(c.subgroupMeans.length).toBe(SUBGROUPS);
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
      expect([c.d2, c.a2, c.d3, c.d4]).toEqual([2.33, 0.58, 0, 2.11]);
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
    expect(c.d2).toBe(2.33);
  });

  it('laisse les indices nuls sans dispersion', () => {
    const plat: Measurement[][] = Array.from({ length: 2 }, () => Array<Measurement>(SUBGROUPS).fill(500));
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

  it('expose les constantes de n = 2 à 10, et refuse les autres tailles', () => {
    expect(SUBGROUP_SIZES).toEqual([2, 3, 4, 5, 6, 7, 8, 9, 10]);
    expect(constantsFor(2)).toEqual({ d2: 1.13, a2: 1.88, d3: 0, d4: 3.27 });
    expect(constantsFor(10)).toEqual({ d2: 3.08, a2: 0.31, d3: 0.223, d4: 1.78 });
    expect(() => constantsFor(1)).toThrowError(RangeError);
    expect(() => constantsFor(11)).toThrowError(RangeError);
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
    expect(g.every(r => r.length === SUBGROUPS && r.every(v => v === null))).toBeTrue();
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
    const raw = Array.from({ length: 2 }, () => Array<string>(SUBGROUPS).fill(''));
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
});
