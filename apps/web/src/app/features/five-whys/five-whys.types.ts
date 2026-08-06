/**
 * Analyse des 5 Pourquoi rattachée à une non-conformité (§3.5).
 *
 * Cinq n'est pas un dogme mais un ordre de grandeur : le modèle porte une SUITE
 * de pourquoi, bornée à sept. Au-delà, on n'remonte plus une cause, on énumère
 * des circonstances.
 */
export interface FiveWhysStep {
  id: string;
  /** Rang dans la chaîne, à partir de 1. L'ordre est le sens de la méthode. */
  position: number;
  answer: string;
  createdAt: string;
  updatedAt: string;
}

export interface FiveWhysAnalysis {
  id: string;
  ncId: string;
  /** Référence lisible de la non-conformité : c'est elle qu'on affiche. */
  ncReference: string;
  problem: string;
  /** Nulle tant que l'analyse n'aboutit pas. */
  rootCause?: string | null;
  steps: FiveWhysStep[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateFiveWhysRequest {
  ncId: string;
  /** Absent = on reprend le titre de la non-conformité. */
  problem?: string;
}
