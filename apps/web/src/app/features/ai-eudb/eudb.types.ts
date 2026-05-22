export type EudbStatus =
  | 'DRAFT' | 'SUBMITTED' | 'REGISTERED' | 'UPDATED' | 'REJECTED' | 'RETIRED';

export interface EudbView {
  id: string;
  tenantId: string;
  reference: string;
  aiSystemId: string;
  providerEntityName?: string | null;
  providerEuRepresentative?: string | null;
  memberStateOfReference?: string | null;
  intendedPurposeSummary?: string | null;
  technicalDocumentationReference?: string | null;
  eudbId?: string | null;
  status: EudbStatus;
  submittedAt?: string | null;
  submittedByUserId?: string | null;
  registrationDate?: string | null;
  lastUpdateDate?: string | null;
  lastUpdateSummary?: string | null;
  rejectedAt?: string | null;
  rejectionReason?: string | null;
  retiredAt?: string | null;
  retirementReason?: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface EudbDraftRequest {
  reference: string;
  aiSystemId: string;
  providerEntityName?: string;
  providerEuRepresentative?: string;
  memberStateOfReference?: string;
  intendedPurposeSummary?: string;
  technicalDocumentationReference?: string;
  createdByUserId: string;
}

export interface EudbEditRequest {
  providerEntityName?: string;
  providerEuRepresentative?: string;
  memberStateOfReference?: string;
  intendedPurposeSummary?: string;
  technicalDocumentationReference?: string;
}

export interface EudbSubmitRequest { submittedByUserId: string; }
export interface EudbMarkRegisteredRequest { eudbId: string; registrationDate: string; }
export interface EudbDeclareUpdateRequest { updateSummary: string; updateDate: string; }
export interface EudbRejectRequest { reason: string; }
export interface EudbRetireRequest { reason: string; }
