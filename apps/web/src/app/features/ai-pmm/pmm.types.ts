export type PmmPlanStatus = 'DRAFT' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
export type PmmReviewFrequency = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL';

export const FREQUENCY_LABEL: Record<PmmReviewFrequency, string> = {
  WEEKLY:      'Hebdomadaire (7j)',
  MONTHLY:     'Mensuelle (30j)',
  QUARTERLY:   'Trimestrielle (90j)',
  SEMI_ANNUAL: 'Semestrielle (182j)',
  ANNUAL:      'Annuelle (365j)'
};

export const FREQUENCY_DAYS: Record<PmmReviewFrequency, number> = {
  WEEKLY: 7, MONTHLY: 30, QUARTERLY: 90, SEMI_ANNUAL: 182, ANNUAL: 365
};

export interface PmmPlanView {
  id: string;
  tenantId: string;
  reference: string;
  aiSystemId: string;
  name: string;
  description?: string | null;
  metricsMonitored?: string | null;
  collectionMethod?: string | null;
  reviewFrequency: PmmReviewFrequency;
  responsiblePartyDescription?: string | null;
  triggerCriteria?: string | null;
  qmsLinkReference?: string | null;
  status: PmmPlanStatus;
  activatedAt?: string | null;
  lastReviewedAt?: string | null;
  lastReviewedByUserId?: string | null;
  nextReviewDueAt?: string | null;
  suspendedAt?: string | null;
  suspensionReason?: string | null;
  effectiveTo?: string | null;
  closureReason?: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface PmmDraftRequest {
  reference: string;
  aiSystemId: string;
  name: string;
  description?: string;
  metricsMonitored?: string;
  collectionMethod?: string;
  reviewFrequency: PmmReviewFrequency;
  responsiblePartyDescription?: string;
  triggerCriteria?: string;
  qmsLinkReference?: string;
  createdByUserId: string;
}

export interface PmmEditRequest {
  name: string;
  description?: string;
  metricsMonitored?: string;
  collectionMethod?: string;
  reviewFrequency: PmmReviewFrequency;
  responsiblePartyDescription?: string;
  triggerCriteria?: string;
  qmsLinkReference?: string;
}

export interface PmmReviewRequest { reviewedByUserId: string; }
export interface PmmSuspendRequest { reason: string; }
export interface PmmCloseRequest { reason: string; }
