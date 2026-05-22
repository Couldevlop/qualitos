/**
 * AI Act — Conformity Assessments (Art. 43).
 * Backend /api/v1/ai-act/conformity-assessments.
 */

export type ConformityStatus =
  | 'PLANNED' | 'IN_PROGRESS' | 'CERTIFIED' | 'EXPIRED' | 'REVOKED' | 'FAILED';

export type ConformityProcedure = 'INTERNAL_CONTROL' | 'NOTIFIED_BODY';

export interface ConformityView {
  id: string;
  tenantId: string;
  reference: string;
  aiSystemId: string;
  qmsId?: string;
  procedure: ConformityProcedure;
  notifiedBodyId?: string;       // 4 digits when NOTIFIED_BODY
  notifiedBodyName?: string;
  scope: string;
  certificateNumber?: string;
  euDeclarationReference?: string;
  validUntil?: string;
  status: ConformityStatus;
  startedAt?: string;
  certifiedAt?: string;
  expiredAt?: string;
  revokedAt?: string;
  revokeReason?: string;
  failedAt?: string;
  failReason?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface PlanRequest {
  reference: string;
  aiSystemId: string;
  qmsId?: string;
  procedure: ConformityProcedure;
  notifiedBodyId?: string;
  notifiedBodyName?: string;
  scope: string;
  createdByUserId: string;
}

export interface EditRequest {
  qmsId?: string;
  notifiedBodyId?: string;
  notifiedBodyName?: string;
  scope: string;
}

export interface CertifyRequest {
  certificateNumber: string;
  euDeclarationReference: string;
  validUntil: string;
}

export interface RevokeRequest { reason: string; }
export interface FailRequest   { reason: string; }
