import { computeGageRr, isComplete } from './gage-rr.calc';
import { WORKBOOK_EXAMPLE } from './gage-rr.examples';

/** Le jeu d'exemple est celui de l'onglet « Données » et rend le verdict du classeur. */
describe('gage-rr.examples', () => {
  it('tient dans le plan qu\'il annonce', () => {
    const ex = WORKBOOK_EXAMPLE;
    expect(ex.grid.length).toBe(ex.operatorCount);
    expect(ex.grid.every(b => b.length === ex.partCount)).toBeTrue();
    expect(ex.grid.every(b => b.every(e => e.length === ex.trialCount))).toBeTrue();
    expect(isComplete(ex.grid)).toBeTrue();
    expect(ex.usl).toBeGreaterThan(ex.lsl);
  });

  it('rend %GRR 21,0 % sous conditions et ndc 6', () => {
    const ex = WORKBOOK_EXAMPLE;
    const c = computeGageRr(ex.lsl, ex.usl, ex.sigmaMultiplier, ex.grid)!;
    expect(c.grr.percentOfTotal).toBeCloseTo(0.21, 3);
    expect(c.ndc).toBe(6);
    expect(c.decision).toBe('CONDITIONAL');
  });
});
