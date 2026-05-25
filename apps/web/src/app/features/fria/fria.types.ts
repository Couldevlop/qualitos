export type FriaStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'ARCHIVED';

export interface FriaView {
  id: string;
  tenantId: string;
  reference: string;
  aiSystemId: string;
  processDescription: string;
  deploymentDurationDescription?: string | null;
  affectedPersonsCategories: string;
  specificRisks: string;
  mitigationMeasures?: string | null;
  humanOversightMeasures?: string | null;
  complaintMechanismDescription?: string | null;
  status: FriaStatus;
  submittedAt?: string | null;
  submittedByUserId?: string | null;
  approvedAt?: string | null;
  approvedByUserId?: string | null;
  approvalNotes?: string | null;
  effectiveTo?: string | null;
  archivedReason?: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface FriaDraftRequest {
  reference: string;
  aiSystemId: string;
  processDescription: string;
  deploymentDurationDescription?: string;
  affectedPersonsCategories: string;
  specificRisks: string;
  mitigationMeasures?: string;
  humanOversightMeasures?: string;
  complaintMechanismDescription?: string;
  createdByUserId: string;
}

export interface FriaEditRequest {
  processDescription: string;
  deploymentDurationDescription?: string;
  affectedPersonsCategories: string;
  specificRisks: string;
  mitigationMeasures?: string;
  humanOversightMeasures?: string;
  complaintMechanismDescription?: string;
}

export interface FriaSubmitRequest { submittedByUserId: string; }
export interface FriaApproveRequest { approvedByUserId: string; approvalNotes?: string; }
export interface FriaReturnRequest { reason: string; }
export interface FriaArchiveRequest { reason: string; }
