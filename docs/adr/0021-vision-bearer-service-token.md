# ADR 0021 — Jeton de service Bearer (client_credentials) pour l'appel engine → ai-vision-5s

- **Statut** : Accepté
- **Date** : 2026-06-12
- **Owners** : @Couldevlop

## Contexte

La passerelle `VisionGatewayClient` (api-quality-engine) relayait les photos 5S vers
`ai-vision-5s` avec le seul en-tête `X-Dev-Claims` (dette « vision prod Bearer », ADR 0014
ayant acté le même schéma pour ai-service). Or :

- en **dev**, ai-vision-5s tourne avec `AUTH_BYPASS=true` et **ignore tout en-tête**
  (il ne lit même pas `X-Dev-Claims`, contrairement à ai-service) — l'en-tête était inutile ;
- en **prod**, ai-vision-5s exige un **Bearer JWT RS256** validé contre le JWKS Keycloak
  avec `aud=api-ai-vision-5s` — `X-Dev-Claims` était insuffisant : l'appel aurait répondu 401.

Problème structurel : le service résolvait le tenant **uniquement** depuis la claim
`tenant_id` du token. Un jeton de service *client_credentials* est émis une fois pour
toutes et **ne porte pas de tenant par requête** : il fallait un canal de propagation du
tenant pour les appels serveur-à-serveur, sans violer la règle §18.2-2 (tenant jamais
lu depuis le body / une source non authentifiée).

## Décision

1. **Côté ai-vision-5s** (`app/infrastructure/auth.py`) : résolution du tenant en deux temps —
   la claim `tenant_id` gagne toujours ; à défaut, l'en-tête **`X-Tenant-Id`** est accepté
   **si et seulement si** l'`azp` du token validé (client id Keycloak) figure dans la
   liste `TRUSTED_SERVICE_AZP` (env CSV, vide par défaut = en-tête jamais accepté).
   L'en-tête reste donc couvert par la signature du Bearer : pas de jeton de client de
   confiance valide → pas d'usurpation de tenant possible. Tout le reste → 401 fail-closed.
2. **Côté api-quality-engine** : mode d'auth configurable `qualitos.vision.auth`
   (`none | dev-claims | bearer`, défaut `dev-claims` rétro-compatible dev ; **la prod
   DOIT être en `bearer`**). En `bearer`, un `ServiceTokenProvider` obtient un jeton
   OAuth2 **client_credentials** au token endpoint Keycloak (client confidentiel
   `api-quality-engine-vision` + audience mapper `api-ai-vision-5s`), le met en cache
   jusqu'à `expires_in - marge` (thread-safe, timeouts courts, aucun secret/jeton
   journalisé) et la passerelle envoie `Authorization: Bearer <token>` +
   `X-Tenant-Id: <tenant du TenantContext, issu du JWT utilisateur>`.
3. **Fail-closed** : tout échec d'obtention du jeton (config incomplète, endpoint
   injoignable, réponse sans `access_token`) lève `VisionUnavailableException` → 503
   `vision-unavailable` existant, **sans repli silencieux** vers dev-claims.

## Alternatives écartées

- *Claim `tenant_id` codée en dur dans le client Keycloak* : un seul tenant possible par
  client de service, incompatible avec l'engine multi-tenant.
- *Token exchange (RFC 8693) du JWT utilisateur* : plus précis mais plus lourd
  (configuration Keycloak avancée, un aller-retour par requête) ; pourra remplacer ce
  schéma plus tard sans changer le contrat côté service (la claim `tenant_id` gagnerait).
- *mTLS seul (mode `none`)* : ne porte pas le tenant et déplace toute la confiance dans
  le réseau ; conservé seulement comme mode explicite déconseillé.

## Conséquences

- Provisionnement prod : client Keycloak confidentiel + secret via Vault/ESO + env
  `VISION_AUTH=bearer`, `VISION_TOKEN_URI`, `VISION_CLIENT_ID`, `VISION_CLIENT_SECRET`
  (engine) et `TRUSTED_SERVICE_AZP=api-quality-engine-vision` (vision). Détail dans
  `docs/runbooks/vision-5s.md` §3bis.
- La même approche pourra être généralisée à `AiGatewayClient` → ai-service (dette ADR
  0014 « à implémenter avant prod ») : ai-service lit déjà `tid`/`tenant_id` depuis le
  token et devra recevoir la même extension azp de confiance.
