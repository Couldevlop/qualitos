import { SpringPage } from '../pdca/pdca.types';

export type FmeaStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type FmeaType =
  | 'PROCESS_FMEA'
  | 'DESIGN_FMEA'
  | 'SYSTEM_FMEA'
  | 'SERVICE_FMEA'
  | 'BOW_TIE';

export interface FmeaProjectResponse {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  scope?: string;
  type: FmeaType;
  status: FmeaStatus;
  criticalRpnThreshold: number;
  revision: number;
  ownerUserId?: string;
  lastReviewedAt?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export type FmeaProjectPage = SpringPage<FmeaProjectResponse>;

export interface CreateFmeaProjectRequest {
  code: string;
  name: string;
  scope?: string;
  type: FmeaType;
  criticalRpnThreshold?: number;
  ownerUserId?: string;
  createdBy: string;
}

export interface UpdateFmeaProjectRequest {
  name?: string;
  scope?: string;
  criticalRpnThreshold?: number;
  ownerUserId?: string;
}

export interface FmeaItemResponse {
  id: string;
  tenantId: string;
  projectId: string;
  sequenceNo: number;
  function?: string;
  failureMode?: string;
  failureEffect?: string;
  failureCause?: string;
  currentControls?: string;
  severity: number;
  occurrence: number;
  detection: number;
  rpn: number;
  recommendedAction?: string;
  actionOwnerUserId?: string;
  actionDueDate?: string;
  resultingSeverity?: number;
  resultingOccurrence?: number;
  resultingDetection?: number;
  rpnAfter?: number;
  critical: boolean;
  createdAt: string;
  updatedAt: string;
}

export type FmeaItemPage = SpringPage<FmeaItemResponse>;

export interface CreateFmeaItemRequest {
  function?: string;
  failureMode?: string;
  failureEffect?: string;
  failureCause?: string;
  currentControls?: string;
  severity: number;
  occurrence: number;
  detection: number;
  recommendedAction?: string;
  actionOwnerUserId?: string;
  actionDueDate?: string;
  resultingSeverity?: number;
  resultingOccurrence?: number;
  resultingDetection?: number;
}

export interface UpdateFmeaItemRequest {
  function?: string;
  failureMode?: string;
  failureEffect?: string;
  failureCause?: string;
  currentControls?: string;
  severity?: number;
  occurrence?: number;
  detection?: number;
  recommendedAction?: string;
  actionOwnerUserId?: string;
  actionDueDate?: string;
  resultingSeverity?: number;
  resultingOccurrence?: number;
  resultingDetection?: number;
}

export interface FmeaProjectStatistics {
  projectId: string;
  totalItems: number;
  criticalItems: number;
  maxRpn: number;
  averageRpn: number;
  criticalRpnThreshold: number;
}
