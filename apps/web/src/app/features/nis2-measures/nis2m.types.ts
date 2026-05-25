export type Nis2MeasureStatus =
  | 'PLANNED' | 'IN_PROGRESS' | 'IMPLEMENTED' | 'VERIFIED' | 'DEPRECATED';

export type Nis2MeasureCategory =
  | 'RISK_ANALYSIS'
  | 'INCIDENT_HANDLING'
  | 'BUSINESS_CONTINUITY'
  | 'SUPPLY_CHAIN_SECURITY'
  | 'SECURE_DEVELOPMENT'
  | 'EFFECTIVENESS_ASSESSMENT'
  | 'CYBER_HYGIENE_TRAINING'
  | 'CRYPTOGRAPHY'
  | 'HR_AND_ACCESS_CONTROL'
  | 'MFA_AND_COMMUNICATIONS';

export type ResidualRiskRating = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export const CATEGORY_LABEL: Record<Nis2MeasureCategory, string> = {
  RISK_ANALYSIS:           '(a) Analyse de risque & politiques SI',
  INCIDENT_HANDLING:       '(b) Gestion des incidents',
  BUSINESS_CONTINUITY:     '(c) Continuité d\'activité',
  SUPPLY_CHAIN_SECURITY:   '(d) Sécurité chaîne d\'approvisionnement',
  SECURE_DEVELOPMENT:      '(e) Sécurité dév./acquisition/maintenance',
  EFFECTIVENESS_ASSESSMENT:'(f) Évaluation efficacité des mesures',
  CYBER_HYGIENE_TRAINING:  '(g) Hygiène cyber & formation',
  CRYPTOGRAPHY:            '(h) Cryptographie & chiffrement',
  HR_AND_ACCESS_CONTROL:   '(i) Sécurité RH & contrôle d\'accès',
  MFA_AND_COMMUNICATIONS:  '(j) MFA & communications sécurisées'
};

export interface Nis2MeasureView {
  id: string;
  tenantId: string;
  reference: string;
  category: Nis2MeasureCategory;
  title: string;
  description?: string | null;
  status: Nis2MeasureStatus;
  ownerUserId?: string | null;
  maturityLevel: number;
  residualRiskRating: ResidualRiskRating;
  criticalRiskJustification?: string | null;
  reviewIntervalDays: number;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  lastReviewedAt?: string | null;
  reviewedByUserId?: string | null;
  nextReviewDueAt?: string | null;
  evidenceUrls?: string[] | null;
  linkedProcessingActivityIds?: string[] | null;
  linkedProcessorAgreementIds?: string[] | null;
  notes?: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
  reviewOverdue: boolean;
  criticalResidualRisk: boolean;
}

export interface Nis2MeasurePlanRequest {
  reference: string;
  category: Nis2MeasureCategory;
  title: string;
  description?: string;
  ownerUserId?: string;
  maturityLevel: number;
  residualRiskRating: ResidualRiskRating;
  criticalRiskJustification?: string;
  reviewIntervalDays: number;
  evidenceUrls?: string[];
  linkedProcessingActivityIds?: string[];
  linkedProcessorAgreementIds?: string[];
  notes?: string;
  createdByUserId: string;
}

export interface Nis2MeasureEditRequest {
  title: string;
  description?: string;
  ownerUserId?: string;
  maturityLevel: number;
  residualRiskRating: ResidualRiskRating;
  criticalRiskJustification?: string;
  reviewIntervalDays: number;
  evidenceUrls?: string[];
  linkedProcessingActivityIds?: string[];
  linkedProcessorAgreementIds?: string[];
  notes?: string;
}

export interface Nis2MeasureVerifyRequest { reviewedByUserId: string; reviewedAt: string; }
export interface Nis2MeasureReviewRequest { reviewedByUserId: string; reviewedAt: string; }
