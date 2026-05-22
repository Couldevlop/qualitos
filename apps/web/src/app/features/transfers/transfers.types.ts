/**
 * GDPR Cross-Border Transfers — RGPD Chapitre V (Art. 44-49).
 * Backend /api/v1/gdpr/cross-border-transfers.
 */

export type TransferStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'TERMINATED';

export type TransferMechanism =
  | 'ADEQUACY_DECISION'            // Art. 45
  | 'STANDARD_CONTRACTUAL_CLAUSES' // Art. 46.2.c-d (SCC 2021)
  | 'BINDING_CORPORATE_RULES'      // Art. 47 (BCR)
  | 'CODE_OF_CONDUCT'              // Art. 46.2.e
  | 'CERTIFICATION'                // Art. 46.2.f
  | 'DEROGATION_ART49';            // Art. 49 — exception, motivation obligatoire

export interface TransferView {
  id: string;
  tenantId: string;
  reference: string;
  recipientName: string;
  recipientLegalEntity?: string;
  recipientContact?: string;
  destinationCountries: string[];
  mechanism: TransferMechanism;
  safeguardsDescription?: string;
  safeguardsDocumentUrl?: string;
  derogationJustification?: string;
  dataCategories: string[];
  linkedProcessingActivityIds: string[];
  linkedProcessorAgreementIds: string[];
  status: TransferStatus;
  effectiveFrom?: string;
  effectiveTo?: string;
  suspendReason?: string;
  terminationReason?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTransferRequest {
  reference: string;
  recipientName: string;
  recipientLegalEntity?: string;
  recipientContact?: string;
  destinationCountries?: string[];
  mechanism: TransferMechanism;
  safeguardsDescription?: string;
  safeguardsDocumentUrl?: string;
  derogationJustification?: string;
  dataCategories?: string[];
  linkedProcessingActivityIds?: string[];
  linkedProcessorAgreementIds?: string[];
  createdByUserId: string;
}

export interface EditTransferRequest {
  recipientName: string;
  recipientLegalEntity?: string;
  recipientContact?: string;
  destinationCountries?: string[];
  mechanism: TransferMechanism;
  safeguardsDescription?: string;
  safeguardsDocumentUrl?: string;
  derogationJustification?: string;
  dataCategories?: string[];
  linkedProcessingActivityIds?: string[];
  linkedProcessorAgreementIds?: string[];
}

export interface SuspendTransferRequest { reason: string; }
export interface TerminateTransferRequest { reason: string; }
