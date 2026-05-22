/**
 * GDPR Consents — Art. 7 RGPD.
 * Backend hashes the subject identifier server-side (SubjectIdentifierHasher);
 * the API never returns the plaintext identifier.
 */

export type ConsentStatus = 'GRANTED' | 'WITHDRAWN' | 'EXPIRED';

export type ConsentSource =
  | 'WEB_FORM' | 'MOBILE_APP' | 'EMAIL' | 'PAPER'
  | 'PHONE' | 'API' | 'IMPORT' | 'OTHER';

export interface ConsentView {
  id: string;
  tenantId: string;
  subjectIdentifierHash: string;     // hash only — RGPD-safe
  subjectIdentifierLabel?: string;   // optional pseudonymised label (e.g. "client#12345")
  purposeCode: string;
  purposeVersion: string;
  source: ConsentSource;
  evidenceUrl?: string;
  ipAddress?: string;
  userAgent?: string;
  grantedByUserId: string;
  grantedAt: string;
  expiresAt?: string;
  status: ConsentStatus;
  withdrawnAt?: string;
  withdrawnByUserId?: string;
  withdrawalReason?: string;
  updatedAt: string;
  active: boolean;
}

/**
 * GrantRequest — the plaintext subjectIdentifier is sent in this payload
 * but is HASHED by the backend before persistence. It must NEVER be stored
 * in any client-side cache or logged.
 */
export interface GrantConsentRequest {
  subjectIdentifier: string;
  subjectIdentifierLabel?: string;
  purposeCode: string;
  purposeVersion: string;
  source: ConsentSource;
  evidenceUrl?: string;
  ipAddress?: string;
  userAgent?: string;
  grantedByUserId: string;
  expiresAt?: string;
}

export interface WithdrawConsentRequest {
  actorUserId: string;
  reason?: string;
}
