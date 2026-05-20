import { SpringPage } from '../pdca/pdca.types';

export type CapaType = 'CORRECTIVE' | 'PREVENTIVE';
export type CapaCriticity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type CapaStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED';
export type CapaSourceType = 'NON_CONFORMITY' | 'AUDIT' | 'COMPLAINT' | 'INTERNAL' | 'IOT_ALERT' | 'OTHER';

export interface CapaActionResponse {
  id: string;
  capaId: string;
  title: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'DONE';
  assigneeId?: string;
  dueDate?: string;
  completedAt?: string;
}

export interface CapaCaseResponse {
  id: string;
  tenantId: string;
  title: string;
  description?: string;
  type: CapaType;
  criticity: CapaCriticity;
  status: CapaStatus;
  sourceType: CapaSourceType;
  sourceRef?: string;
  ownerId: string;
  rootCauseId?: string;
  dueDate?: string;
  resolvedAt?: string;
  closedAt?: string;
  effectivenessVerified?: boolean;
  createdAt: string;
  updatedAt: string;
  actions: CapaActionResponse[];
}

export type CapaPage = SpringPage<CapaCaseResponse>;

export interface CreateCapaCaseRequest {
  title: string;
  description?: string;
  type: CapaType;
  criticity: CapaCriticity;
  sourceType: CapaSourceType;
  sourceRef?: string;
  ownerId: string;
  dueDate?: string;
}

export interface CreateCapaActionRequest {
  title: string;
  description?: string;
  status?: 'PENDING' | 'IN_PROGRESS' | 'DONE';
  assigneeId?: string;
  dueDate?: string;
}

export interface UpdateCapaCaseRequest {
  title?: string;
  description?: string;
  criticity?: CapaCriticity;
  sourceRef?: string;
  rootCauseId?: string;
  dueDate?: string;
}
