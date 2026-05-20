export type PdcaStatus = 'PLAN' | 'DO' | 'CHECK' | 'ACT' | 'COMPLETED' | 'CANCELLED';
export type PdcaPhase = 'PLAN' | 'DO' | 'CHECK' | 'ACT';
export type StepStatus = 'PENDING' | 'IN_PROGRESS' | 'DONE';

export interface CreatePdcaCycleRequest {
  title: string;
  description?: string;
  ownerId: string;
}

export interface CreatePdcaStepRequest {
  title: string;
  description?: string;
  phase: PdcaPhase;
  status?: StepStatus;
  assigneeId?: string;
  dueDate?: string;
}

export interface PdcaStepResponse {
  id: string;
  cycleId: string;
  phase: PdcaPhase;
  title: string;
  description?: string;
  status: StepStatus;
  assigneeId?: string;
  dueDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PdcaCycleResponse {
  id: string;
  tenantId: string;
  title: string;
  description?: string;
  status: PdcaStatus;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  steps: PdcaStepResponse[];
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
