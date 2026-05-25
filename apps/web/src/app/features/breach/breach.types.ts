export type BreachStatus = 'DETECTED' | 'ASSESSING' | 'CONTAINED' | 'CLOSED' | 'REJECTED';
export type BreachSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface BreachView {
  id: string;
  tenantId: string;
  internalReference: string;
  title: string;
  description?: string | null;
  detectedAt: string;
  occurredAt?: string | null;
  dpaDeadlineAt?: string | null;
  severity: BreachSeverity;
  status: BreachStatus;
  affectedSubjectsCount: number;
  affectedDataCategories?: string[] | null;
  riskOfHarmDescription?: string | null;
  containmentMeasures?: string | null;
  dpaNotifiedAt?: string | null;
  dpaReference?: string | null;
  subjectsNotifiedAt?: string | null;
  subjectsNotificationChannel?: string | null;
  rejectionReason?: string | null;
  closureNotes?: string | null;
  reportedByUserId: string;
  handledByUserId?: string | null;
  closedAt?: string | null;
  updatedAt: string;
  dpaOverdue: boolean;
  subjectNotificationRequired: boolean;
}

export interface BreachDetectRequest {
  internalReference: string;
  title: string;
  description?: string;
  detectedAt: string;
  occurredAt?: string;
  severity: BreachSeverity;
  affectedSubjectsCount: number;
  affectedDataCategories?: string[];
  riskOfHarmDescription?: string;
  reportedByUserId: string;
}

export interface BreachStartAssessmentRequest { handledByUserId: string; }
export interface BreachContainRequest { containmentMeasures: string; handledByUserId?: string; }
export interface BreachDpaNotificationRequest { notifiedAt: string; reference: string; }
export interface BreachSubjectsNotificationRequest { notifiedAt: string; channel: string; }
export interface BreachCloseRequest { closureNotes?: string; }
export interface BreachRejectRequest { reason: string; }
export interface BreachUpdateSeverityRequest { severity: BreachSeverity; }
