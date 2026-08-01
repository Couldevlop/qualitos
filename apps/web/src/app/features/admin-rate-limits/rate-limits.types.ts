/**
 * Quotas et limites d'API par tenant (§13.1 — rate-limiting par tenant).
 * Contrat de `/api/v1/rate-limits` (RateLimitController).
 */

/**
 * Politique à fenêtre fixe : `maxRequests` requêtes autorisées par tranche de
 * `windowSeconds` secondes, pour un couple (tenant, périmètre).
 * Le tenant est dérivé du JWT côté serveur — il n'est jamais envoyé par le client.
 */
export interface RateLimitPolicy {
  id: string;
  tenantId: string;
  scope: string;
  windowSeconds: number;
  maxRequests: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Corps du `PUT /policies`. Le serveur crée ou met à jour selon (tenant, scope). */
export interface UpsertPolicyRequest {
  scope: string;
  windowSeconds: number;
  maxRequests: number;
  enabled: boolean;
}

/**
 * Décision d'introspection renvoyée par `GET /check/{scope}` : elle lit le compteur
 * de la fenêtre courante SANS l'incrémenter, ce qui la rend utilisable par un écran
 * d'administration sans fausser la mesure qu'il affiche.
 */
export interface RateLimitDecision {
  allowed: boolean;
  limit: number;
  remaining: number;
  retryAfterSeconds: number;
  resetSeconds: number;
}

/**
 * Quand aucune politique active ne couvre le périmètre, le serveur répond
 * `RateLimitDecision.unlimited()` : limite et restant valent `Integer.MAX_VALUE`.
 * Sans ce garde-fou l'écran afficherait « 0 / 2 147 483 647 » et une jauge à 0 %.
 */
export const UNLIMITED_LIMIT = 2147483647;

/** Seuil à partir duquel la consommation mérite un coup d'œil. */
export const WARNING_RATIO = 0.5;

/** Seuil à partir duquel le tenant est réellement proche de sa limite. */
export const CRITICAL_RATIO = 0.8;

/**
 * Niveau de lecture d'une ligne, du plus grave au plus calme.
 * `unmeasured` = politique suspendue (ou non couverte) : rien n'est compté.
 */
export type UsageLevel = 'exceeded' | 'critical' | 'warning' | 'normal' | 'idle' | 'unmeasured';

/** Ligne affichée : la politique, sa mesure courante et sa lecture. */
export interface RateLimitRow {
  policy: RateLimitPolicy;
  /** `null` quand la mesure n'a pas pu être faite (politique suspendue, sonde en échec). */
  usage: RateLimitDecision | null;
  /** Requêtes déjà consommées sur la fenêtre courante. */
  used: number;
  /** Part consommée, entre 0 et 1 ; 0 quand la consommation n'est pas mesurable. */
  ratio: number;
  level: UsageLevel;
}

/** Compteurs de la barre de synthèse. */
export interface RateLimitTotals {
  policies: number;
  enforced: number;
  nearLimit: number;
  exceeded: number;
}

/** Vue consolidée de l'écran : politiques + consommation mesurée. */
export interface RateLimitOverview {
  rows: RateLimitRow[];
  totals: RateLimitTotals;
}
