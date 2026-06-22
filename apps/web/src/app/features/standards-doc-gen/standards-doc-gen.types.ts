/**
 * Types — génération documentaire IA AVANCÉE multi-documents (Standards Hub §8.8).
 * Reflètent les DTO de l'API (DossierController, /api/v1/standards/doc-dossiers).
 */

export type NormDocKind = 'MANUAL' | 'POLICY' | 'PROCEDURE';

export type DossierStatus = 'GENERATION_EN_COURS' | 'GENERE' | 'FINALISE';

export type DossierDocStatus =
  | 'EN_ATTENTE' | 'EN_GENERATION' | 'GENERE' | 'ECHEC' | 'REUTILISE';

/** Pièce du dossier (suivi de génération + réutilisation suggérée). */
export interface DossierDocumentView {
  key: string;
  kind: NormDocKind;
  label: string;
  status: DossierDocStatus;
  normDocId: string | null;
  reuseSuggestedNormDocId: string | null;
  failureReason: string | null;
  sectionCount: number;
}

/** Vue complète d'un dossier documentaire. */
export interface DossierView {
  id: string;
  tenantId: string;
  standardId: string;
  standardCode: string;
  standardName: string;
  organizationName: string;
  language: string;
  status: DossierStatus;
  aiProvider: string | null;
  documents: DossierDocumentView[];
  totalCount: number;
  generatedCount: number;
  failedCount: number;
  progressPercent: number;
  integritySha256: string | null;
  integritySignature: string | null;
  anchorTxRef: string | null;
  finalizedAt: string | null;
  finalizedByUserId: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

/** Profil tenant non sensible (jamais de PII, jamais de tenant_id technique). */
export interface DossierTenantProfile {
  organizationName: string;
  industry?: string;
  size?: string;
  language?: string;
  knownProcesses?: string[];
}

/** Démarrage d'un dossier. documentKeys vide/absent = plan complet par défaut. */
export interface DossierStartRequest {
  standardId: string;
  tenantProfile: DossierTenantProfile;
  documentKeys?: string[];
}

/** Finalisation : signature humaine globale obligatoire. */
export interface DossierFinalizeRequest {
  signature: string;
  notes?: string;
}

// ---- Document normatif (pièce, workflow de validation humaine — ADR 0032) ----

export type NormDocStatus = 'BROUILLON_IA' | 'EN_VALIDATION' | 'APPROUVE' | 'REJETE';

export interface NormDocSectionView {
  key: string;
  title: string;
  clauses: string[];
  bodyMarkdown: string;
}

export interface NormDocView {
  id: string;
  tenantId: string;
  standardId: string;
  standardCode: string;
  kind: NormDocKind;
  title: string;
  sections: NormDocSectionView[];
  status: NormDocStatus;
  aiProvider: string | null;
  markdown: string;
  submittedAt: string | null;
  submittedByUserId: string | null;
  approvedAt: string | null;
  approvedByUserId: string | null;
  approvalNotes: string | null;
  humanSignature: string | null;
  rejectionReason: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface NormDocApproveRequest {
  signature: string;
  notes?: string;
}

export interface NormDocRejectRequest {
  reason: string;
}
