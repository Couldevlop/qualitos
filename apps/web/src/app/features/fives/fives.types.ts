import { SpringPage } from '../pdca/pdca.types';

export type FiveSAuditStatus = 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type FiveSPillar = 'SEIRI' | 'SEITON' | 'SEISO' | 'SEIKETSU' | 'SHITSUKE';

export interface FiveSItemResponse {
  id: string;
  auditId: string;
  pillar: FiveSPillar;
  score: number;
  note?: string;
  photoUrl?: string;
}

export interface FiveSAuditResponse {
  id: string;
  tenantId: string;
  zone: string;
  description?: string;
  status: FiveSAuditStatus;
  auditorId: string;
  scheduledAt?: string;
  completedAt?: string;
  overallScore?: number;
  createdAt: string;
  updatedAt: string;
  items: FiveSItemResponse[];
}

export type FiveSPage = SpringPage<FiveSAuditResponse>;

export interface CreateFiveSAuditRequest {
  zone: string;
  description?: string;
  auditorId: string;
  scheduledAt?: string;
}
