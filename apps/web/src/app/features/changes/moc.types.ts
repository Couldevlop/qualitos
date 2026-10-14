/**
 * Contrat de `/api/v1/changes` — la gestion des changements (MOC, ADR 0087).
 */

export type MocStatus = 'DRAFT' | 'IN_ANALYSIS' | 'IN_VALIDATION' | 'APPROVED' | 'REJECTED';

export type MocType = 'PERMANENT' | 'TEMPORARY';

export type MocClassification = 'NOT_ASSESSED' | 'MINOR' | 'MAJOR' | 'CRITICAL';

export type ImpactDomain =
  | 'SAFETY' | 'HEALTH_ENVIRONMENT' | 'QUALITY' | 'PRODUCTION'
  | 'MAINTENANCE' | 'COSTS' | 'REGULATORY' | 'TRAINING';

export type ImpactLevel = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH';

export type ValidationDecision = 'PENDING' | 'APPROVED' | 'REJECTED';

export type MocHistoryEvent =
  | 'CREATED' | 'UPDATED' | 'SENT_TO_ANALYSIS' | 'ANALYSIS_COMPLETED' | 'SUBMITTED'
  | 'VALIDATION_APPROVED' | 'VALIDATION_REJECTED' | 'APPROVED' | 'REJECTED' | 'REOPENED'
  | 'REMINDER_SENT' | 'LEGACY_STATUS' | 'LEGACY_DECISION' | 'LEGACY_IMPACT';

/** L'ordre des domaines, celui de l'écran et de l'extraction. */
export const IMPACT_DOMAINS: ImpactDomain[] = [
  'SAFETY', 'HEALTH_ENVIRONMENT', 'QUALITY', 'PRODUCTION', 'MAINTENANCE', 'COSTS', 'REGULATORY', 'TRAINING'
];

export const IMPACT_LEVELS: ImpactLevel[] = ['NONE', 'LOW', 'MEDIUM', 'HIGH'];

/** La frise d'avancement : un rejet se lit sur la dernière étape. */
export const MOC_STEPS: MocStatus[] = ['DRAFT', 'IN_ANALYSIS', 'IN_VALIDATION', 'APPROVED'];

export interface MocSummary {
  id: string;
  code: string;
  title: string;
  type: MocType;
  status: MocStatus;
  classification: MocClassification;
  score: number | null;
  requesterName: string | null;
  createdAt: string;
  awaitingMe: boolean;
}

export interface MocImpact {
  domain: ImpactDomain;
  weight: number;
  level: ImpactLevel | null;
  notes: string | null;
}

export interface MocValidation {
  /** Nul pour une carte d'aperçu (demande pas encore soumise). */
  id: string | null;
  roleLabel: string;
  decision: ValidationDecision;
  decidedByName: string | null;
  decidedAt: string | null;
  comment: string | null;
  holders: string[];
  heldByMe: boolean;
  canDecide: boolean;
  preview: boolean;
}

export interface MocHistoryEntry {
  occurredAt: string;
  event: MocHistoryEvent;
  actorName: string | null;
  detail: string | null;
}

export interface MocDetail {
  id: string;
  code: string;
  title: string;
  type: MocType;
  site: string | null;
  description: string | null;
  justification: string | null;
  plannedFor: string | null;
  status: MocStatus;
  requesterName: string | null;
  requestedByMe: boolean;
  /** L'utilisateur conduit cette demande : son demandeur, ou un gestionnaire des changements. */
  canManage: boolean;
  createdAt: string;
  submittedAt: string | null;
  closedAt: string | null;
  score: number | null;
  classification: MocClassification;
  impacts: MocImpact[];
  validations: MocValidation[];
  history: MocHistoryEntry[];
}

export interface MocRequestForm {
  title: string;
  type: MocType;
  site: string | null;
  description: string | null;
  justification: string | null;
  plannedFor: string | null;
}

export interface MocImpactForm {
  level: ImpactLevel;
  notes: string | null;
}

export interface MocDecisionForm {
  decision: Exclude<ValidationDecision, 'PENDING'>;
  comment: string | null;
}

// ---------- paramètres (administrateur du client) ----------

export interface MocHolder {
  /** Le compte de connexion (`sub` du jeton) du titulaire. */
  userId: string;
  /** Son nom, recopié de l'annuaire (ADR 0091) ; une adresse pour les titulaires nommés avant. */
  label: string;
  /** Son adresse, pour le courriel aux validateurs ; absente pour les titulaires nommés avant. */
  email?: string | null;
}

export interface MocRole {
  id: string | null;
  label: string;
  requiredFor: Exclude<MocClassification, 'NOT_ASSESSED'>[];
  holders: MocHolder[];
}

export interface MocSettings {
  majorThreshold: number;
  criticalThreshold: number;
  reminderDays: number;
  weights: Record<ImpactDomain, number>;
  roles: MocRole[];
}

export const REQUIRED_CLASSES: Exclude<MocClassification, 'NOT_ASSESSED'>[] = ['MINOR', 'MAJOR', 'CRITICAL'];
