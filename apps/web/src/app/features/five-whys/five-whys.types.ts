/**
 * Nombre de pourquoi exigés avant de conclure une cause racine.
 *
 * <p>Écrit ici et non dans un écran : la règle vaut partout où la cause racine
 * se conclut — l'écran dédié, et les figures d'analyse de cause de la fiche
 * d'écart comme du D4 du rapport 8D. Deux copies auraient divergé au premier
 * ajustement, et l'une des deux aurait fait découvrir la règle par un refus du
 * serveur, qui applique la même ({@code MIN_WHYS_BEFORE_ROOT_CAUSE}).
 */
export const MIN_WHYS_BEFORE_ROOT_CAUSE = 3;

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
