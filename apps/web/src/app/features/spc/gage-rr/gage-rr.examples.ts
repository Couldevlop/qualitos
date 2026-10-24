/**
 * Le jeu d'exemple de l'onglet « Données » du classeur
 * `docs/spc/Etude_RR_Gage_1.xlsx` : arbre AX-210, diamètre extérieur,
 * tolérance 10,00 ± 0,10 mm, 3 opérateurs × 10 pièces × 3 essais, 5,15 σ.
 *
 * Mesures fictives générées pour la démonstration. Le classeur donne pour
 * elles %GRR 21,0 % (sous conditions) et ndc 6. Les opérateurs et le
 * responsable ne sont pas chargés : ce sont des personnes de l'annuaire, à
 * choisir.
 */
export interface GageRrExample {
  partReference: string;
  characteristic: string;
  instrument: string;
  lsl: number;
  usl: number;
  operatorCount: number;
  partCount: number;
  trialCount: number;
  sigmaMultiplier: number;
  /** [opérateur][pièce][essai] */
  grid: number[][][];
}

export const WORKBOOK_EXAMPLE: GageRrExample = {
  partReference: 'Arbre AX-210',
  characteristic: 'Diamètre extérieur (mm)',
  instrument: 'Micromètre numérique 0–25 mm, résolution 0,001 mm',
  lsl: 9.9,
  usl: 10.1,
  operatorCount: 3,
  partCount: 10,
  trialCount: 3,
  sigmaMultiplier: 5.15,
  grid: [
    [[9.955, 9.951, 9.94], [9.956, 9.966, 9.971], [9.978, 9.984, 9.981], [9.993, 9.985, 9.999],
      [9.997, 10.006, 9.995], [10.018, 10.016, 10.014], [10.017, 10.032, 10.022], [10.031, 10.039, 10.03],
      [10.071, 10.06, 10.057], [10.087, 10.073, 10.081]],
    [[9.949, 9.954, 9.943], [9.979, 9.974, 9.956], [9.983, 9.997, 9.993], [9.986, 10.002, 10.009],
      [10.026, 10.013, 10.022], [10.02, 10.026, 10.021], [10.029, 10.041, 10.019], [10.043, 10.046, 10.053],
      [10.066, 10.071, 10.055], [10.086, 10.089, 10.102]],
    [[9.943, 9.93, 9.936], [9.954, 9.959, 9.966], [9.975, 9.979, 9.988], [9.99, 9.989, 9.981],
      [10.009, 10, 10.008], [9.998, 10.01, 10.007], [10.042, 10.014, 10.039], [10.029, 10.041, 10.031],
      [10.058, 10.047, 10.054], [10.092, 10.094, 10.077]]
  ]
};
