# ADR 0001 — Multi-tenant via claim JWT, jamais via le body de la requête

- **Statut** : Accepté
- **Date** : 2026-05-10
- **Owners** : @Couldevlop

## Contexte

QualitOS est une plateforme SaaS multi-tenant. Chaque appel API doit être isolé
au tenant de l'utilisateur appelant — un compromis d'isolation tenant constitue
une faille critique (fuite de données entre clients).

Trois options évaluées :

1. `tenant_id` dans le path : `/api/v1/tenants/{tenantId}/pdca/cycles`
2. `tenant_id` dans le body de la requête (champ `tenantId`)
3. `tenant_id` extrait du JWT (claim signé par Keycloak)

## Décision

**Option 3 retenue** : le `tenant_id` est porté par un claim signé du JWT et
extrait côté backend par `TenantJwtFilter` → `TenantContext` (ThreadLocal).

Aucun code applicatif ne lit `tenant_id` depuis le body, les query params, ou
les path variables. Toutes les requêtes au repository filtrent par
`tenantId == TenantContext.getTenantId()`.

## Conséquences

- ✅ **Sécurité forte** : un attaquant ne peut pas se faire passer pour un
  autre tenant en modifiant son payload. Il faudrait forger un JWT signé.
- ✅ **Code applicatif simple** : pas de duplication du `tenant_id` dans chaque
  DTO ; un seul point de lecture (`TenantContext`).
- ✅ **Audit trail uniforme** : tous les logs portent `tenant_id` via le MDC
  injecté par le filtre.
- ⚠ Le frontend ne peut pas changer de tenant sans relogin (acceptable : un
  utilisateur appartient à un tenant unique en MVP, multi-tenant assignment
  viendra en P5).
- ⚠ Couplage fort avec Keycloak : un mapper custom est nécessaire pour
  exposer le claim `tenant_id` depuis l'attribut utilisateur. Documenté
  dans `infra/keycloak/realm-export.json`.

## Tests d'invariant

- `TenantJwtFilterTest` (api-core + api-quality-engine) : 7 cas couvrant
  JWT valide, sans claim, claim vide, JWT invalide.
- Pour chaque module métier, le `Service` lève
  `MissingTenantContextException` si le ThreadLocal est vide → 403.
- Les `Repository` exposent uniquement `findByIdAndTenantId(...)`,
  `findByTenantId(...)` etc. Pas de `findById(...)` direct sur les
  entités tenant-scoped.

## Références

- CLAUDE.md §18.2 (règles non négociables) : « Aucun `tenant_id` lu depuis
  le body d'une requête : toujours depuis le JWT validé. »
- CLAUDE.md §10.3 (multi-tenancy hybride).
