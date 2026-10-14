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
  /** Auteur, lu par le serveur dans le jeton. Absent d'une réponse optimiste hors ligne. */
  raisedBy?: string;
  raisedAt: string;
  createdAt: string;
  updatedAt: string;
  /** true = déclaré hors-ligne, en attente de synchronisation (réponse optimiste). */
  pendingSync?: boolean;
}

export interface AuditPlanResponse {
  id: string;
  /** Désignation citable — `AUD-2026-0001`. Immuable une fois attribuée. */
  reference: string;
  tenantId: string;
  title: string;
  scope?: string;
  type: AuditType;
  status: AuditStatus;
  standard?: string;
  /**
   * Référentiel du catalogue dont la checklist a été tirée (§8), s'il y en a un.
   * Distinct de `standard`, texte libre : c'est lui qui dit qu'un référentiel
   * réel est derrière cet audit.
   */
  standardId?: string;
  leadAuditorId: string;
  /**
   * Nom de l'auditeur responsable, recopié à la désignation. Absent sur les
   * audits antérieurs : l'écran ne montre alors jamais l'identifiant brut.
   */
  leadAuditorName?: string;
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
  /** Absent = l'utilisateur connecté (le serveur le lit dans le jeton). */
  leadAuditorId?: string;
  /** Nom affiché par l'annuaire ; ignoré par le serveur quand l'auditeur est soi-même. */
  leadAuditorName?: string;
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
  reference: string;
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
  /** Lu seulement quand `leadAuditorId` change. */
  leadAuditorName?: string;
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
  // Pas d'auteur : le serveur le lit dans le jeton (§18.2 #2), jamais dans le corps.
}

// ---- rapport d'audit -------------------------------------------------------

export type AuditReportStatus = 'DRAFT' | 'ISSUED';
export type AuditConclusion = 'FAVORABLE' | 'FAVORABLE_WITH_RESERVATIONS' | 'UNFAVORABLE';

/** Nombre de constats par type ; calculés en direct sur un brouillon, figés à l'émission. */
export interface AuditReportKeyFigures {
  majorNc: number;
  minorNc: number;
  observation: number;
  opportunity: number;
  conformity: number;
  total: number;
  conformityScore?: number | null;
}

export interface AuditReport {
  /** null tant qu'aucun brouillon n'a été enregistré. */
  id: string | null;
  planId: string;
  status: AuditReportStatus;
  summary?: string | null;
  strengths?: string | null;
  improvements?: string | null;
  conclusion?: AuditConclusion | null;
  keyFigures: AuditReportKeyFigures;
  issuedAt?: string | null;
  issuedBy?: string | null;
  issuedByName?: string | null;
  contentSha256?: string | null;
  updatedAt?: string | null;
}

/** Remplacement complet du brouillon : un champ vidé se vide. */
export interface AuditReportDraftRequest {
  summary: string | null;
  strengths: string | null;
  improvements: string | null;
  conclusion: AuditConclusion | null;
}

// ---- documents joints ------------------------------------------------------

export interface AuditDocument {
  id: string;
  planId: string;
  contentType: string;
  sizeBytes: number;
  originalFilename?: string | null;
  uploadedBy?: string | null;
  createdAt: string;
  /** URL présignée, de courte durée : elle ne se conserve pas. */
  url?: string | null;
}
