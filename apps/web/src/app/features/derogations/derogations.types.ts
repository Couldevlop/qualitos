/**
 * Le contrat de la dérogation client, tel que le serveur le rend.
 *
 * Les champs portent les noms du serveur, sans traduction : un renommage au
 * passage obligerait à relire deux fichiers pour suivre une donnée, et c'est
 * exactement là que se glissent les écarts silencieux.
 */

/** Les sept états du cycle. Un seul fait sortir le dossier de l'organisation. */
export type DerogationStatus =
  | 'DRAFT'
  | 'PENDING_INTERNAL'
  | 'TO_AMEND'
  | 'QUALITY_APPROVED'
  | 'PENDING_CUSTOMER'
  | 'ACCEPTED'
  | 'REFUSED';

/** La réponse du client. « En attente » est une valeur, pas une absence. */
export type CustomerDecision =
  | 'PENDING'
  | 'ACCEPTED'
  | 'ACCEPTED_WITH_CONDITIONS'
  | 'REFUSED';

export type ShipmentStage = 'TO_SHIP' | 'ALREADY_SHIPPED';
export type DeviationOrigin = 'PRODUCT' | 'PROCESS';
export type Occurrence = 'FIRST' | 'REPEAT';
export type DurationKind = 'TEMPORARY' | 'PERMANENT';

/** La langue du document, figée sur le dossier au moment de l'émission. */
export type DerogationLanguage = 'FR' | 'EN';

/** Une ligne d'écart : ce qui était attendu, ce qui a été mesuré. */
export interface DerogationLine {
  position: number;
  specification: string | null;
  actualValue: string | null;
  nonConformingQty: number | null;
}

/** Une ligne du registre. Les colonnes suivent celles de la maquette. */
export interface DerogationRow {
  id: string;
  reference: string;
  status: DerogationStatus;
  createdAt: string;
  partReference: string | null;
  customerName: string | null;
  createdByName: string | null;
  /** Le premier écart, résumé « spécification → valeur réelle ». */
  firstDeviation: string | null;
  attachmentsNote: string | null;
  customerDecision: CustomerDecision;
}

export interface DerogationPage {
  content: DerogationRow[];
  total: number;
  page: number;
  size: number;
}

/** La fiche complète d'un dossier, ses six sections. */
export interface Derogation {
  id: string;
  reference: string;
  status: DerogationStatus;
  /** Rendu par le serveur : l'écran n'en décide pas. */
  editable: boolean;

  customerName: string | null;
  customerQualityContact: string | null;
  customerOrderNo: string | null;
  partReference: string | null;
  partIndex: string | null;
  batchNumber: string | null;
  quantity: number | null;
  requestDate: string | null;

  shipmentStage: ShipmentStage;
  deviationOrigin: DeviationOrigin;
  occurrence: Occurrence;
  durationKind: DurationKind;
  durationDetail: string | null;

  lines: DerogationLine[];
  justification: string | null;

  rootCause: string | null;
  containment: string | null;
  correctiveAction: string | null;
  capaReference: string | null;
  attachmentsNote: string | null;

  managerAccountId: string | null;
  managerAccountName: string | null;
  signedByName: string | null;
  /** Le tracé, pour le réafficher. Absent tant que le dossier n'est pas signé. */
  signaturePngBase64: string | null;
  signedAt: string | null;
  /** Le motif du renvoi pour correction. C'est ce qu'il faut corriger. */
  amendReason: string | null;

  customerEmail: string | null;
  sendComment: string | null;
  language: DerogationLanguage | null;
  sentAt: string | null;

  customerDecision: CustomerDecision;
  customerDecisionDate: string | null;

  createdByUserId: string | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Le compteur de « Mes validations » : l'alerte rouge et son nombre. */
export interface PendingApprovalCount {
  count: number;
}

// ---- ce que l'écran envoie --------------------------------------------------

export interface DerogationLineInput {
  specification: string | null;
  actualValue: string | null;
  nonConformingQty: number | null;
}

/**
 * La fiche, telle qu'on l'enregistre.
 *
 * Aucun champ n'est requis : un brouillon s'enregistre incomplet, autant de fois
 * qu'on veut. Les six champs obligatoires ne sont exigés qu'à la demande de
 * signature, et c'est le serveur qui les contrôle — le dupliquer ici ferait
 * deux règles à tenir d'accord.
 */
export interface DerogationSaveInput {
  customerName: string | null;
  customerQualityContact: string | null;
  customerOrderNo: string | null;
  partReference: string | null;
  partIndex: string | null;
  batchNumber: string | null;
  quantity: number | null;
  requestDate: string | null;

  shipmentStage: ShipmentStage;
  deviationOrigin: DeviationOrigin;
  occurrence: Occurrence;
  durationKind: DurationKind;
  durationDetail: string | null;

  lines: DerogationLineInput[];
  justification: string | null;

  rootCause: string | null;
  containment: string | null;
  correctiveAction: string | null;
  capaReference: string | null;
  attachmentsNote: string | null;
}

export interface DerogationSearchInput {
  owner?: string | null;
  partReference?: string | null;
  customerName?: string | null;
  status?: DerogationStatus | null;
  page?: number;
  size?: number;
}

export interface ApproveInput {
  signedByName: string;
  /** Avec ou sans le préfixe `data:image/png;base64,` : le serveur le retire. */
  signaturePngBase64: string;
}

export interface SendToCustomerInput {
  customerEmail: string;
  comment: string | null;
  /** Exigée : aucun document ne part dans une langue que personne n'a choisie. */
  language: DerogationLanguage;
}

export interface CustomerDecisionInput {
  decision: CustomerDecision;
  decidedOn: string;
}
