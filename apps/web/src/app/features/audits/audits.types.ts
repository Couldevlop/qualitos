import { SpringPage } from '../pdca/pdca.types';

export type AuditType = 'INTERNAL' | 'EXTERNAL' | 'SUPPLIER' | 'LPA' | 'CERTIFICATION' | 'SURVEILLANCE';
export type AuditStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type FindingType = 'CONFORMITY' | 'MINOR_NC' | 'MAJOR_NC' | 'OBSERVATION' | 'OPPORTUNITY';

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
}

export type AuditsPage = SpringPage<AuditPlanResponse>;
