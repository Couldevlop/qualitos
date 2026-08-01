/**
 * Registre des systèmes d'IA — AI Act (règlement UE 2024/1689).
 * Contrat exact de `/api/v1/ai-act/systems` (AiSystemController + AiSystemDto).
 */

/**
 * Classification de risque (Art. 5, Annexe III, Art. 50).
 * L'ordre d'écriture n'a rien d'anodin : c'est celui de la sévérité décroissante,
 * repris tel quel pour trier le registre (voir `RISK_SEVERITY`).
 */
export type AiRiskClassification = 'UNACCEPTABLE' | 'HIGH' | 'LIMITED' | 'MINIMAL_OR_NO';

/** Rôle de l'organisation vis-à-vis du système (Art. 3). */
export type AiSystemRole = 'PROVIDER' | 'DEPLOYER' | 'IMPORTER' | 'DISTRIBUTOR';

/** Cycle de vie : DRAFT → REGISTERED → IN_USE → DECOMMISSIONED, et DRAFT|REGISTERED → WITHDRAWN. */
export type AiSystemStatus = 'DRAFT' | 'REGISTERED' | 'IN_USE' | 'DECOMMISSIONED' | 'WITHDRAWN';

/** Sévérité décroissante — sert au tri et aux compteurs, jamais à un libellé. */
export const RISK_SEVERITY: AiRiskClassification[] =
  ['UNACCEPTABLE', 'HIGH', 'LIMITED', 'MINIMAL_OR_NO'];

export interface AiSystemView {
  id: string;
  tenantId: string;
  reference: string;
  name: string;
  description: string | null;
  providerName: string | null;
  intendedPurpose: string;
  riskClassification: AiRiskClassification;
  role: AiSystemRole;
  generalPurpose: boolean;
  status: AiSystemStatus;
  conformityAssessmentEvidenceUrl: string | null;
  ceMarkingNumber: string | null;
  humanOversightDescription: string | null;
  transparencyMeasures: string | null;
  dataGovernanceNotes: string | null;
  linkedDpiaId: string | null;
  linkedProcessingActivityIds: string[];
  linkedAutomatedDecisionIds: string[];
  effectiveFrom: string | null;
  effectiveTo: string | null;
  withdrawalReason: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
  /** Dérivés serveur : c'est le back qui fait autorité sur les obligations. */
  prohibited: boolean;
  requiresConformityAssessment: boolean;
  requiresTransparency: boolean;
}

/** Corps commun aux deux écritures — seule la référence distingue la création. */
export interface AiSystemPayload {
  name: string;
  description: string | null;
  providerName: string | null;
  intendedPurpose: string;
  riskClassification: AiRiskClassification;
  role: AiSystemRole;
  generalPurpose: boolean;
  conformityAssessmentEvidenceUrl: string | null;
  ceMarkingNumber: string | null;
  humanOversightDescription: string | null;
  transparencyMeasures: string | null;
  dataGovernanceNotes: string | null;
  linkedDpiaId: string | null;
  linkedProcessingActivityIds: string[];
  linkedAutomatedDecisionIds: string[];
}

export interface AiSystemDraftRequest extends AiSystemPayload {
  reference: string;
  /** Exigé par le serveur ; provient du JWT côté client (AuthService), jamais saisi. */
  createdByUserId: string;
}

export type AiSystemEditRequest = AiSystemPayload;

export interface AiSystemWithdrawRequest {
  reason: string;
}

/** Filtres du registre — `null` = pas de filtre. */
export interface AiSystemFilter {
  status: AiSystemStatus | null;
  risk: AiRiskClassification | null;
}

/**
 * Vue de l'écran de liste : `all` (registre complet, pour des compteurs honnêtes)
 * et `rows` (résultat filtré, affiché dans le tableau).
 */
export interface AiSystemRegistry {
  all: AiSystemView[];
  rows: AiSystemView[];
}

/** Motif de référence imposé par le serveur (`@Pattern` du contrôleur ET du domaine). */
export const AI_SYSTEM_REFERENCE_PATTERN = /^[A-Z][A-Z0-9_-]{1,63}$/;

export const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Le domaine n'accepte qu'une URL http(s) sans espace (AiSystem#sanitizeUrl). */
export const HTTP_URL_PATTERN = /^https?:\/\/\S{1,1022}$/;
