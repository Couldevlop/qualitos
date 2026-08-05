# ADR 0048 — Propagation du tenant sur l'appel engine → ai-service (jeton de service)

- **Statut** : Accepté
- **Date** : 2026-08-05
- **Owners** : @Couldevlop

## Contexte

Le passage de l'ai-service en validation réelle des jetons (retrait de `QOS_DEV_AUTH`,
mode `bearer` côté engine) a mis **toutes les fonctions d'IA hors service** en
préproduction : chaque écran affichait « Passerelle IA indisponible : 401 Unauthorized ».

Le jeton n'était pas en cause — vérifié en préproduction : émetteur exact, `aud` contenant
`qualitos-ai`, `azp=api-quality-engine-ai`, signature valide. Le refus venait du validateur,
qui exigeait la claim `tid` dans **tout** jeton (`CrossTenantAccessError: JWT missing 'tid'
or 'sub'`). Or un jeton `client_credentials` représente une **machine** : il est émis une
fois pour toutes et ne porte aucun tenant par requête.

C'est exactement le problème déjà rencontré et résolu pour `ai-vision-5s` (ADR 0021), dont
les conséquences annonçaient la généralisation à `AiGatewayClient` — dette restée ouverte
jusqu'ici, et payée au premier déploiement réel.

## Décision

Transposer le schéma de l'ADR 0021, sans en inventer un second.

1. **Côté ai-service** (`infrastructure/auth/keycloak_jwks_validator.py`) : la résolution du
   tenant se fait en deux temps. La claim `tid` (ou `tenant_id`) **gagne toujours** ; à
   défaut, l'en-tête **`X-Tenant-Id`** est accepté **si et seulement si** l'`azp` du jeton
   validé (à défaut `client_id`) figure dans `TRUSTED_SERVICE_AZP` (liste CSV d'environnement,
   **vide par défaut = en-tête jamais cru**). Tout le reste échoue en 401 — fail-closed.
2. **Côté api-quality-engine** (`AiGatewayClient`) : en mode `bearer`, l'appel porte
   `Authorization: Bearer <jeton de service>` **et** `X-Tenant-Id: <tenant du TenantContext>`,
   c'est-à-dire le tenant du JWT **utilisateur** d'origine — jamais une valeur du corps de
   la requête (§18.2-2). Le jeton est obtenu avant la construction de la requête : un défaut
   de configuration interrompt l'appel en amont, sans repli sur `X-Dev-Claims`.

La restriction sur l'`azp` est le cœur de la décision : sans elle, l'en-tête rouvrirait
exactement la faille que le retrait de `X-Dev-Claims` avait fermée — n'importe quel porteur
d'un jeton valide se déclarerait de n'importe quel tenant.

## Alternatives écartées

- **`QOS_DEV_AUTH=1` sur l'ai-service** pour débloquer la préproduction : accepte un en-tête
  déclaratif sans preuve, donc usurpation de tenant possible depuis n'importe quel pod, sur
  un environnement exposé. Écartée : un contournement d'exploitation ne doit pas devenir la
  configuration d'un environnement accessible.
- **Claim `tenant_id` figée dans le client Keycloak de service** : un seul tenant par client,
  incompatible avec une plateforme multi-tenant.
- **Token exchange (RFC 8693)** du JWT utilisateur : plus précis, mais un aller-retour de plus
  par requête et une configuration Keycloak avancée. Pourra remplacer ce schéma plus tard sans
  changer le contrat côté service : la claim `tid` gagnerait alors d'elle-même.

## Conséquences

- Provisionnement : `TRUSTED_SERVICE_AZP=api-quality-engine-ai` sur l'ai-service (préprod et
  prod), à tenir cohérent avec `AI_CLIENT_ID` côté engine. Une divergence entre les deux se
  traduit par un 401 sur toutes les fonctions d'IA — c'est le mode de panne à reconnaître.
- En développement, la valeur reste **vide** : la pile locale tourne en `QOS_DEV_AUTH`, où le
  tenant vient de `X-Dev-Claims`.
- La dette « azp de confiance à généraliser à AiGatewayClient » (ADR 0014, rappelée par
  l'ADR 0021) est soldée. Les deux passerelles serveur-à-serveur — vision et IA — appliquent
  désormais la même règle, ce qui rend le contrat vérifiable d'un seul coup d'œil.
- Tests : la règle de résolution est couverte côté ai-service (claim prioritaire, `azp` de
  confiance, `azp` inconnu, liste vide, en-tête absent ou non-UUID), le câblage l'est par un
  test de bout en bout signant un vrai jeton RS256, et l'engine vérifie ce qui part réellement
  sur le réseau (`AiGatewayAuthModeTest`).
