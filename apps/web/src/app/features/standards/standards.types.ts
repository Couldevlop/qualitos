import { SpringPage } from '../pdca/pdca.types';

export type StandardStatus = 'PUBLISHED' | 'DEPRECATED' | 'WITHDRAWN';
export type AdoptionStatus = 'PLANNING' | 'IN_PROGRESS' | 'CERTIFIED' | 'SURVEILLANCE' | 'EXPIRED' | 'WITHDRAWN';

export interface StandardSummary {
  id: string;
  code: string;
  fullName: string;
  publisher?: string;
  currentVersion: string;
  family?: string;
  applicableIndustries?: string;
  status: StandardStatus;
  recertificationCycleMonths?: number;
}

export interface AdoptionResponse {
  id: string;
  tenantId: string;
  standardId: string;
  standardCode: string;
  standardName: string;
  status: AdoptionStatus;
  scopeDescription?: string;
  targetCertificationDate?: string;
  certificationBody?: string;
  certifiedAt?: string;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
}

export type StandardsPage = SpringPage<StandardSummary>;
export type AdoptionsPage = SpringPage<AdoptionResponse>;
