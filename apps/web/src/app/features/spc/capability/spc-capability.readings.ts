import { CalculationView } from './spc-capability.types';

/**
 * En deçà de cet écart (en % de la demi-tolérance), la moyenne se lit
 * « centrée ». C'est une tolérance de lecture, pas une règle métier : le
 * verdict de l'étude ne dépend que du Cpk et des seuils du client.
 */
export const CENTERED_BELOW_PERCENT = 2;

export interface CapabilityReadings {
  /** Dispersion = 6σ / (USL − LSL) × 100. */
  dispersionPercent: number;
  sixSigma: number;
  tolerance: number;
  /** Centrage = |X̿ − milieu| / (tolérance / 2) × 100. */
  centeringPercent: number;
  centered: boolean;
  offset: number;
  middle: number;
  diagnostic: string;
}

/**
 * Les lectures sous la distribution : dispersion, centrage, et le diagnostic
 * — capable, à recentrer, ou à rendre moins variable — au seuil du client.
 * `null` tant que le calcul n'est pas complet.
 */
export function capabilityReadings(calc: CalculationView, lsl: number | null, usl: number | null,
                                   capable: number): CapabilityReadings | null {
  if (lsl === null || usl === null || calc.sigma === null || calc.cp === null || calc.cpk === null
    || calc.dispersionPercent === null || calc.centeringPercent === null || calc.centerOffset === null) {
    return null;
  }
  const seuil = capable.toFixed(2);
  const diagnostic = calc.cpk >= capable
    ? $localize`:@@spc.reading.capable:Le procédé est capable : la dispersion tient largement dans la tolérance et la moyenne est bien placée.`
    : calc.cp >= capable
      ? $localize`:@@spc.reading.recenter:La dispersion est suffisante, mais la moyenne est trop décalée : recentrer le procédé suffit à atteindre Cpk ${seuil}:threshold:.`
      : $localize`:@@spc.reading.reduce:La dispersion est trop grande pour la tolérance : il faut réduire la variabilité, puis vérifier le centrage.`;
  return {
    dispersionPercent: calc.dispersionPercent,
    sixSigma: 6 * calc.sigma,
    tolerance: usl - lsl,
    centeringPercent: calc.centeringPercent,
    centered: calc.centeringPercent < CENTERED_BELOW_PERCENT,
    offset: calc.centerOffset,
    middle: (usl + lsl) / 2,
    diagnostic
  };
}
