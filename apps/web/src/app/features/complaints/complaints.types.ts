/**
 * Réclamations clients / Voice of Customer (§4.9).
 *
 * Miroir strict du contrat serveur `/api/v1/complaints` (ComplaintController,
 * ComplaintDto, ComplaintService) : aucun champ, aucune route et aucune
 * transition n'est inventé ici. Les 15 routes existaient sans écran — ce module
 * est leur premier consommateur.
 *
 * L'analyse NLP des réclamations vit à part (/complaints-nlp) : ici on gère le
 * cycle de vie opérationnel (saisie, triage, réponses, clôture, satisfaction).
 */

/**
 * Cycle de vie (ComplaintStatus).
 *
 * `REOPENED` fait partie de l'énumération serveur mais n'est jamais persisté :
 * `reopen()` repositionne directement la réclamation en UNDER_INVESTIGATION.
 * On le conserve dans le type pour rester fidèle au contrat et pour ne pas
 * casser l'affichage si une donnée historique le porte.
 */
export type ComplaintStatus =
  | 'RECEIVED'
  | 'UNDER_INVESTIGATION'
  | 'RESPONDED'
  | 'RESOLVED'
  | 'CLOSED'
  | 'REJECTED'
  | 'REOPENED';

export type ComplaintChannel =
  | 'EMAIL' | 'PHONE' | 'WEB_FORM' | 'IN_PERSON' | 'SOCIAL'
  | 'ITSM_IMPORT' | 'API' | 'POSTAL' | 'OTHER';

export type ComplaintCategory =
  | 'PRODUCT' | 'SERVICE' | 'DELIVERY' | 'BILLING' | 'QUALITY' | 'SAFETY' | 'OTHER';

export type ComplaintSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export const COMPLAINT_STATUSES: ComplaintStatus[] = [
  'RECEIVED', 'UNDER_INVESTIGATION', 'RESPONDED', 'RESOLVED', 'CLOSED', 'REJECTED', 'REOPENED'
];

export const COMPLAINT_CHANNELS: ComplaintChannel[] = [
  'EMAIL', 'PHONE', 'WEB_FORM', 'IN_PERSON', 'SOCIAL', 'ITSM_IMPORT', 'API', 'POSTAL', 'OTHER'
];

export const COMPLAINT_CATEGORIES: ComplaintCategory[] = [
  'PRODUCT', 'SERVICE', 'DELIVERY', 'BILLING', 'QUALITY', 'SAFETY', 'OTHER'
];

export const COMPLAINT_SEVERITIES: ComplaintSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export const COMPLAINT_STATUS_LABEL: Record<ComplaintStatus, string> = {
  RECEIVED: $localize`:@@complaints.status.received:Reçue`,
  UNDER_INVESTIGATION: $localize`:@@complaints.status.under-investigation:En investigation`,
  RESPONDED: $localize`:@@complaints.status.responded:Répondue`,
  RESOLVED: $localize`:@@complaints.status.resolved:Résolue`,
  CLOSED: $localize`:@@complaints.status.closed:Clôturée`,
  REJECTED: $localize`:@@complaints.status.rejected:Rejetée`,
  REOPENED: $localize`:@@complaints.status.reopened:Rouverte`
};

export const COMPLAINT_CHANNEL_LABEL: Record<ComplaintChannel, string> = {
  EMAIL: $localize`:@@complaints.channel.email:E-mail`,
  PHONE: $localize`:@@complaints.channel.phone:Téléphone`,
  WEB_FORM: $localize`:@@complaints.channel.web-form:Formulaire web`,
  IN_PERSON: $localize`:@@complaints.channel.in-person:En personne`,
  SOCIAL: $localize`:@@complaints.channel.social:Réseaux sociaux`,
  ITSM_IMPORT: $localize`:@@complaints.channel.itsm-import:Import ITSM`,
  API: $localize`:@@complaints.channel.api:API`,
  POSTAL: $localize`:@@complaints.channel.postal:Courrier`,
  OTHER: $localize`:@@complaints.channel.other:Autre`
};

