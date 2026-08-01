/**
 * Webhooks sortants (§13.2) — contrat de `/api/v1/webhooks`.
 *
 * Les identifiants d'évènements sont les chaînes « wire » (`<domaine>.<entité>.<action>`)
 * produites par `EventType#wire()` côté serveur : c'est ce format qui circule dans les
 * payloads publics, et c'est donc lui — et non le nom d'énumération Java — qui doit être
 * envoyé et affiché.
 */

/** Cycle de vie d'un abonnement. `DISABLED_ON_ERRORS` est posé par le serveur. */
export type WebhookSubscriptionStatus = 'ACTIVE' | 'PAUSED' | 'DISABLED_ON_ERRORS';

/** Cycle de vie d'une livraison. `DEAD_LETTER` = abandon après épuisement des tentatives. */
export type WebhookDeliveryStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'RETRYING' | 'DEAD_LETTER';

/** Types d'évènements souscriptibles, au format wire (miroir exact de `EventType`). */
export type WebhookEventType =
  | 'pdca.cycle.created'
  | 'pdca.cycle.advanced'
  | 'pdca.cycle.completed'
  | 'pdca.cycle.cancelled'
  | 'capa.case.opened'
  | 'capa.case.resolved'
  | 'capa.case.closed'
  | 'capa.effectiveness.verified'
  | 'fives.audit.completed'
  | 'ishikawa.diagram.validated'
  | 'audit.plan.completed'
  | 'audit.finding.raised'
  | 'documents.version.published'
  | 'documents.version.acknowledged'
  | 'standards.adoption.certified'
  | 'standards.adoption.expired'
  | 'dmaic.project.phase_advanced'
  | 'dmaic.project.completed'
  | 'circles.proposal.approved'
  | 'circles.proposal.measured'
  | 'kpi.threshold.breached'
  | 'webhook.test.ping';

/**
 * Regroupement par domaine pour le sélecteur multi-évènements.
 *
 * `webhook.test.ping` est délibérément absent : il n'est pas souscriptible, le serveur
 * l'émet de lui-même via l'endpoint de test. Le proposer laisserait croire qu'on peut
 * s'abonner à un évènement que la plateforme ne publie jamais spontanément.
 */
export interface WebhookEventGroup {
  /** Clé stable du domaine ; le libellé traduit est calculé côté composant. */
  domain: string;
  types: WebhookEventType[];
}

export const WEBHOOK_EVENT_GROUPS: readonly WebhookEventGroup[] = [
  { domain: 'pdca', types: ['pdca.cycle.created', 'pdca.cycle.advanced', 'pdca.cycle.completed', 'pdca.cycle.cancelled'] },
  { domain: 'capa', types: ['capa.case.opened', 'capa.case.resolved', 'capa.case.closed', 'capa.effectiveness.verified'] },
  { domain: 'fives', types: ['fives.audit.completed'] },
  { domain: 'ishikawa', types: ['ishikawa.diagram.validated'] },
  { domain: 'audit', types: ['audit.plan.completed', 'audit.finding.raised'] },
  { domain: 'documents', types: ['documents.version.published', 'documents.version.acknowledged'] },
  { domain: 'standards', types: ['standards.adoption.certified', 'standards.adoption.expired'] },
  { domain: 'dmaic', types: ['dmaic.project.phase_advanced', 'dmaic.project.completed'] },
  { domain: 'circles', types: ['circles.proposal.approved', 'circles.proposal.measured'] },
  { domain: 'kpi', types: ['kpi.threshold.breached'] }
];

/** Abonnement tel que renvoyé par le serveur — le secret n'y figure jamais. */
export interface WebhookSubscription {
  id: string;
  tenantId: string;
  name: string;
  endpointUrl: string;
  eventTypes: string[];
  status: WebhookSubscriptionStatus;
  maxRetries: number;
  consecutiveFailures: number;
  lastTriggeredAt: string | null;
  lastSuccessAt: string | null;
  createdBy: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

/** Réponse de création : seule occasion où le secret HMAC transite en retour. */
export interface CreatedWebhookSubscription {
  subscription: WebhookSubscription;
  secret: string;
}

export interface CreateWebhookSubscriptionRequest {
  name: string;
  endpointUrl: string;
  eventTypes: WebhookEventType[];
  secret: string;
  maxRetries: number;
  /** Identité de l'auteur : le serveur l'exige explicitement (`@NotNull UUID`). */
  createdBy: string;
}

/** PATCH partiel : seuls les champs présents sont appliqués côté serveur. */
export interface UpdateWebhookSubscriptionRequest {
  name?: string;
  endpointUrl?: string;
  eventTypes?: WebhookEventType[];
  secret?: string;
  maxRetries?: number;
  status?: WebhookSubscriptionStatus;
}

/** Tentative de livraison d'un évènement vers l'endpoint d'un abonnement. */
export interface WebhookDelivery {
  id: string;
  tenantId: string;
  subscriptionId: string;
  eventId: string;
  eventType: string;
  payload: string;
  status: WebhookDeliveryStatus;
  attemptCount: number;
  lastAttemptAt: string | null;
  nextRetryAt: string | null;
  responseStatusCode: number | null;
  responseBody: string | null;
  errorMessage: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

/** Résultat synchrone d'un ping de test — permet de valider endpoint + secret. */
export interface WebhookTestPingResult {
  deliveryId: string | null;
  status: WebhookDeliveryStatus;
  responseStatusCode: number | null;
  errorMessage: string | null;
}

/** Sous-ensemble utilisé de la page Spring Data. */
export interface WebhookPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Critères de la liste des livraisons (miroir des paramètres acceptés par l'API). */
export interface WebhookDeliveryQuery {
  subscriptionId: string | null;
  status: WebhookDeliveryStatus | null;
  page: number;
  size: number;
}
