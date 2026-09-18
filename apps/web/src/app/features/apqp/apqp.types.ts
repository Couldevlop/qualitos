/**
 * Les projets APQP d'un client, et le cycle de chacun, tels que le serveur les rend.
 *
 * <p>Le cycle n'appartient plus au client mais à un PROJET : un même client mène
 * de front un lancement de produit, un transfert d'activité et l'ouverture d'un
 * nouveau client, et chacun a ses phases, ses livrables et son dossier PPAP. Un
 * cycle unique par client obligeait à les mélanger, ce qui rendait le dossier
 * remis au client illisible.
 */

/**
 * Ce qui motive un projet APQP.
 *
 * <p>Un jeu fermé, décidé par le serveur : il oriente les livrables attendus, et
 * une saisie libre aurait interdit tout filtre et tout comparatif entre projets.
 */
export type ApqpProjectType = 'NPI' | 'TOW' | 'MAJOR_MODIFICATION' | 'OTHER';

/** Le module visé par le renvoi FACULTATIF d'un livrable. */
export type ApqpLinkedKind = 'FMEA' | 'CONTROL_PLAN' | 'PDCA' | 'CAPA';

/**
 * Où en est un livrable.
 *
 * <p>Quatre états, dont `BLOCKED` : un livrable en attente d'un tiers n'est ni
 * « pas commencé » ni « en cours », et les confondre masquait exactement ce qui
 * retarde le projet.
 */
export type ApqpDeliverableStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'BLOCKED' | 'DONE';

/** Un projet APQP, avec l'avancement que le serveur compte pour lui. */
export interface ApqpProject {
  id: string;
  name: string;
  type: ApqpProjectType;
  customer?: string | null;
  reference?: string | null;
  description?: string | null;
  /** Livrables du projet, tous confondus — comptés par le serveur. */
  deliverablesTotal: number;
  deliverablesDone: number;
  /** Livrables que l'utilisateur a marqués « requis au dossier PPAP ». */
  ppapTotal: number;
  ppapDone: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateApqpProjectRequest {
  name: string;
  type: ApqpProjectType;
  customer?: string | null;
  reference?: string | null;
  description?: string | null;
}

export type UpdateApqpProjectRequest = CreateApqpProjectRequest;

/**
 * Un livrable attendu en sortie de phase.
 *
 * <p>Un seul formulaire pour tous : le genre qui décidait autrefois du corps du
 * popup a disparu. Il obligeait à qualifier un livrable avant de savoir ce qu'on
 * en ferait, et interdisait de joindre une pièce à un livrable qualifié
 * « renvoi » — alors que c'est précisément ce qu'un auditeur demande.
 */
export interface ApqpDeliverable {
  id: string;
  position: number;
  label: string;
  /** Ce que le livrable doit produire, en clair : un document, un relevé, un accord. */
  expectedArtifact?: string | null;
  /** Requis au dossier PPAP — c'est l'UTILISATEUR qui en décide, pas un référentiel. */
  ppap: boolean;
  owner?: string | null;
  /** Date ISO (yyyy-MM-dd), ou absente : une échéance peut n'être pas posée. */
  dueDate?: string | null;
  status: ApqpDeliverableStatus;
  /** Avancement déclaré, de 0 à 100. */
  percentComplete: number;
  done: boolean;
  doneAt?: string | null;
  doneBy?: string | null;
  comment?: string | null;
  /** Renvoi FACULTATIF vers un enregistrement déjà tenu ailleurs dans QualitOS. */
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
 * Le cycle d'un projet, et l'état de son dossier PPAP.
 *
 * <p>Le projet voyage avec le cycle : l'écran affiche son nom et son client sans
 * un second appel, et deux requêtes ne peuvent pas se répondre sur deux états.
 */
export interface ApqpCycle {
  projectId: string;
  projectName: string;
  projectType: ApqpProjectType;
  customer?: string | null;
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

/** Ce qu'on dit d'un livrable en le créant ou en le reformulant. */
export interface ApqpDeliverableRequest {
  label: string;
  expectedArtifact?: string | null;
  ppap: boolean;
}

/**
 * Le formulaire UNIQUE du livrable.
 *
 * <p>Ni l'heure ni l'auteur : le serveur les pose depuis le jeton. Les envoyer
 * laisserait antidater un livrable et l'attribuer à quelqu'un d'autre.
 *
 * <p>`status` et `percentComplete` sont FACULTATIFS parce que la case pilote :
 * `done:true` impose `DONE` et 100, `done:false` ramène sous les 100. Les
 * laisser absents, c'est demander au serveur d'appliquer sa règle plutôt que de
 * lui dicter un état que l'écran aurait recalculé de son côté.
 */
export interface ApqpCompletionRequest {
  done: boolean;
  expectedArtifact?: string | null;
  ppap?: boolean | null;
  owner?: string | null;
  dueDate?: string | null;
  status?: ApqpDeliverableStatus | null;
  percentComplete?: number | null;
  comment?: string | null;
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