export const COMPLAINT_CATEGORY_LABEL: Record<ComplaintCategory, string> = {
  PRODUCT: $localize`:@@complaints.category.product:Produit`,
  SERVICE: $localize`:@@complaints.category.service:Service`,
  DELIVERY: $localize`:@@complaints.category.delivery:Livraison`,
  BILLING: $localize`:@@complaints.category.billing:Facturation`,
  QUALITY: $localize`:@@complaints.category.quality:Qualité`,
  SAFETY: $localize`:@@complaints.category.safety:Sécurité`,
  OTHER: $localize`:@@complaints.category.other:Autre`
};

export const COMPLAINT_SEVERITY_LABEL: Record<ComplaintSeverity, string> = {
  LOW: $localize`:@@complaints.severity.low:Faible`,
  MEDIUM: $localize`:@@complaints.severity.medium:Moyenne`,
  HIGH: $localize`:@@complaints.severity.high:Élevée`,
  CRITICAL: $localize`:@@complaints.severity.critical:Critique`
};

/**
 * Les références échangées avec le serveur (fournisseur, dossier CAPA,
 * utilisateur assigné) sont des UUID : un libellé saisi à la main partirait en
 * 400. On valide donc la forme avant d'émettre la requête.
 */
export const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function isUuid(value: string | null | undefined): boolean {
  return !!value && UUID_PATTERN.test(value.trim());
}

/** Réclamation telle que renvoyée par le serveur (ComplaintDto.ComplaintResponse). */
export interface Complaint {
  id: string;
  tenantId: string;
  code: string;
  channel: ComplaintChannel;
  customerName: string | null;
  customerEmail: string | null;
  customerExternalId: string | null;
  subject: string;
  description: string | null;
  severity: ComplaintSeverity;
  category: ComplaintCategory;
  status: ComplaintStatus;
  supplierId: string | null;
  capaCaseId: string | null;
  assignedToUserId: string | null;
  /** Note de satisfaction 0..10, saisissable une fois la réclamation résolue ou clôturée. */
  satisfactionScore: number | null;
  receivedAt: string | null;
  /** Horodatage du premier message NON interne : prérequis serveur à la résolution. */
  firstResponseAt: string | null;
  resolvedAt: string | null;
  closedAt: string | null;
  rejectionReason: string | null;
  createdBy: string;
  createdAt: string | null;
  updatedAt: string | null;
}

/** Message du fil de réponses (ComplaintDto.ResponseEntryResponse). */
export interface ComplaintResponseEntry {
  id: string;
  tenantId: string;
  complaintId: string;
  authorUserId: string;
  channel: ComplaintChannel;
  body: string;
  /** Une note interne n'est pas adressée au client ; elle seule est supprimable. */
  internalNote: boolean;
  sentAt: string | null;
  createdAt: string | null;
}

/** Compteurs de l'écran (ComplaintDto.ComplaintStatistics). */
export interface ComplaintStatistics {
  tenantId: string;
  total: number;
  received: number;
  underInvestigation: number;
  responded: number;
  resolved: number;
  closed: number;
  rejected: number;
  product: number;
  service: number;
  delivery: number;
  billing: number;
  quality: number;
  safety: number;
  other: number;
}

/** Enveloppe `Page<T>` de Spring Data telle que sérialisée par Jackson. */
export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateComplaintRequest {
  code: string;
  channel: ComplaintChannel;
  customerName: string | null;
  customerEmail: string | null;
  customerExternalId: string | null;
  subject: string;
  description: string | null;
  severity: ComplaintSeverity;
  category: ComplaintCategory;
  supplierId: string | null;
  assignedToUserId: string | null;
  /** Identité de l'auteur : obligatoire côté serveur (@NotNull UUID createdBy). */
  createdBy: string;
  receivedAt: string | null;
}

/**
 * Mise à jour partielle : le serveur n'applique QUE les champs non nuls
 * (`if (req.x() != null)`), un `null` signifie donc « inchangé » et non « vider ».
 */
