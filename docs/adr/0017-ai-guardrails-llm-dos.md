# ADR 0017 — Garde-fous du chemin LLM (OWASP LLM04 — Model Denial of Service)

- **Statut** : Accepté
- **Date** : 2026-05-31
- **Décideurs** : Architecte principal
- **Contexte** : api-quality-engine, passerelle IA (`AiGatewayClient`), CLAUDE.md §11.2 (OWASP LLM Top 10), §18.2 règle 4

## Contexte

Tout appel LLM de la plateforme transite par un point unique : `AiGatewayClient`
(`complete` pour les brouillons normatifs / suggestions Ishikawa-CAPA, `askNlq` pour
le NLQ). Le CLAUDE.md §11.2 exige pour **LLM04 (Model DoS)** : « quotas par tenant,
rate-limit, timeouts, circuit breaker ». L'audit `docs/AUDIT-stub-vs-reel.md` (#9)
constatait que ces garde-fous n'étaient **pas branchés** sur ce chemin : un tenant
pouvait saturer le service IA (inférence CPU coûteuse) au détriment des autres, et
rien ne court-circuitait les appels quand `ai-service` tombe.

## Décision

Garde-fou **en architecture hexagonale**, appliqué dans l'engine au plus près du
point de sortie, **avant tout départ réseau** :

- **Port** `AiGuard` (`quality.ai.guard`) : `check(AiCallContext)` +
  `recordSuccess/recordFailure(tenantId)`. Découple la politique de l'appelant.
- **Adaptateur** `TokenBucketAiGuard` (en mémoire, zéro dépendance → pas de surface
  CVE, OWASP A06), **cloisonné par tenant** :
  - **token bucket** à remplissage continu (débit/minute) ;
  - **quota journalier** (remis à zéro au changement de jour, horloge injectable) ;
  - **disjoncteur** CLOSED → OPEN → HALF_OPEN (ouvre après N échecs consécutifs,
    demi-ouvert après refroidissement, referme sur succès) ;
  - **borne de taille de prompt** (charge utile abusive).
- **Paramétrage** `AiGuardProperties` (`qualitos.ai.guard.*`), tout par variable
  d'env, désactivable (§18.2-9 — aucune valeur en dur).
- **Câblage** : `AiGatewayClient.complete`/`askNlq` appellent `guard.check(ctx)` puis
  `recordSuccess`/`recordFailure`. Les rejets (`AiGuardException`) sont mappés en
  **429** (débit/quota), **413** (prompt trop grand), **503** (disjoncteur ouvert)
  par `GlobalExceptionHandler` (RFC 7807 `ProblemDetail`, `retryAfterSeconds`).
- Le `tenantId` provient du `TenantContext` (JWT), jamais du body (ADR 0001).

## Alternatives écartées

- **resilience4j / Bucket4j** : dépendances supplémentaires pour un besoin simple ;
  écartées en V1 (le port permet d'y basculer sans toucher les appelants).
- **Garde-fou uniquement côté ai-service (slowapi)** : insuffisant — ne cloisonne
  pas par tenant applicatif et ne protège pas l'engine du coût du relais. Conservé
  en **défense en profondeur**.

## Conséquences

- ✅ LLM04 honoré sur le chemin réel ; rejets explicites et actionnables (Retry-After).
- ✅ Fail-fast quand `ai-service` est en panne (disjoncteur), sans l'enfoncer.
- ✅ Testé : `TokenBucketAiGuardTest` (horloge contrôlée) + `AiGatewayClientTest`.
- ⚠ **État local au nœud** (non partagé entre répliques) : cloisonnement per-instance.
  Pour un quota strict en cluster, fournir un adaptateur **Redis** du port `AiGuard`
  — appelants inchangés.
- ⚠ Quota en **nombre d'appels** (pas en tokens / budget monétaire). Évolution
  possible via `AiCallContext.promptChars`.
