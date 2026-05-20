import { SpringPage } from '../pdca/pdca.types';

export type AuditType = 'INTERNAL' | 'EXTERNAL' | 'SUPPLIER' | 'LPA' | 'CERTIFICATION' | 'SURVEILLANCE';
export type AuditStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type FindingType = 'CONFORMITY' | 'MINOR_NC' | 'MAJOR_NC' | 'OBSERVATION' | 'OPPORTUNITY';

export interface ChecklistItemResponse {
  id: string;
  planId: string;
  question: string;
  clauseRef?: string;
  expectedEvidence?: string;
  weight?: number;
  orderIndex?: number;
  response?: string;
  conformant?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FindingResponse {
  id: string;
  planId: string;
  checklistItemId?: string;
  type: FindingType;
  description: string;
  clauseRef?: string;
  photoUrl?: string;
  capaId?: string;
  raisedBy: string;
  raisedAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface AuditPlanResponse {
  id: string;
  tenantId: string;
  title: string;
  scope?: string;
  type: AuditType;
  status: AuditStatus;
  standard?: string;
  leadAuditorId: string;
  auditeeId?: string;
  scheduledDate?: string;
  startedAt?: string;
  completedAt?: string;
  reportSummary?: string;
  conformityScore?: number;
  createdAt: string;
  updatedAt: string;
  /** Server includes both on GET /plans/{id}. List view may omit them. */
  checklist?: ChecklistItemResponse[];
  findings?: FindingResponse[];
}

export type AuditsPage = SpringPage<AuditPlanResponse>;

export interface CreateAuditPlanRequest {
  title: string;
  scope?: string;
  type: AuditType;
  standard?: string;
  leadAuditorId: string;
  scheduledDate?: string;
}

export interface CreateChecklistItemRequest {
  question: string;
  clauseRef?: string;
  expectedEvidence?: string;
  weight?: number;
  orderIndex?: number;
}
