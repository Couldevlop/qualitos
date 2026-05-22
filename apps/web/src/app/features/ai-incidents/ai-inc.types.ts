/**
 * AI Act — Incidents (Art. 73).
 * Backend /api/v1/ai-act/incidents.
 * Délais de notification régulateur :
 *  - DEATH_OR_SERIOUS_HARM_TO_HEALTH : 2 jours
 *  - SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS : 10 jours
 *  - CRITICAL_INFRASTRUCTURE_DISRUPTION : 15 jours
 *  - SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE : 15 jours
 */

export type AiIncStatus = 'DETECTED' | 'INVESTIGATING' | 'NOTIFIED_REGULATOR' | 'CLOSED' | 'DISMISSED';

export type AiIncSeverity =
  | 'DEATH_OR_SERIOUS_HARM_TO_HEALTH'
  | 'SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS'
  | 'CRITICAL_INFRASTRUCTURE_DISRUPTION'
  | 'SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE';

export interface AiIncView {
  id: string;
  tenantId: string;
  reference: string;
  aiSystemId: string;
  severity: AiIncSeverity;
  description: string;
  affectedPersonsDescription?: string;
  immediateActionsTaken?: string;
  occurredAt: string;
  detectedAt: string;
  investigationStartedAt?: string;
  investigationLeadUserId?: string;
  regulatorNotifiedAt?: string;
  regulatorReference?: string;
  rootCauseAnalysis?: string;
  correctiveActions?: string;
  closedAt?: string;
  dismissedAt?: string;
  dismissReason?: string;
  status: AiIncStatus;
  overdueForRegulator: boolean;
  regulatorNotificationDeadline?: string;  // computed by backend
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface DetectRequest {
  reference: string;
  aiSystemId: string;
  severity: AiIncSeverity;
  description: string;
  affectedPersonsDescription?: string;
  immediateActionsTaken?: string;
  occurredAt: string;
  detectedAt: string;
  createdByUserId: string;
}

export interface EditRequest {
  description: string;
  affectedPersonsDescription?: string;
  immediateActionsTaken?: string;
}

export interface StartInvestigationRequest { investigationLeadUserId: string; }

export interface NotifyRegulatorRequest {
  regulatorReference: string;
  rootCauseAnalysis: string;
  correctiveActions?: string;
}

export interface CloseRequest { correctiveActions: string; }
export interface DismissRequest { reason: string; }

/** Délai légal en jours par sévérité (Art. 73). */
export const SEVERITY_DEADLINE_DAYS: Record<AiIncSeverity, number> = {
  DEATH_OR_SERIOUS_HARM_TO_HEALTH: 2,
  SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS: 10,
  CRITICAL_INFRASTRUCTURE_DISRUPTION: 15,
  SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE: 15
};
