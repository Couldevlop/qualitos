import { CONSTANT_ROWS, SUBGROUPS, SUBGROUP_SIZES, classify, computeCapability, resizeGrid } from './spc-capability.calc';
import { SPC_EXAMPLES, TURNING_EXAMPLE, WORKBOOK_EXAMPLE } from './spc-capability.examples';
import { GOLDEN, GOLDEN_GRID, GOLDEN_LSL, GOLDEN_USL } from './spc-capability.fixtures';

/**
 * Les jeux d'exemple : le premier rend le calcul doré du classeur, le second un
 * procédé décentré, non maîtrisé, avec un sous-groupe hors limites.
 */
describe('spc-capability.examples', () => {
  const calc = (ex: typeof WORKBOOK_EXAMPLE) =>
    computeCapability(ex.lsl, ex.usl, ex.subgroupSize, resizeGrid(ex.grid, ex.subgroupSize));

  it('a deux exemples complets, de forme valide', () => {
    expect(SPC_EXAMPLES.map(e => e.id)).toEqual(['workbook', 'turning']);
    for (const ex of SPC_EXAMPLES) {
      expect(SUBGROUP_SIZES).toContain(ex.subgroupSize);
      expect(ex.grid.length).toBe(ex.subgroupSize);
      expect(ex.grid.every(r => r.length === SUBGROUPS && r.every(v => Number.isFinite(v)))).toBeTrue();
      expect(ex.usl).toBeGreaterThan(ex.lsl);
      expect(ex.partReference.trim()).not.toBe('');
    }
  });

  it('reprend la grille et les limites du classeur', () => {
    expect(WORKBOOK_EXAMPLE.grid).toEqual(GOLDEN_GRID);
    expect([WORKBOOK_EXAMPLE.lsl, WORKBOOK_EXAMPLE.usl]).toEqual([GOLDEN_LSL, GOLDEN_USL]);
    const c = calc(WORKBOOK_EXAMPLE);
    expect(c.cp!).toBeCloseTo(GOLDEN.cp, 3);
    expect(c.cpk!).toBeCloseTo(GOLDEN.cpk, 3);
    expect(classify(c.cpk!, 1.67, 1.33)).toBe('CAPABLE');
  });

  it('donne un procédé décentré, non maîtrisé, au sous-groupe 14 hors limites', () => {
    const c = calc(TURNING_EXAMPLE);
    expect(c.completeSubgroups).toBe(SUBGROUPS);
    expect(c.cp!).toBeCloseTo(1.42, 2);
    expect(c.cpk!).toBeCloseTo(0.66, 2);
    expect(c.cpu!).toBeLessThan(c.cpl!);
    expect(classify(c.cpk!, 1.67, 1.33)).toBe('NOT_CAPABLE');
    const hors = c.subgroupMeans
      .map((m, i) => (m! > c.meanUcl! || m! < c.meanLcl! ? i + 1 : 0))
      .filter(i => i > 0);
    expect(hors).toContain(14);
  });

  it('expose la table des constantes ligne à ligne', () => {
    expect(CONSTANT_ROWS.map(r => r.n)).toEqual(SUBGROUP_SIZES);
    expect(CONSTANT_ROWS.find(r => r.n === 5)).toEqual({ n: 5, d2: 2.33, a2: 0.58, d3: 0, d4: 2.11 });
  });
});
