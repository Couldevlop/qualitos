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

export type CapaActionStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';

export interface CreateCapaActionRequest {
  title: string;
  description?: string;
  status?: CapaActionStatus;
  assigneeId?: string;
  dueDate?: string;
}

/** Mise à jour d'une action (le titre est requis côté backend). §4.2 */
export interface UpdateCapaActionRequest {
  title: string;
  status?: CapaActionStatus;
  description?: string;
  assigneeId?: string;
  dueDate?: string;
}

/** Action corrective/préventive suggérée par l'IA (à valider/ajouter). §4.2 */
export interface SuggestedAction {
  title: string;
  description?: string;
}

export interface UpdateCapaCaseRequest {
  title?: string;
  description?: string;
  criticity?: CapaCriticity;
  sourceRef?: string;
  rootCauseId?: string;
  dueDate?: string;
}
