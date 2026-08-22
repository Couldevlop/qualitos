/**
 * Référentiel Produit, PFMEA rattaché et Control Plan.
 *
 * <p>Aucun type ne porte de `tenantId` en entrée : le tenant vient du jeton côté
 * serveur (§18.2 #2). Les vues en renvoient un parce que le serveur le renvoie —
 * l'y accepter en écriture ouvrirait la porte d'à côté.
 */

export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'OBSOLETE';

export interface ProductResponse {
  id: string;
  code: string;
  designation: string;
  family?: string;
  revisionIndex?: string;
  status: ProductStatus;
  customerLabel?: string;
  siteLabel?: string;
  ownerUserId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProductRequest {
  code: string;
  designation: string;
  family?: string;
  revisionIndex?: string;
  customerLabel?: string;
  siteLabel?: string;
  ownerUserId?: string;
}

export type UpdateProductRequest = Omit<CreateProductRequest, 'code'>;

export interface ProductComponentResponse {
  id: string;
  sequenceNo: number;
  reference: string;
  label?: string;
  quantity?: number;
  unit?: string;
  supplierId?: string;
}

export interface ProductComponentRequest {
  sequenceNo: number;
  reference: string;
  label?: string;
  quantity?: number;
  unit?: string;
  supplierId?: string;
}

export interface ProductOperationResponse {
  id: string;
  sequenceNo: number;
  code: string;
  label: string;
  workstation?: string;
}

export interface ProductOperationRequest {
  sequenceNo: number;
  code: string;
  label: string;
  workstation?: string;
}

// ---------- Control Plan ----------

export type ControlPlanPhase = 'PROTOTYPE' | 'PRE_LAUNCH' | 'PRODUCTION';
export type ControlPlanStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
export type CharacteristicType = 'PRODUCT' | 'PROCESS';
export type CharacteristicClass = 'STANDARD' | 'SPECIAL' | 'SAFETY' | 'REGULATORY';

export interface ControlPlanView {
  id: string;
  productId: string;
  phase: ControlPlanPhase;
  code: string;
  revision: number;
  status: ControlPlanStatus;
  ownerUserId?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
  updatedAt: string;
  /**
   * Empreinte du document approuvé et référence de la transaction qui l'ancre.
   * Absentes tant que le plan est en brouillon : un document qui n'est pas
   * opposable n'a rien à prouver.
   */
  sealSha256?: string;
  anchorTxRef?: string;
}

export type InputOutput = 'INPUT' | 'OUTPUT';

export interface ControlPlanLineView {
  id: string;
  sequenceNo: number;
  operationId?: string;
  machine?: string;
  characteristicNo?: string;
  characteristicLabel: string;
  characteristicType: CharacteristicType;
  specialClass?: CharacteristicClass;
  specification?: string;
  toleranceLower?: number;
  toleranceUpper?: number;
  unit?: string;
  measurementTechnique?: string;
  /**
   * Texte et non nombre : « 100 % (automatisé) » ou « 5 au réglage puis 1 sur
   * 50 » sont des tailles d'échantillon parfaitement valides.
   */
  sampleSize?: string;
  sampleFrequency?: string;
  controlMethod?: string;
  reactionPlan?: string;
  /** Absent : le contrôle n'est justifié par aucune ligne de PFMEA. */
  fmeaItemId?: string;
  /** Référence de la procédure appliquée au poste (colonne « SOP # »). */
  sopReference?: string;
  /** Entrée surveillée ou sortie constatée. */
  inputOutput?: InputOutput;
  /** Qui — ou quoi — mesure : la trame accepte une personne comme une machine. */
  whoMeasures?: string;
  /** Où l'enregistrement est conservé : c'est ce que l'auditeur suivra. */
  recordingLocation?: string;
}

export interface ControlPlanDetail {
  plan: ControlPlanView;
  lines: ControlPlanLineView[];
}

export interface CreateControlPlanRequest {
  phase: ControlPlanPhase;
  code: string;
  ownerUserId?: string;
}

export interface ControlPlanLineRequest {
  sequenceNo: number;
  operationId?: string;
  machine?: string;
  characteristicNo?: string;
  characteristicLabel: string;
  characteristicType: CharacteristicType;
  specialClass?: CharacteristicClass;
  specification?: string;
  toleranceLower?: number;
  toleranceUpper?: number;
  unit?: string;
  measurementTechnique?: string;
  /**
   * Texte et non nombre : « 100 % (automatisé) » ou « 5 au réglage puis 1 sur
   * 50 » sont des tailles d'échantillon parfaitement valides.
   */
  sampleSize?: string;
  sampleFrequency?: string;
  controlMethod?: string;
  reactionPlan?: string;
  fmeaItemId?: string;
  sopReference?: string;
  inputOutput?: InputOutput;
  whoMeasures?: string;
  recordingLocation?: string;
}

// ---------- Propositions de révision ----------

export type RevisionTargetType = 'PFMEA_ITEM' | 'PFMEA_ITEM_CREATE' | 'CONTROL_PLAN_LINE_CREATE';
export type RevisionTriggerType = 'NC_CREATED' | 'CAPA_CLOSED';
export type RevisionRequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'SUPERSEDED';

export interface RevisionRequestView {
  id: string;
  productId: string;
  targetType: RevisionTargetType;
  targetId?: string;
  triggerType: RevisionTriggerType;
  triggerRefId: string;
  triggerRefLabel: string;
  /** La justification chiffrée : sans elle, la proposition ne se conteste pas. */
  rationale: string;
  field?: string;
  from?: string;
  to?: string;
  draftJson?: string;
  status: RevisionRequestStatus;
  decidedBy?: string;
  decidedAt?: string;
  decisionNote?: string;
  createdAt: string;
  updatedAt: string;
}

/** Suggestion de mode de défaillance pour une non-conformité en cours de saisie. */
export interface FailureModeSuggestion {
  fmeaItemId: string;
  score: number;
  /** Les termes qui ont motivé la suggestion : c'est ce qui la rend contestable. */
  matchedTerms: string;
}
