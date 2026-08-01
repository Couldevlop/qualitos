/**
 * Administration des clés d'API (§13.1 — API REST publique).
 * Contrat de `/api/v1/api-keys` (ApiKeyController + ApiKeyDto).
 */

/** Cycle de vie côté serveur (`ApiKeyStatus`). Seul `ACTIVE` autorise une opération. */
export type ApiKeyStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';

/**
 * Vue serveur d'une clé. Elle n'expose NI le secret en clair NI son hash : le seul
 * élément public est le `prefix`, qui sert à identifier la clé dans les journaux
 * sans rien révéler du secret.
 */
export interface ApiKeyView {
  id: string;
  tenantId: string;
  name: string;
  prefix: string;
  scopes: string[];
  status: ApiKeyStatus;
  createdAt: string;
  createdBy: string;
  expiresAt: string | null;
  lastUsedAt: string | null;
  revokedAt: string | null;
  revokedBy: string | null;
}

/**
 * Réponse de création et de rotation. `plaintext` (`qos_<prefix>_<secret>`) n'est
 * renvoyé QUE par ces deux routes : le serveur ne stocke qu'un bcrypt, il ne pourra
 * jamais le réafficher. L'UI doit donc le présenter une seule fois puis l'oublier.
 */
export interface IssuedApiKey {
  view: ApiKeyView;
  plaintext: string;
}

/** Saisie du formulaire de création. `actor` est ajouté par le service, pas par l'écran. */
export interface ApiKeyCreateInput {
  name: string;
  scopes: string[];
  /** Instant ISO-8601, ou `null` pour une clé sans échéance. */
  expiresAt: string | null;
}

/** Compteurs de la barre de synthèse, dérivés de la liste (le serveur n'en expose pas). */
export interface ApiKeyCounts {
  total: number;
  active: number;
  expiringSoon: number;
  expired: number;
  revoked: number;
}

/** Vue consolidée de l'écran : liste ordonnée + compteurs cohérents avec elle. */
export interface ApiKeysOverview {
  keys: ApiKeyView[];
  counts: ApiKeyCounts;
}

/** `@Size(max = 120)` sur `ApiKeyWebDto.CreateRequest.name`. */
export const API_KEY_NAME_MAX_LENGTH = 120;

/**
 * Format imposé par `ApiKeyService.normalizeScopes` : un scope hors format fait
 * échouer la création en 400. On le valide côté écran pour expliquer le refus au
 * moment de la saisie plutôt qu'après un aller-retour.
 */
export const API_KEY_SCOPE_PATTERN = /^[a-z][a-z0-9._:-]{0,99}$/;

/** Fenêtre d'alerte « expire bientôt » : laisse le temps d'organiser une rotation. */
export const API_KEY_EXPIRING_SOON_DAYS = 30;

/** `@Min(1) @Max(500)` sur le paramètre `limit` de `/expire-due`. */
export const API_KEY_EXPIRE_DUE_MIN = 1;
export const API_KEY_EXPIRE_DUE_MAX = 500;
export const API_KEY_EXPIRE_DUE_DEFAULT = 200;

/**
 * Aucune session exploitable pour renseigner l'`actor` exigé par le serveur
 * (`@NotNull UUID actor`). Distinguée d'une erreur HTTP : le message à afficher
 * n'est pas « le serveur a refusé » mais « reconnectez-vous ».
 */
export class MissingActorError extends Error {
  constructor() {
    super('api-keys: no authenticated actor');
    this.name = 'MissingActorError';
  }
}

/**
 * Découpe une saisie libre de scopes (espaces, virgules, points-virgules, retours
 * ligne) en liste canonique. Dédoublonne et trie comme le fait le `TreeSet` du
 * serveur, pour que ce qui est affiché après création soit ce qui a été saisi.
 *
 * Ne transforme PAS la casse : un scope majuscule est invalide et doit être signalé
 * à l'utilisateur, pas corrigé en douce sous son nez.
 */
export function parseScopes(raw: string | null | undefined): string[] {
  const unique = new Set<string>();
  for (const token of (raw ?? '').split(/[\s,;]+/)) {
    const trimmed = token.trim();
    if (trimmed) unique.add(trimmed);
  }
  return [...unique].sort((a, b) => a.localeCompare(b));
}
