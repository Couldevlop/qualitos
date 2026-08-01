import { SpringPage } from '../pdca/pdca.types';

/**
 * Calibration & gestion des équipements (§4.10).
 * Contrat de `/api/v1/calibration` (CalibrationController + CalibrationDto).
 *
 * Le tenant n'apparaît jamais en entrée : il est dérivé du JWT côté serveur
 * (règle §18.2 #2). Il figure en sortie parce que le serveur le renvoie.
 */

/** Cycle de vie d'un équipement. `RETIRED` est terminal côté serveur. */
export type EquipmentStatus = 'ACTIVE' | 'OUT_OF_SERVICE' | 'RETIRED';

/** Verdict d'une opération de calibration. */
export type CalibrationResult = 'PASS' | 'CONDITIONAL' | 'FAIL';

/** Type d'étude MSA (AIAG / IATF 16949). */
export type MsaType = 'GAGE_R_R' | 'BIAS' | 'LINEARITY' | 'STABILITY';

/** Verdict d'une étude MSA. */
export type MsaResult = 'PASS' | 'MARGINAL' | 'FAIL';

// ----- Équipement -----

export interface EquipmentResponse {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  manufacturer: string | null;
  model: string | null;
  serialNumber: string | null;
  location: string | null;
  status: EquipmentStatus;
  critical: boolean;
  iotDeviceId: string | null;
  ownerUserId: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export type EquipmentPage = SpringPage<EquipmentResponse>;

export interface CreateEquipmentRequest {
  code: string;
  name: string;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  location?: string;
  critical: boolean;
  iotDeviceId?: string;
  ownerUserId?: string;
  /** UUID de l'utilisateur courant : exigé (@NotNull) par le serveur. */
  createdBy: string;
}

/**
 * PATCH partiel : le serveur n'applique que les champs non `null`. Un champ omis
 * conserve donc sa valeur — impossible de vider une valeur existante par cette route.
 */
export interface UpdateEquipmentRequest {
  name?: string;
  manufacturer?: string;
  model?: string;
  serialNumber?: string;
  location?: string;
  critical?: boolean;
  iotDeviceId?: string;
  ownerUserId?: string;
}

// ----- Plan de calibration -----

export interface PlanResponse {
  id: string;
  tenantId: string;
  equipmentId: string;
  frequencyMonths: number;
  procedureReference: string | null;
  tolerance: string | null;
  accreditationRef: string | null;
  lastCalibratedOn: string | null;
  nextDueOn: string | null;
  /** Calculé par le serveur avec SON horloge : seule source de vérité du retard. */
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

export type PlanPage = SpringPage<PlanResponse>;

export interface UpsertPlanRequest {
  frequencyMonths: number;
  procedureReference?: string;
  tolerance?: string;
  accreditationRef?: string;
  /**
   * Échéance initiale, utilisée uniquement tant qu'aucune date n'est fixée.
   * Ensuite ce sont les enregistrements qui pilotent `nextDueOn`.
   */
  firstDueOn: string;
}

// ----- Enregistrements de calibration -----

export interface RecordResponse {
  id: string;
  tenantId: string;
  equipmentId: string;
  performedOn: string;
  performedByUserId: string | null;
  performedByOrg: string | null;
  result: CalibrationResult;
  measurements: string | null;
  certificateReference: string | null;
  nextDueOverride: string | null;
  createdAt: string;
}

export type RecordPage = SpringPage<RecordResponse>;

export interface CreateRecordRequest {
  performedOn: string;
  performedByUserId?: string;
  performedByOrg?: string;
  result: CalibrationResult;
  measurements?: string;
  certificateReference?: string;
  /** Écrase l'échéance dérivée du plan (`performedOn + frequencyMonths`). */
  nextDueOverride?: string;
}

// ----- Études MSA -----

export interface MsaResponse {
  id: string;
  tenantId: string;
  equipmentId: string;
  type: MsaType;
  performedOn: string;
  studyValue: number;
  passingThreshold: number | null;
  result: MsaResult;
  notes: string | null;
  createdBy: string;
  createdAt: string;
}

export type MsaPage = SpringPage<MsaResponse>;

export interface CreateMsaRequest {
  type: MsaType;
  performedOn: string;
  studyValue: number;
  passingThreshold?: number;
  result: MsaResult;
  notes?: string;
  createdBy: string;
}

// ----- Synthèse d'un équipement -----

export interface EquipmentSummary {
  equipmentId: string;
  status: EquipmentStatus;
  critical: boolean;
  lastCalibratedOn: string | null;
  nextDueOn: string | null;
  overdue: boolean;
  lastResult: CalibrationResult | null;
  passRecords: number;
  failRecords: number;
  conditionalRecords: number;
  msaPass: number;
  msaFail: number;
}

// ----- Vues composées (côté écran, pas côté serveur) -----

/** Criticité d'échéance d'une ligne de la liste. */
export type DueState = 'OVERDUE' | 'DUE_SOON' | 'OK';

/**
 * Ligne de la liste : un équipement, accompagné de son plan **uniquement s'il est
 * à risque**. La route de liste des équipements ne porte aucune date d'échéance ;
 * la seule route qui expose les échéances en masse est `/plans/overdue?cutoff=…`.
 */
export interface EquipmentRow {
  equipment: EquipmentResponse;
  /** Plan du fenêtrage `cutoff` — `null` si l'échéance est au-delà de l'horizon. */
  plan: PlanResponse | null;
  due: DueState;
}

export interface CalibrationOverview {
  page: EquipmentPage;
  rows: EquipmentRow[];
  /** Échéances dépassées sur tout le tenant (pas seulement la page courante). */
  overdueCount: number;
  /** Échéances à venir dans l'horizon, non encore dépassées. */
  dueSoonCount: number;
}

export interface EquipmentDetail {
  equipment: EquipmentResponse;
  summary: EquipmentSummary;
  /** `null` quand aucun plan n'est configuré (le serveur répond 404). */
  plan: PlanResponse | null;
  records: RecordResponse[];
  msa: MsaResponse[];
}
