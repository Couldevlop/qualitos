import { computeCapability } from './spc-capability.calc';
import { GOLDEN, GOLDEN_GRID, GOLDEN_LSL, GOLDEN_USL } from './spc-capability.fixtures';
import { CENTERED_BELOW_PERCENT, capabilityReadings } from './spc-capability.readings';
import { Measurement } from './spc-capability.types';

describe('capabilityReadings', () => {
  const calc = computeCapability(GOLDEN_LSL, GOLDEN_USL, 5, GOLDEN_GRID);

  it('rend dispersion et centrage du classeur, et dit le procédé capable', () => {
    const r = capabilityReadings(calc, GOLDEN_LSL, GOLDEN_USL, 1.67)!;
    expect(r.dispersionPercent).toBeCloseTo(GOLDEN.dispersionPercent, 2);
    expect(r.sixSigma).toBeCloseTo(27.167, 3);
    expect(r.tolerance).toBe(50);
    expect(r.centeringPercent).toBeCloseTo(GOLDEN.centeringPercent, 2);
    expect(r.centered).toBeFalse();
    expect(r.offset).toBeCloseTo(-0.97, 3);
    expect(r.middle).toBe(500);
    expect(r.diagnostic).toContain('capable');
  });

  it('demande de recentrer quand Cp passe le seuil mais pas Cpk', () => {
    const r = capabilityReadings(calc, GOLDEN_LSL, GOLDEN_USL, 1.8)!;
    expect(r.diagnostic).toContain('recentrer');
    expect(r.diagnostic).toContain('1.80');
  });

  it('demande de réduire la variabilité quand Cp ne passe pas le seuil', () => {
    expect(capabilityReadings(calc, GOLDEN_LSL, GOLDEN_USL, 2)!.diagnostic).toContain('réduire');
  });

  it('lit « centré » sous la tolérance de lecture', () => {
    const centre: Measurement[][] = [
      Array<Measurement>(20).fill(499),
      Array<Measurement>(20).fill(501)
    ];
    const c = computeCapability(475, 525, 2, centre);
    const r = capabilityReadings(c, 475, 525, 1.67)!;
    expect(r.centeringPercent).toBeLessThan(CENTERED_BELOW_PERCENT);
    expect(r.centered).toBeTrue();
  });

  it('ne lit rien tant que le calcul est incomplet', () => {
    expect(capabilityReadings(calc, null, GOLDEN_USL, 1.67)).toBeNull();
    expect(capabilityReadings(calc, GOLDEN_LSL, null, 1.67)).toBeNull();
    expect(capabilityReadings(computeCapability(null, null, 5, GOLDEN_GRID), GOLDEN_LSL, GOLDEN_USL, 1.67)).toBeNull();
  });
});
