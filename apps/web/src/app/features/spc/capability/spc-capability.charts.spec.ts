import { SUBGROUPS, computeCapability, emptyGrid } from './spc-capability.calc';
import { completeValues, controlChart, distribution } from './spc-capability.charts';
import { GOLDEN_GRID, GOLDEN_LSL, GOLDEN_USL } from './spc-capability.fixtures';

describe('spc-capability.charts', () => {

  describe('controlChart', () => {
    it('ne trace rien sans limites ou sans valeur', () => {
      expect(controlChart([1, 2], null, 1, 0)).toBeNull();
      expect(controlChart([1, 2], 3, null, 0)).toBeNull();
      expect(controlChart([1, 2], 3, 1, null)).toBeNull();
      expect(controlChart([null, null], 3, 1, 0)).toBeNull();
    });

    it('place les points, saute les sous-groupes incomplets et repère les hors limites', () => {
      const g = controlChart([5, null, 12, -1], 10, 5, 0)!;
      expect(g.points.map(p => p.index)).toEqual([0, 2, 3]);
      expect(g.points.map(p => p.out)).toEqual([false, true, true]);
      expect(g.polyline.split(' ').length).toBe(3);
      expect(g.lines.map(l => l.label)).toEqual(['UCL', 'CL', 'LCL']);
      // L'axe vertical est inversé : UCL au-dessus de LCL.
      expect(g.lines[0].y).toBeLessThan(g.lines[2].y);
      expect(g.ticks.length).toBe(5);
      expect(g.points[0].x).toBe(g.left);
      expect(g.xLabels.map(l => l.label)).toEqual([1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 20]);
    });

    it('trace la tolérance sur la carte et l\'inclut dans l\'échelle', () => {
      expect(controlChart([5, 6], 10, 6, 2)!.specs).toEqual([]);
      const g = controlChart([5, 6], 10, 6, 2, { usl: 20, lsl: Number.NaN })!;
      expect(g.specs.map(t => t.label)).toEqual(['USL']);
      expect(g.specs[0].value).toBe(20);
      expect(g.specs[0].y).toBeLessThan(g.lines[0].y);
      expect(g.ticks[4].value).toBeGreaterThan(20);
      const h = controlChart([5, 6], 10, 6, 2, { usl: null, lsl: -4 })!;
      expect(h.specs.map(t => t.label)).toEqual(['LSL']);
      expect(h.ticks[0].value).toBeLessThan(-4);
    });

    it('élargit la marge gauche pour que les graduations ne soient pas rognées', () => {
      const court = controlChart([5, 6], 10, 6, 2, { usl: null, lsl: null }, 2)!;
      const long = controlChart([12.05, 12.06], 12.09, 12.05, 12.02, { usl: 12.1, lsl: 11.9 }, 4)!;
      // « 12.0620 » : sept caractères mono de 10 px, plus l'écart au trait.
      expect(long.left).toBeGreaterThanOrEqual(7 * 6 + 6);
      expect(long.left).toBeGreaterThan(court.left);
      expect(long.points[0].x).toBe(long.left);
      expect(court.left).toBe(44);
    });

    it('garde une échelle quand toutes les valeurs sont égales', () => {
      const g = controlChart([2, 2], 2, 2, 2)!;
      expect(g.points.every(p => Number.isFinite(p.y))).toBeTrue();
      expect(g.points.every(p => !p.out)).toBeTrue();
    });
  });

  describe('completeValues', () => {
    it('ne garde que les mesures des sous-groupes complets', () => {
      expect(completeValues(GOLDEN_GRID, 5).length).toBe(5 * SUBGROUPS);
      const g = GOLDEN_GRID.map(r => [...r]);
      g[1][3] = null;
      expect(completeValues(g, 5).length).toBe(5 * (SUBGROUPS - 1));
      expect(completeValues(emptyGrid(3), 3)).toEqual([]);
    });
  });

  describe('distribution', () => {
    const calc = computeCapability(GOLDEN_LSL, GOLDEN_USL, 5, GOLDEN_GRID);
    const values = completeValues(GOLDEN_GRID, 5);

    it('ne trace rien sans mesure, sans dispersion ou sans tolérance valide', () => {
      expect(distribution([], 500, 4, 475, 525)).toBeNull();
      expect(distribution(values, null, 4, 475, 525)).toBeNull();
      expect(distribution(values, 500, 0, 475, 525)).toBeNull();
      expect(distribution(values, 500, null, 475, 525)).toBeNull();
      expect(distribution(values, 500, 4, null, 525)).toBeNull();
      expect(distribution(values, 500, 4, 525, 475)).toBeNull();
    });

    it('répartit toutes les mesures dans les classes et place la tolérance', () => {
      const g = distribution(values, calc.grandMean, calc.sigma, GOLDEN_LSL, GOLDEN_USL)!;
      expect(g.bars.reduce((s, b) => s + b.count, 0)).toBe(values.length);
      expect(g.bars.every(b => !b.out)).toBeTrue();
      expect(g.lslX).toBeLessThan(g.meanX);
      expect(g.meanX).toBeLessThan(g.uslX);
      expect(g.lowZone.x).toBe(g.left);
      expect(g.highZone.x).toBe(g.uslX);
      expect(g.curve.split(' ').length).toBe(121);
    });

    it('marque les classes hors tolérance', () => {
      const g = distribution([470, 500, 500, 530], 500, 10, 475, 525)!;
      expect(g.bars.filter(b => b.out).length).toBe(2);
    });
  });
});
