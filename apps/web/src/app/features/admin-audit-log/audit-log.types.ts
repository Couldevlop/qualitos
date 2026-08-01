/**
 * Journal d'audit (§11.5) — contrat de `/api/v1/audit/events`.
 *
 * Le journal est APPEND-ONLY côté serveur : aucun DTO de mise à jour ni de suppression
 * n'existe. Seul `blockchainTxRef` peut être écrit une fois après création (ancrage
 * idempotent), et cet écran ne l'expose qu'en lecture.
 */

/** Nature de l'acteur à l'origine de l'événement. */
export type ActorType = 'USER' | 'SYSTEM' | 'EXTERNAL' | 'SCHEDULER';

/** Une ligne du journal, telle que renvoyée par `AuditEventDto.EventResponse`. */
export interface AuditEvent {
  id: string;
  tenantId: string;
  /** Numéro de séquence strictement croissant par tenant : c'est la clé du chaînage. */
  sequenceNo: number;
  occurredAt: string;
  recordedAt: string;
  actorType: ActorType;
  actorUserId: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  summary: string | null;
  payloadJson: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  integrityHash: string;
  /** Hash de l'événement N-1 du même tenant ; `null` sur le tout premier. */
  previousHash: string | null;
  blockchainTxRef: string | null;
}

/** Enveloppe `Page<T>` de Spring Data. */
export interface AuditEventPage {
  content: AuditEvent[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Rupture détectée par la vérification de chaîne (trou de séquence, hash falsifié). */
export interface ChainBreak {
  /** `null` quand la rupture est un événement manquant : il n'y a pas d'id à citer. */
  eventId: string | null;
  sequenceNo: number;
  reason: string;
}

/** Résultat de `GET /verify` sur une fenêtre [fromSeq, toSeq] inclusive. */
export interface ChainVerification {
  tenantId: string;
  fromSequenceNo: number;
  toSequenceNo: number;
  verifiedCount: number;
  valid: boolean;
  breaks: ChainBreak[];
}

/**
 * Critères acceptés par `GET /api/v1/audit/events`.
 * Les dates sont des instants ISO-8601 (avec fuseau), le serveur les parse en `Instant`.
 */
export interface AuditEventFilter {
  action?: string;
  resourceType?: string;
  resourceId?: string;
  actorUserId?: string;
  from?: string;
  to?: string;
}

/**
 * Critère réellement appliqué par le serveur.
 *
 * `AuditEventService.list` est une cascade `if/else` : un seul critère est retenu, dans
 * l'ordre ressource → action → acteur → période. Saisir plusieurs filtres ne les combine
 * PAS. L'écran doit le dire à l'utilisateur plutôt que de lui laisser croire à un ET.
 */
export type EffectiveCriterion = 'resource' | 'action' | 'actor' | 'period' | 'none';