export interface UpdateComplaintRequest {
  customerName?: string | null;
  customerEmail?: string | null;
  customerExternalId?: string | null;
  subject?: string | null;
  description?: string | null;
  severity?: ComplaintSeverity | null;
  category?: ComplaintCategory | null;
  supplierId?: string | null;
  assignedToUserId?: string | null;
}

export interface AddResponseRequest {
  authorUserId: string;
  /** `null` ⇒ le serveur reprend le canal de la réclamation. */
  channel: ComplaintChannel | null;
  body: string;
  internalNote: boolean;
}

/**
 * Critères de la liste.
 *
 * Le serveur n'honore QU'UN filtre à la fois, par précédence
 * status > category > supplierId (ComplaintService.list) : l'écran n'en propose
 * donc qu'un seul à la fois, pour ne jamais afficher un filtre silencieusement ignoré.
 */
export interface ComplaintListCriteria {
  status?: ComplaintStatus;
  category?: ComplaintCategory;
  supplierId?: string;
  page: number;
  size: number;
}

/** Vue consolidée de la liste : page de résultats + compteurs. */
export interface ComplaintOverview {
  page: SpringPage<Complaint>;
  statistics: ComplaintStatistics;
}

/** Vue consolidée du détail : réclamation + fil de réponses. */
export interface ComplaintDetail {
  complaint: Complaint;
  responses: ComplaintResponseEntry[];
}

// ---------------------------------------------------------------------------
// Règles de cycle de vie — miroir de ComplaintService (table ALLOWED + gardes).
//
// Elles servent à n'AFFICHER une action que si le serveur l'accepterait : un
// bouton présent doit aboutir, sinon l'utilisateur essuie un 409 gratuit.
// ---------------------------------------------------------------------------

/** CLOSED et REJECTED sont terminaux : plus aucune écriture métier n'est admise. */
export function isTerminalStatus(status: ComplaintStatus): boolean {
  return status === 'CLOSED' || status === 'REJECTED';
}

export function canEditComplaint(status: ComplaintStatus): boolean {
  return !isTerminalStatus(status);
}

export function canAssignComplaint(status: ComplaintStatus): boolean {
  return !isTerminalStatus(status);
}

export function canRespondToComplaint(status: ComplaintStatus): boolean {
  return !isTerminalStatus(status);
}

/** REJECTED n'est atteignable que depuis RECEIVED ou UNDER_INVESTIGATION. */
export function canRejectComplaint(status: ComplaintStatus): boolean {
  return status === 'RECEIVED' || status === 'UNDER_INVESTIGATION';
}

/**
 * RESOLVED n'est atteignable que depuis UNDER_INVESTIGATION ou RESPONDED, et le
 * serveur exige qu'une réponse client ait été envoyée (pas de résolution silencieuse).
 */
export function canResolveComplaint(complaint: Complaint): boolean {
  return statusAllowsResolve(complaint.status) && !!complaint.firstResponseAt;
}

/** Résolution bloquée par la seule absence de réponse client : cas à expliquer à l'écran. */
export function awaitingFirstResponse(complaint: Complaint): boolean {
  return statusAllowsResolve(complaint.status) && !complaint.firstResponseAt;
}

function statusAllowsResolve(status: ComplaintStatus): boolean {
  return status === 'UNDER_INVESTIGATION' || status === 'RESPONDED';
}

export function canCloseComplaint(status: ComplaintStatus): boolean {
  return status === 'RESOLVED';
}

export function canReopenComplaint(status: ComplaintStatus): boolean {
  return status === 'RESOLVED' || status === 'CLOSED';
}

/** La satisfaction ne se mesure qu'une fois le traitement terminé. */
export function canRateComplaint(status: ComplaintStatus): boolean {
  return status === 'RESOLVED' || status === 'CLOSED';
}

/** Suppression réservée aux réclamations non traitées ou rejetées. */
export function canDeleteComplaint(status: ComplaintStatus): boolean {
  return status === 'RECEIVED' || status === 'REJECTED';
}
