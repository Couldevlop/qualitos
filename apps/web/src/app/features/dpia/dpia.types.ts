/**
 * GDPR DPIA — Data Protection Impact Assessment (Art. 35 RGPD).
 * Backend /api/v1/gdpr/dpias.
 *
 * Workflow :
 *   DRAFT → IN_PROGRESS → DPO_REVIEW → APPROVED | REJECTED → ARCHIVED
 *   IN_PROGRESS peut être renvoyée en DRAFT pour correction.
 *
 * Invariant Art. 36 : un risque résiduel HIGH ou SEVERE déclenche
 * l'obligation de consultation préalable de l'autorité de contrôle.
 */

export type DpiaStatus =
  | 'DRAFT' | 'IN_PROGRESS' | 'DPO_REVIEW'
  | 'APPROVED' | 'REJECTED' | 'ARCHIVED';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'SEVERE';

export interface DpiaView {
  id: string;
  tenantId: string;
  reference: string;
  title: string;
  description?: string;
  linkedProcessingActivityIds: string[];
  necessityAndProportionalityNotes?: string;
  risksToRightsAndFreedoms?: string;
  mitigationMeasures?: string;
  overallRiskLevel: RiskLevel;
  consultationRequired: boolean;
  consultationNotes?: string;
  status: DpiaStatus;
  dpoUserId?: string;
  dpoOpinion?: string;
  dpoOpinionAt?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdByUserId: string;
  handledByUserId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDpiaRequest {
  reference: string;
  title: string;
  description?: string;
  linkedProcessingActivityIds?: string[];
  initialRiskLevel: RiskLevel;
  createdByUserId: string;
}

export interface EditDpiaRequest {
  title: string;
  description?: string;
  linkedProcessingActivityIds?: string[];
  necessityAndProportionalityNotes?: string;
  risksToRightsAndFreedoms?: string;
  mitigationMeasures?: string;
  overallRiskLevel: RiskLevel;
  consultationRequired: boolean;
  consultationNotes?: string;
}

export interface StartDpiaRequest { handledByUserId: string; }

export interface OpinionRequest {
  dpoUserId: string;
  dpoOpinion: string;
}
