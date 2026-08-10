import { SpringPage } from '../pdca/pdca.types';

export type AuditType = 'INTERNAL' | 'EXTERNAL' | 'SUPPLIER' | 'LPA' | 'CERTIFICATION' | 'SURVEILLANCE';
export type AuditStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type FindingType = 'CONFORMITY' | 'MINOR_NC' | 'MAJOR_NC' | 'OBSERVATION' | 'OPPORTUNITY';

export interface ChecklistItemResponse {
  id: string;
  planId: string;
  question: string;
  clauseRef?: string;
  expectedEvidence?: string;
  weight?: number;
  orderIndex?: number;
  response?: string;
  conformant?: boolean;
  createdAt: string;
  updatedAt: string;
  /** true = répondu hors-ligne, en attente de synchronisation (réponse optimiste). */
  pendingSync?: boolean;
}

export interface FindingResponse {
  id: string;
  planId: string;
  checklistItemId?: string;
  type: FindingType;
  description: string;
  clauseRef?: string;
  photoUrl?: string;
  capaId?: string;
  raisedBy: string;
  raisedAt: string;
  createdAt: string;
  updatedAt: string;
  /** true = déclaré hors-ligne, en attente de synchronisation (réponse optimiste). */
  pendingSync?: boolean;
}

export interface AuditPlanResponse {
  id: string;
  tenantId: string;
  title: string;
  scope?: string;
  type: AuditType;
  status: AuditStatus;
  standard?: string;
  leadAuditorId: string;
  auditeeId?: string;
  scheduledDate?: string;
  startedAt?: string;
  completedAt?: string;
  reportSummary?: string;
  conformityScore?: number;
  /** Destinataire du rappel d'échéance par courriel. Vide = rappel interne seul. */
  reminderEmail?: string;
  /** Horodatage du rappel déjà parti. Sert de marque d'idempotence côté serveur. */
  reminderSentAt?: string;
  createdAt: string;
  updatedAt: string;
  /** Server includes both on GET /plans/{id}. List view may omit them. */
  checklist?: ChecklistItemResponse[];
  findings?: FindingResponse[];
}

export type AuditsPage = SpringPage<AuditPlanResponse>;

export interface CreateAuditPlanRequest {
  title: string;
  scope?: string;
  type: AuditType;
  standard?: string;
  leadAuditorId: string;
  scheduledDate?: string;
  reminderEmail?: string;
}

/**
 * Ligne du planning des audits (§4.4).
 *
 * `daysUntil` vient du SERVEUR et n'est pas recalculé ici : le déduire de
 * l'horloge du poste ferait dépendre « J-30 » du fuseau et de l'heure locale, et
 * deux utilisateurs verraient deux échéances pour le même audit. Négatif = retard.
 */
export interface AuditPlanningEntry {
  id: string;
  title: string;
  type: AuditType;
  status: AuditStatus;
  standard?: string;
  leadAuditorId: string;
  scheduledDate: string;
  daysUntil: number;
  overdue: boolean;
  reminderSent: boolean;
}

export interface CreateChecklistItemRequest {
  question: string;
  clauseRef?: string;
  expectedEvidence?: string;
  weight?: number;
  orderIndex?: number;
}

export interface ChecklistResponseRequest {
  response?: string;
  conformant?: boolean;
}

export interface UpdateAuditPlanRequest {
  title?: string;
  scope?: string;
  type?: AuditType;
  standard?: string;
  leadAuditorId?: string;
  auditeeId?: string;
  scheduledDate?: string;
  /** Chaîne vide = retirer le destinataire ; absent = ne pas y toucher. */
  reminderEmail?: string;
}

export interface AddFindingRequest {
  type: FindingType;
  description: string;
  clauseRef?: string;
  photoUrl?: string;
  checklistItemId?: string;
  capaId?: string;
  raisedBy: string;
}
