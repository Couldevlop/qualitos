import { SpringPage } from '../pdca/pdca.types';

export type ChangeRequestStatus =
  | 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW'
  | 'APPROVED' | 'REJECTED' | 'IMPLEMENTED' | 'CANCELLED';

export type ChangeRequestType =
  | 'DOCUMENT' | 'PROCESS' | 'EQUIPMENT' | 'SUPPLIER'
  | 'IT_SYSTEM' | 'ORGANIZATIONAL' | 'OTHER';

export type ChangeRequestPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type ChangeImpactTargetType =
  | 'DOCUMENT' | 'TRAINING_PATH' | 'SUPPLIER' | 'IOT_DEVICE'
  | 'FMEA_PROJECT' | 'PDCA_CYCLE' | 'STANDARD' | 'OTHER';

export type ApprovalDecision = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface ChangeResponse {
  id: string;
  tenantId: string;
  code: string;
  title: string;
  description?: string;
  type: ChangeRequestType;
  priority: ChangeRequestPriority;
  status: ChangeRequestStatus;
  requesterUserId: string;
  ownerUserId?: string;
  plannedFor?: string;
  implementedAt?: string;
  impactSummary?: string;
  riskAssessment?: string;
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
}

export type ChangePage = SpringPage<ChangeResponse>;

export interface CreateChangeRequest {
  code: string;
  title: string;
  description?: string;
  type: ChangeRequestType;
  priority?: ChangeRequestPriority;
  requesterUserId: string;
  ownerUserId?: string;
  plannedFor?: string;
  impactSummary?: string;
  riskAssessment?: string;
}

export interface UpdateChangeRequest {
  title?: string;
  description?: string;
  priority?: ChangeRequestPriority;
  ownerUserId?: string;
  plannedFor?: string;
  impactSummary?: string;
  riskAssessment?: string;
}

export interface ImplementRequest { implementedAt: string; }

export interface AddApproverRequest {
  approverUserId: string;
  approvalLevel?: number;
}

export interface DecisionRequest {
  approverUserId: string;
  decision: ApprovalDecision;
  comment?: string;
}

export interface ApprovalResponse {
  id: string;
  tenantId: string;
  changeId: string;
  approverUserId: string;
  approvalLevel: number;
  decision: ApprovalDecision;
  comment?: string;
  decidedAt?: string;
  createdAt: string;
}

export interface AddImpactRequest {
  targetType: ChangeImpactTargetType;
  targetId: string;
  notes?: string;
}

export interface ImpactResponse {
  id: string;
  tenantId: string;
  changeId: string;
  targetType: ChangeImpactTargetType;
  targetId: string;
  notes?: string;
  createdAt: string;
}

export interface ChangeSummary {
  changeId: string;
  status: ChangeRequestStatus;
  totalApprovers: number;
  approved: number;
  rejected: number;
  pending: number;
  impactCount: number;
  approvals: ApprovalResponse[];
  impacts: ImpactResponse[];
}
