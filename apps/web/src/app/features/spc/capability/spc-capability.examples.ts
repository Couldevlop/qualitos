import { Measurement } from './spc-capability.types';

/**
 * Les jeux d'exemple de la fiche d'étude : on les charge pour découvrir
 * l'écran, rien n'est enregistré avant « Enregistrer l'étude ».
 */
export interface SpcExample {
  id: 'workbook' | 'turning';
  characteristic: string;
  specification: string;
  partReference: string;
  lsl: number;
  usl: number;
  subgroupSize: number;
  grid: Measurement[][];
}

/**
 * Exemple 1 — la grille du classeur de référence (« SPC Cp Cpk Study
 * Template.xlsx ») : poids de l'arbre Shaft AX, 500 ± 25 g, n = 5. Procédé
 * maîtrisé : Cp 1,84, Cpk 1,77.
 */
export const WORKBOOK_EXAMPLE: SpcExample = {
  id: 'workbook',
  characteristic: 'Weight',
  specification: '500 ± 25 gram',
  partReference: 'Shaft AX',
  lsl: 475,
  usl: 525,
  subgroupSize: 5,
  grid: [
    [504, 508, 497, 504, 496, 493, 494, 494, 507, 499, 500, 496, 494, 505, 499, 498, 508, 495, 493, 495],
    [497, 505, 508, 497, 498, 507, 498, 502, 501, 507, 504, 505, 494, 495, 495, 498, 506, 497, 499, 497],
    [497, 492, 496, 494, 502, 496, 498, 502, 497, 498, 496, 493, 502, 496, 496, 499, 506, 492, 506, 500],
    [497, 494, 501, 498, 493, 504, 506, 498, 494, 498, 494, 500, 503, 502, 498, 507, 493, 500, 504, 497],
    [498, 492, 496, 500, 494, 507, 497, 502, 500, 499, 500, 499, 494, 502, 500, 500, 503, 502, 498, 492]
  ]
};

/**
 * Exemple 2 — un diamètre usiné décentré : Ø 12,00 ± 0,10 mm, n = 4. La
 * dispersion tient dans la tolérance (Cp 1,42) mais la moyenne dérive vers la
 * USL (Cpk 0,66) : procédé non maîtrisé, sous-groupe 14 hors limites de la
 * carte X̄, mesures au-delà de la USL.
 */
export const TURNING_EXAMPLE: SpcExample = {
  id: 'turning',
  characteristic: 'Diamètre',
  specification: 'Ø 12,00 ± 0,10 mm',
  partReference: 'Axe AX-12',
  lsl: 11.9,
  usl: 12.1,
  subgroupSize: 4,
  grid: [
    [12.07, 12.057, 12.035, 12.053, 12.04, 12.054, 12.044, 12.066, 12.068, 12.061,
      12.049, 12.069, 12.026, 12.094, 12.099, 12.072, 12.043, 12.077, 12.003, 12.08],
    [12.05, 12.039, 12.041, 12.085, 12.023, 12.05, 12.012, 12.009, 12.054, 12.056,
      12.066, 12.089, 12.021, 12.101, 12.02, 12.027, 12.085, 12.041, 12.002, 12.043],
    [12.055, 12.041, 12.055, 12.047, 12.091, 12.067, 12.078, 12.071, 12.044, 12.023,
      12.083, 12.061, 12.028, 12.084, 12.024, 12.042, 12.07, 12.049, 12.031, 12.102],
    [12.095, 12.051, 12.083, 12.042, 12.111, 12.01, 12.044, 12.016, 12.059, 12.02,
      12.027, 12.071, 11.998, 12.115, 12.082, 12.066, 12.028, 12.047, 12.045, 12.033]
  ]
};

export const SPC_EXAMPLES: readonly SpcExample[] = [WORKBOOK_EXAMPLE, TURNING_EXAMPLE];
