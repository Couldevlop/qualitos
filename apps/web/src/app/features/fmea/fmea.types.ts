import { SpringPage } from '../pdca/pdca.types';

export type FmeaStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type FmeaType =
  | 'PROCESS_FMEA'
  | 'DESIGN_FMEA'
  | 'SYSTEM_FMEA'
  | 'SERVICE_FMEA'
  | 'BOW_TIE';

export type ActionPriority = 'HIGH' | 'MEDIUM' | 'LOW';

export type CharacteristicClass = 'STANDARD' | 'SPECIAL' | 'SAFETY' | 'REGULATORY';

export interface FmeaProjectResponse {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  scope?: string;
  type: FmeaType;
  status: FmeaStatus;
  criticalRpnThreshold: number;
  revision: number;
  /** Produit couvert. Absent pour les FMEA système, service et bow-tie. */
  productId?: string;
  ownerUserId?: string;
  lastReviewedAt?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export type FmeaProjectPage = SpringPage<FmeaProjectResponse>;

export interface CreateFmeaProjectRequest {
  code: string;
  name: string;
  scope?: string;
  type: FmeaType;
  criticalRpnThreshold?: number;
  ownerUserId?: string;
  createdBy: string;
}

export interface UpdateFmeaProjectRequest {
  name?: string;
  scope?: string;
  criticalRpnThreshold?: number;
  ownerUserId?: string;
}

export interface FmeaItemResponse {
  id: string;
  tenantId: string;
  projectId: string;
  sequenceNo: number;
  function?: string;
  failureMode?: string;
  failureEffect?: string;
  failureCause?: string;
  currentControls?: string;
  severity: number;
  occurrence: number;
  detection: number;
  rpn: number;
  recommendedAction?: string;
  actionOwnerUserId?: string;
  /** Le responsable en clair : souvent un service, pas une personne. */
  actionOwnerName?: string;
  actionDueDate?: string;
  /** Ce qui a réellement été fait — et qui justifie la nouvelle cotation. */
  actionsTaken?: string;
  actionsTakenAt?: string;
  resultingSeverity?: number;
  resultingOccurrence?: number;
  resultingDetection?: number;
  rpnAfter?: number;
  /** Opération de gamme visée : le mot commun avec le control plan. */
  operationId?: string;
  characteristicClass?: CharacteristicClass;
  /**
   * Priorité d'action AIAG-VDA. Elle lit les trois notes séparément là où le RPN
   * les multiplie — un RPN de 120 peut cacher une défaillance grave comme une
   * défaillance fréquente et bénigne. Absente tant que l'item n'est pas coté.
   */
  actionPriority?: ActionPriority;
  critical: boolean;
  createdAt: string;
  updatedAt: string;
}

export type FmeaItemPage = SpringPage<FmeaItemResponse>;

export interface CreateFmeaItemRequest {
  function?: string;
  failureMode?: string;
  failureEffect?: string;
  failureCause?: string;
  currentControls?: string;
  severity: number;
  occurrence: number;
  detection: number;
  recommendedAction?: string;
  actionOwnerUserId?: string;
  /** Le responsable en clair : souvent un service, pas une personne. */
  actionOwnerName?: string;
  actionDueDate?: string;
  /** Ce qui a réellement été fait — et qui justifie la nouvelle cotation. */
  actionsTaken?: string;
  actionsTakenAt?: string;
  resultingSeverity?: number;
  resultingOccurrence?: number;
  resultingDetection?: number;
}

export interface UpdateFmeaItemRequest {
  function?: string;
  failureMode?: string;
  failureEffect?: string;
  failureCause?: string;
  currentControls?: string;
  severity?: number;
  occurrence?: number;
  detection?: number;
  recommendedAction?: string;
  actionOwnerUserId?: string;
  /** Le responsable en clair : souvent un service, pas une personne. */
  actionOwnerName?: string;
  actionDueDate?: string;
  /** Ce qui a réellement été fait — et qui justifie la nouvelle cotation. */
  actionsTaken?: string;
  actionsTakenAt?: string;
  resultingSeverity?: number;
  resultingOccurrence?: number;
  resultingDetection?: number;
}

export interface FmeaProjectStatistics {
  projectId: string;
  totalItems: number;
  criticalItems: number;
  maxRpn: number;
  averageRpn: number;
  criticalRpnThreshold: number;
}

// ---------- Référentiel de cotation (barèmes S/O/D) ----------

/** Les trois échelles dont le produit fait le RPN. */
export type FmeaScaleKind = 'SEVERITY' | 'OCCURRENCE' | 'DETECTION';

/** Une ligne de barème : ce que vaut un score, et pourquoi. */
export interface FmeaScaleRow {
  score: number;
  label: string;
  description?: string;
  /** Occurrence seulement : « une fois par semaine ». */
  timePeriod?: string;
  /** Occurrence seulement : « 1 sur 8 ». */
  failureRate?: string;
}

/**
 * Une échelle telle que le serveur la rend.
 *
 * <p>`custom` dit si l'organisation cote sur SON barème ou sur celui de
 * référence. Ce n'est pas cosmétique : deux RPN issus de barèmes différents ne
 * se comparent pas, et l'écran doit pouvoir le signaler.
 */
export interface FmeaScaleView {
  kind: FmeaScaleKind;
  custom: boolean;
  rows: FmeaScaleRow[];
  updatedBy?: string;
  updatedAt?: string;
}

export interface FmeaScaleReference {
  scales: FmeaScaleView[];
}
