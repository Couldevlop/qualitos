/**
 * GDPR Processor Agreements — DPA Art. 28 RGPD.
 * Backend /api/v1/gdpr/processor-agreements.
 */

export type PaStatus = 'DRAFT' | 'ACTIVE' | 'TERMINATED' | 'EXPIRED';

export interface PaView {
  id: string;
  tenantId: string;
  reference: string;
  processorName: string;
  processorLegalEntity?: string;
  processorContact?: string;
  processorDpoContact?: string;
  processorCountry?: string;       // ISO-3166-1 alpha-2
  servicesDescription: string;
  subProcessorCategories: string[];
  linkedProcessingActivityIds: string[];
  thirdCountryTransfers: string[];
  transferSafeguards?: string;
  contractDocumentUrl?: string;
  signedAt?: string;
  effectiveFrom?: string;
  expirationDate?: string;
  securityMeasures?: string;
  breachNotificationCommitmentHours: number;  // 1-720
  auditRights: boolean;
  auditRightsNotes?: string;
  dataReturnOrDeletionTerms?: string;
  status: PaStatus;
  terminationReason?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaRequest {
  reference: string;
  processorName: string;
  processorLegalEntity?: string;
  processorContact?: string;
  processorDpoContact?: string;
  processorCountry?: string;
  servicesDescription: string;
  subProcessorCategories?: string[];
  linkedProcessingActivityIds?: string[];
  thirdCountryTransfers?: string[];
  transferSafeguards?: string;
  contractDocumentUrl?: string;
  signedAt?: string;
  effectiveFrom?: string;
  expirationDate?: string;
  securityMeasures?: string;
  breachNotificationCommitmentHours: number;
  auditRights: boolean;
  auditRightsNotes?: string;
  dataReturnOrDeletionTerms?: string;
  createdByUserId: string;
}

export interface EditPaRequest {
  processorName: string;
  processorLegalEntity?: string;
  processorContact?: string;
  processorDpoContact?: string;
  processorCountry?: string;
  servicesDescription: string;
  subProcessorCategories?: string[];
  linkedProcessingActivityIds?: string[];
  thirdCountryTransfers?: string[];
  transferSafeguards?: string;
  contractDocumentUrl?: string;
  signedAt?: string;
  effectiveFrom?: string;
  expirationDate?: string;
  securityMeasures?: string;
  breachNotificationCommitmentHours: number;
  auditRights: boolean;
  auditRightsNotes?: string;
  dataReturnOrDeletionTerms?: string;
}

export interface TerminatePaRequest { reason: string; }
