/**
 * Le cycle APQP d'un client, tel que le serveur le rend.
 *
 * <p>Les cinq phases du document de référence ne sont plus une constante du
 * code : elles amorcent le cycle d'un client, qui le remanie ensuite. D'où ces
 * types, là où un fichier de référence suffisait.
 */

/**
 * Le genre d'un livrable : ce que son formulaire demande.
 *
 * <p>Un jeu fermé, décidé par le serveur, et non déduit du libellé : deviner
 * qu'« Control plan » renvoie au module des plans de surveillance marcherait sur
 * le référentiel et sur rien d'autre.
 */
export type ApqpDeliverableKind = 'ATTACHMENT' | 'MODULE_LINK' | 'DATA_ENTRY' | 'CHECKLIST';

/** Le module visé par un livrable de genre `MODULE_LINK`. */
export type ApqpLinkedKind = 'FMEA' | 'CONTROL_PLAN' | 'PDCA' | 'CAPA';

/**
 * Une ligne du contenu d'un livrable.
 *
 * <p>Une seule forme pour les deux genres qui en portent : un sous-point
 * n'utilise que `label` et `checked`, une mesure que `label`, `value`, `unit` et
 * `measuredAt`. Deux types auraient obligé l'écran à choisir avant d'avoir lu le
 * genre du livrable.
 */
export interface ApqpDataRow {
  label: string;
  value?: string | null;
  unit?: string | null;
  /** Date ISO (yyyy-MM-dd), ou absente : une mesure peut ne pas être datée. */
  measuredAt?: string | null;
  checked?: boolean | null;
}

/** Un livrable attendu en sortie de phase. */
export interface ApqpDeliverable {
  id: string;
  position: number;
  label: string;
  /** Élément du dossier PPAP — l'astérisque du référentiel. */
  ppap: boolean;
  kind: ApqpDeliverableKind;
  done: boolean;
  doneAt?: string | null;
  doneBy?: string | null;
  comment?: string | null;
  data?: ApqpDataRow[] | null;
  linkedKind?: ApqpLinkedKind | null;
  linkedId?: string | null;
  /**
   * Nombre de pièces versées.
   *
   * <p>Un compte plutôt que les pièces : la liste d'une phase dit « prouvé »
   * sans réclamer une URL présignée par fichier, qui coûterait un appel au
   * stockage pour une icône.
   */
  evidenceCount: number;
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

/**
 * Le cycle, et l'état de son dossier PPAP.
 *
 * <p>Le compte voyage avec le cycle : la section PPAP surmonte le même cycle que
 * le schéma, et deux requêtes pourraient se répondre sur deux états différents.
 */
export interface ApqpCycle {
  phases: ApqpPhase[];
  ppapDone: number;
  ppapTotal: number;
}

export interface CreateApqpPhaseRequest {
  title: string;
  purpose?: string;
  question?: string;
}

export type UpdateApqpPhaseRequest = CreateApqpPhaseRequest;

export interface ApqpDeliverableRequest {
  label: string;
  ppap: boolean;
  kind: ApqpDeliverableKind;
}

/**
 * Ce qu'on déclare d'un livrable.
 *
 * <p>Ni l'heure ni l'auteur : le serveur les pose depuis le jeton. Les envoyer
 * laisserait antidater un livrable et l'attribuer à quelqu'un d'autre.
 */
export interface ApqpCompletionRequest {
  done: boolean;
  comment?: string | null;
  data?: ApqpDataRow[] | null;
  linkedKind?: ApqpLinkedKind | null;
  linkedId?: string | null;
}

/** Une pièce versée en preuve d'un livrable. L'URL est présignée, courte. */
export interface ApqpEvidence {
  id: string;
  phaseId: string;
  deliverableId: string;
  url?: string | null;
  contentType: string;
  sizeBytes: number;
  originalFilename: string;
  createdAt: string;
}

/** L'ordre du cycle entier — jamais un déplacement isolé (cf. le service). */
export interface ApqpReorderRequest {
  phaseIds: string[];
}
