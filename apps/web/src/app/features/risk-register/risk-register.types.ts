/**
 * Le registre des risques et opportunités (ISO 9001 §6.1).
 *
 * <p>Scores et niveaux viennent du serveur : l'écran les affiche, il ne les
 * recalcule pas. Un niveau « Critique » présenté en revue de direction ne doit
 * pas dépendre du navigateur qui le montre.
 */

export type RegisterType = 'QUALITY' | 'ENVIRONMENT' | 'HEALTH_SAFETY' | 'INFORMATION_SECURITY' | 'LEGAL';

export type RegisterOrigin =
  | 'DIRECT' | 'MANAGEMENT_REVIEW' | 'AUDIT' | 'CUSTOMER_FEEDBACK' | 'MONITORING'
  | 'FMEA' | 'NON_CONFORMITY' | 'INCIDENT' | 'CHANGE';

/** Les origines qui pointent un objet réel de la plateforme, vérifié par le serveur. */
export type RiskSourceOrigin = 'FMEA' | 'NON_CONFORMITY' | 'AUDIT' | 'CHANGE';

export type RegisterRequirement =
  | 'ISO_9001_6_1' | 'ISO_9001_10_3' | 'ISO_14001_6_1' | 'ISO_45001_6_1' | 'IATF_16949_6_1_2';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type OpportunityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'PRIORITY';
export type RiskDecision = 'UNDECIDED' | 'REDUCE' | 'ACCEPT' | 'AVOID' | 'TRANSFER';
export type RiskStatus = 'TO_TREAT' | 'IN_TREATMENT' | 'MONITORED' | 'ACCEPTED' | 'CLOSED';
export type OpportunityDecision = 'UNDECIDED' | 'PLAN' | 'STUDY' | 'POSTPONE' | 'DISCARD';
export type OpportunityStatus = 'UNDER_STUDY' | 'PLANNED' | 'IN_PROGRESS' | 'DONE' | 'DISCARDED';
export type OpportunityActionStatus = 'TO_START' | 'IN_PROGRESS' | 'DONE' | 'CANCELLED';
export type RegisterEventType =
  | 'CREATED' | 'RATING_CHANGED' | 'RESIDUAL_CHANGED' | 'STATUS_CHANGED' | 'DECISION_CHANGED'
  | 'ACTION_OPENED';

export interface RiskView {
  id: string;
  reference: string;
  title: string;
  type: RegisterType;
  process: string;
  site: string | null;
  owner: string;
  cause: string | null;
  effect: string | null;
  origin: RegisterOrigin;
  originRef: string | null;
  sourceId: string | null;
  grossSeverity: number;
  grossProbability: number;
  grossScore: number;
  grossLevel: RiskLevel;
  residualSeverity: number | null;
  residualProbability: number | null;
  residualScore: number | null;
  residualLevel: RiskLevel | null;
  decision: RiskDecision;
  status: RiskStatus;
  requirements: RegisterRequirement[];
  nextReviewOn: string | null;
  effectivenessCriterion: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CapaLink {
  id: string;
  title: string;
  dueDate: string | null;
  status: string;
}

export interface RegisterEvent {
  id: string;
  type: RegisterEventType;
  fromValue: string | null;
  toValue: string | null;
  detail: string | null;
  at: string;
}

export interface RiskSheet {
  risk: RiskView;
  capas: CapaLink[];
  events: RegisterEvent[];
}

export interface OpportunityView {
  id: string;
  reference: string;
  title: string;
  type: RegisterType;
  process: string;
  site: string | null;
  owner: string;
  targetDate: string | null;
  context: string | null;
  benefit: string | null;
  origin: RegisterOrigin;
  originRef: string | null;
  gain: number;
  feasibility: number;
  score: number;
  level: OpportunityLevel;
  decision: OpportunityDecision;
  status: OpportunityStatus;
  requirements: RegisterRequirement[];
  benefitCriterion: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OpportunityAction {
  id: string;
  number: number;
  title: string;
  dueDate: string | null;
  status: OpportunityActionStatus;
}

export interface OpportunitySheet {
  opportunity: OpportunityView;
  actions: OpportunityAction[];
  events: RegisterEvent[];
}

export interface RegisterSuggestions {
  processes: string[];
  sites: string[];
  owners: string[];
}

/** Ce que le serveur propose de pré-remplir depuis un objet source (AMDEC, NC, audit, changement). */
export interface RiskDraft {
  origin: RiskSourceOrigin;
  sourceId: string;
  originRef: string;
  title: string;
  cause: string | null;
  effect: string | null;
  grossSeverity: number | null;
  grossProbability: number | null;
  process: string | null;
  /** Faux quand l'objet ne justifie pas un risque (ligne d'AMDEC sous le seuil, constat conforme). */
  eligible: boolean;
  /** Pourquoi il n'est pas éligible, en code : BELOW_THRESHOLD, NOT_A_GAP. */
  reason: string | null;
  /** Risques déjà créés depuis cette source : l'écran les montre plutôt que d'en créer un doublon. */
  existing: { id: string; reference: string }[];
}

export interface RiskRequest {
  title: string;
  type: RegisterType;
  process: string;
  site?: string | null;
  owner: string;
  cause?: string | null;
  effect?: string | null;
  origin: RegisterOrigin;
  originRef?: string | null;
  sourceId?: string | null;
  grossSeverity: number;
  grossProbability: number;
  residualSeverity?: number | null;
  residualProbability?: number | null;
  decision: RiskDecision;
  status?: RiskStatus;
  requirements: RegisterRequirement[];
  nextReviewOn?: string | null;
  effectivenessCriterion?: string | null;
}

export interface OpportunityRequest {
  title: string;
  type: RegisterType;
  process: string;
  site?: string | null;
  owner: string;
  targetDate?: string | null;
  context?: string | null;
  benefit?: string | null;
  origin: RegisterOrigin;
  originRef?: string | null;
  gain: number;
  feasibility: number;
  decision: OpportunityDecision;
  status?: OpportunityStatus;
  requirements: RegisterRequirement[];
  benefitCriterion?: string | null;
}

export interface CapaRequest {
  title: string;
  description?: string | null;
  dueDate?: string | null;
}

export interface ActionRequest {
  title: string;
  dueDate?: string | null;
  status?: OpportunityActionStatus;
}
