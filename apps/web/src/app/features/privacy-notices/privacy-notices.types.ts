/**
 * GDPR Privacy Notices — mentions d'information Art. 13/14 RGPD.
 * Backend /api/v1/gdpr/privacy-notices.
 *
 * Cycle de vie : DRAFT → PUBLISHED → ARCHIVED.
 * Une mention PUBLISHED est immutable (preuve de ce qui a été affiché).
 * Toute évolution = nouvelle version DRAFT.
 */

export type PrivacyNoticeStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface PrivacyNoticeView {
  id: string;
  tenantId: string;
  reference: string;       // ex. PUBLIC_WEB_NOTICE
  version: string;         // ex. 2026.1
  language: string;        // ISO-639-1 lowercase (fr, en, es, …)
  title: string;
  summary?: string;
  contentMarkdown?: string;
  linkedProcessingActivityIds: string[];
  publishUrl?: string;
  contactName?: string;
  contactEmail?: string;
  status: PrivacyNoticeStatus;
  effectiveFrom?: string;
  effectiveTo?: string;
  publishedAt?: string;
  publishedByUserId?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePrivacyNoticeRequest {
  reference: string;
  version: string;
  language: string;
  title: string;
  summary?: string;
  contentMarkdown?: string;
  linkedProcessingActivityIds?: string[];
  publishUrl?: string;
  contactName?: string;
  contactEmail?: string;
  createdByUserId: string;
}

export interface EditPrivacyNoticeRequest {
  title: string;
  summary?: string;
  contentMarkdown?: string;
  linkedProcessingActivityIds?: string[];
  publishUrl?: string;
  contactName?: string;
  contactEmail?: string;
}

export interface PublishPrivacyNoticeRequest {
  publishedByUserId: string;
}
