/**
 * Modèle du flux d'activité (miroir du read-model backend `audit_activity_feed`,
 * exposé par GET /api/v1/activity-feed).
 */
export interface ActivityEntry {
  id: string;
  sequenceNo: number;
  occurredAt: string | null;
  recordedAt: string | null;
  action: string;
  resourceType: string | null;
  resourceId: string | null;
  actorUserId: string | null;
  summary: string | null;
}

/** Forme paginée renvoyée par Spring Data (sous-ensemble utilisé). */
export interface ActivityPage {
  content: ActivityEntry[];
  totalElements: number;
}
