/**
 * Le cycle APQP d'un client, tel que le serveur le rend.
 *
 * <p>Les cinq phases du manuel AIAG ne sont plus une constante du code : elles
 * amorcent le cycle d'un client, qui le remanie ensuite. D'où ces types, là où
 * un fichier de référence suffisait.
 */

/** Un livrable attendu en sortie de phase. */
export interface ApqpDeliverable {
  id: string;
  position: number;
  label: string;
}

export interface ApqpPhase {
  id: string;
  /** Rang dans le cycle, à partir de 1. */
  position: number;
  /**
   * Rang du jalon dans le V : 1 en haut, le plus grand au point bas.
   *
   * <p>Calculé par le SERVEUR et non par l'écran : deux vues du même cycle
   * doivent le dessiner pareil, et la règle change dès qu'on ajoute une phase.
   */
  level: number;
  title: string;
  purpose: string | null;
  question: string | null;
  deliverables: ApqpDeliverable[];
}

export interface CreateApqpPhaseRequest {
  title: string;
  purpose?: string;
  question?: string;
}

export type UpdateApqpPhaseRequest = CreateApqpPhaseRequest;

export interface ApqpDeliverableRequest {
  label: string;
}

/** L'ordre du cycle entier — jamais un déplacement isolé (cf. le service). */
export interface ApqpReorderRequest {
  phaseIds: string[];
}
