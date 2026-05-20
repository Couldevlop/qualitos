import { SpringPage } from '../pdca/pdca.types';

export type DocumentStatus = 'ACTIVE' | 'ARCHIVED';

export type DocumentType =
  | 'POLICY'
  | 'PROCEDURE'
  | 'WORK_INSTRUCTION'
  | 'RECORD'
  | 'FORM'
  | 'MANUAL'
  | 'OTHER';

export type VersionStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'OBSOLETE';

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
}

export interface UpdateDocumentRequest {
  title?: string;
  description?: string;
  type?: DocumentType;
  ownerId?: string;
  mandatoryRead?: boolean;
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
