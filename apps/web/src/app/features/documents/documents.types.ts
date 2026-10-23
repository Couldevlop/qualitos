import { SpringPage } from '../pdca/pdca.types';

export type DocumentStatus = 'ACTIVE' | 'ARCHIVED';

export type DocumentType =
  | 'POLICY'
  | 'PROCEDURE'
  | 'WORK_INSTRUCTION'
  | 'RECORD'
  | 'FORM'
  | 'MANUAL'
  | 'EXTERNAL'
  | 'OTHER';

/** Cycle de vie hors archivage (ADR 0094). */
export type DocumentLifecycle = 'DRAFT' | 'IN_FORCE' | 'IN_REVISION';

/** Le statut tel que la liste l'affiche : une revue dépassée l'emporte. */
export type DisplayStatus = 'DRAFT' | 'IN_FORCE' | 'IN_REVISION' | 'REVIEW_OVERDUE' | 'ARCHIVED';

/** Les vues rapides du panneau de gauche. */
export type DocumentView = 'ALL' | 'MINE' | 'TO_VALIDATE' | 'REVIEW_DUE' | 'ARCHIVED';

/** Le fichier d'une version ; jamais sa clé de stockage. */
export interface DocumentFileInfo {
  fileName: string | null;
  contentType: string | null;
  sizeBytes: number;
  previewable: boolean;
  /** Retiré au terme de sa conservation : l'empreinte reste. */
  purged: boolean;
}

export type VersionStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'OBSOLETE';

export type SignatoryDecision = 'PENDING' | 'APPROVED' | 'REJECTED';

/** Un signataire désigné à la soumission ; ils signent dans l'ordre 1 → 3 (ADR 0099). */
export interface DocumentSignatory {
  position: number;
  /** Le compte de connexion (`sub`) de la personne. */
  userId: string;
  name: string;
  decision: SignatoryDecision;
  decidedAt: string | null;
  comment: string | null;
}

/** Un signataire choisi dans l'annuaire, tel qu'il part à la soumission. */
export interface SignatoryRequest {
  userId: string;
  name: string;
}

export interface DocumentVersionResponse {
  id: string;
  documentId: string;
  versionNumber: number;
  content?: string;
  contentUri?: string;
  contentHash?: string;
  changeNote?: string;
  status: VersionStatus;
  authorId: string;
  approvedBy?: string;
  approvedAt?: string;
  publishedAt?: string;
  blockchainTxHash?: string;
  createdAt: string;
  updatedAt: string;
  /** Le dernier refus : la version est revenue en brouillon avec sa raison (ADR 0080). */
  rejectedBy?: string | null;
  rejectedAt?: string | null;
  rejectionReason?: string | null;
  file?: DocumentFileInfo | null;
  /** Le circuit de signature, dans l'ordre ; vide pour une revue d'avant l'ADR 0099. */
  signatories?: DocumentSignatory[];
}

export interface DocumentResponse {
  id: string;
  tenantId: string;
  code: string;
  title: string;
  description?: string;
  type: DocumentType;
  status: DocumentStatus;
  ownerId: string;
  currentVersionId?: string;
  mandatoryRead: boolean;
  createdAt: string;
  updatedAt: string;
  versions: DocumentVersionResponse[];
  folderId?: string | null;
  processId?: string | null;
  lifecycle?: DocumentLifecycle;
  displayStatus?: DisplayStatus;
  /** Échéance de la revue périodique (AAAA-MM-JJ). */
  nextReviewDate?: string | null;
}

export interface DocumentListFilter {
  view?: DocumentView;
  folderId?: string | null;
  q?: string;
  displayStatus?: DisplayStatus | null;
  type?: DocumentType | null;
  processId?: string | null;
  ownerId?: string | null;
}

export interface DocumentViewCounts {
  all: number;
  mine: number;
  toValidate: number;
  reviewDue: number;
  archived: number;
}

export interface DocumentFolder {
  id: string;
  parentId: string | null;
  name: string;
  position: number;
}

export interface DocumentProcess {
  id: string;
  name: string;
  position: number;
}

export interface FolderRequest {
  name: string;
  parentId: string | null;
  position?: number | null;
}

export interface ProcessRequest {
  name: string;
  position?: number | null;
}

export interface DocumentSettings {
  reviewPeriodMonths: number;
  reviewWarningDays: number;
  maxFileMb: number;
  platformMaxFileMb: number;
  quotaMb: number | null;
  obsoleteRetentionYears: number | null;
}

export type DocumentSettingsForm = Omit<DocumentSettings, 'platformMaxFileMb'>;

export interface UploadPolicy {
  maxFileMb: number;
  allowedTypes: string[];
  allowedExtensions: string[];
}

export interface StorageUsage {
  files: number;
  logicalBytes: number;
  originalBytes: number;
  storedBytes: number;
  quotaBytes: number | null;
}

export interface FileLink {
  url: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  expiresInSeconds: number;
}

export type DocumentPage = SpringPage<DocumentResponse>;

export interface CreateDocumentRequest {
  code: string;
  title: string;
  description?: string;
  type: DocumentType;
  ownerId: string;
  mandatoryRead: boolean;
  initialContent?: string;
  initialContentUri?: string;
  initialChangeNote?: string;
  folderId?: string | null;
  processId?: string | null;
}

export interface UpdateDocumentRequest {
  title?: string;
  description?: string;
  type?: DocumentType;
  ownerId?: string;
  mandatoryRead?: boolean;
  folderId?: string | null;
  processId?: string | null;
  nextReviewDate?: string | null;
  clearFolder?: boolean;
  clearProcess?: boolean;
}

export interface CreateVersionRequest {
  content?: string;
  contentUri?: string;
  changeNote?: string;
  authorId: string;
}

export interface UpdateVersionRequest {
  content?: string;
  contentUri?: string;
  changeNote?: string;
}

export interface ApprovalRequest {
  approverId: string;
  /** Accompagne une étape de circuit ; facultatif. */
  comment?: string;
}

export interface AcknowledgeRequest {
  userId: string;
}

export interface AcknowledgmentResponse {
  id: string;
  versionId: string;
  userId: string;
  acknowledgedAt: string;
}
