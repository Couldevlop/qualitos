/**
 * RGPD Subject Requests — droits Art. 15/16/17/18/20/21.
 * Le backend hashe le subjectIdentifier ; l'API ne retourne que le hash.
 */

export type SubjectRequestType =
  | 'ACCESS'        // Art. 15
  | 'ERASURE'       // Art. 17 — droit à l'oubli
  | 'PORTABILITY'   // Art. 20
  | 'RECTIFICATION' // Art. 16
  | 'RESTRICTION'   // Art. 18
  | 'OBJECTION';    // Art. 21

export type SubjectRequestStatus =
  | 'RECEIVED' | 'IN_PROGRESS' | 'COMPLETED' | 'REJECTED';

export interface SubjectRequestView {
  id: string;
  tenantId: string;
  type: SubjectRequestType;
  subjectIdentifierHash: string;       // hash only — RGPD-safe
  subjectIdentifierLabel?: string;     // optional pseudonymised label
  status: SubjectRequestStatus;
  receivedAt: string;
  deadlineAt: string;
  extended: boolean;
  inProgressAt?: string;
  completedAt?: string;
  rejectionReason?: string;
  resolutionNotes?: string;
  evidenceUrl?: string;
  requestedByUserId: string;
  handledByUserId?: string;
  updatedAt: string;
  overdue: boolean;
}

/** ReceiveRequest — plaintext subjectIdentifier is hashed server-side. */
export interface ReceiveSubjectRequest {
  type: SubjectRequestType;
  subjectIdentifier: string;
  subjectIdentifierLabel?: string;
  requestedByUserId: string;
}

export interface StartProcessingRequest { handledByUserId: string; }

export interface CompleteSubjectRequest {
  resolutionNotes: string;
  evidenceUrl?: string;
  handledByUserId?: string;
}

export interface RejectSubjectRequest {
  reason: string;
  handledByUserId?: string;
}

export interface ExtendDeadlineRequest {
  newDeadline: string;
}
