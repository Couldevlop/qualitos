# ADR 0020 — Matrice d'autorisation par rôle (CLAUDE.md §16) sur api-quality-engine

- **Statut** : Accepté (partiel-mesuré — voir « Reste à faire »)
- **Date** : 2026-06-06
- **Owners** : Architecte principal

## Contexte

L'audit sécurité (H1/H2, OWASP A01 — Broken Access Control) constatait deux
trous sur `api-quality-engine` :

- **H1** : presque tous les endpoints n'étaient gardés que par `authenticated()`,
  sans propager la matrice de rôles de CLAUDE.md §16 (Super Admin / Admin Tenant /
  Directeur Qualité / Manager Qualité / Auditeur / Utilisateur / Externe).
- **H2** : l'attribution d'acteur (`actor`) d'une action transitait par le **corps
  de requête** (falsifiable), au lieu d'être dérivée de l'identité authentifiée.

Un premier lot avait gardé les endpoints les plus sensibles (industry-packs
activate, api-keys, webhooks, blockchain anchor, audit/events) et introduit
`CurrentUser` (sub du JWT). Restaient : le dernier trou H2 (tenant-modules) et
l'extension mesurée du role-gating aux actions d'administration et destructrices.

### Mapping rôles : réalité du realm Keycloak

`infra/keycloak/realm-export.json` définit les rôles realm suivants, mappés en
`ROLE_<UPPER>` par le `jwtAuthenticationConverter` (source `realm_access.roles`) :

| CLAUDE.md §16        | Rôle realm Keycloak | Authority Spring     |
| -------------------- | ------------------- | -------------------- |
| Super Admin          | `super_admin`       | `ROLE_SUPER_ADMIN`   |
| Admin Tenant         | `admin_tenant`      | `ROLE_ADMIN_TENANT`  |
| Directeur Qualité    | `quality_director`  | `ROLE_QUALITY_DIRECTOR` |
| Manager Qualité      | `quality_manager`   | `ROLE_QUALITY_MANAGER`  |
| Auditeur             | `auditor`           | `ROLE_AUDITOR`       |
| Utilisateur          | `user`              | `ROLE_USER`          |
| Externe              | `external_auditor`  | `ROLE_EXTERNAL_AUDITOR` |

**Finding critique** : le realm n'a **pas** de rôle nu `admin` (→ `ROLE_ADMIN`).
Les règles existantes en `hasAnyRole("ADMIN", "SUPER_ADMIN")` n'auraient donc
**jamais matché** l'utilisateur `admin` réel (porteur de `admin_tenant`) →
**lock-out** de l'administrateur de tenant. Le frontend confirme l'ambiguïté
(`ADMIN_ROLES = ['super_admin','admin','tenant_admin','admin_tenant']`), et le
module IoT utilise encore `TENANT_ADMIN` (autre alias non aligné).

## Décision

### H2 — acteur dérivé du JWT (tenant-modules)

- Suppression du champ `actor` du corps de requête (`ModuleActivationWebDto` +
  records de `ModuleActivationDto`). Un `actor` envoyé par le client est
  silencieusement **ignoré** (propriété inconnue tolérée).
- Introduction d'un port `ActorProvider` (couche application) + adapter
  `CurrentUserActorProvider` (infra) branché sur `CurrentUser.requireUserId()`
  (le `sub` du JWT). Le service `ModuleActivationService` dérive l'acteur de ce
  port pour chaque transition ; `AuditLogModuleEventPublisher` continue d'émettre
  `lastChangedBy` (désormais = sub JWT). Pattern symétrique de `TenantProvider`.

### H1 — role-gating mesuré par règles URL (pas `@PreAuthorize` partout)

Toutes les règles ajoutent `ADMIN_TENANT` à côté de `ADMIN` (compat tokens legacy
/ api-core) et `SUPER_ADMIN`, pour **ne pas verrouiller** l'admin de tenant réel.

| Endpoint (méthode)                              | Rôles autorisés                                          | Rôle §16             |
| ----------------------------------------------- | ------------------------------------------------------- | -------------------- |
| `POST/DELETE /api/v1/industry-packs/*/activate` | ADMIN, ADMIN_TENANT, SUPER_ADMIN                        | Admin Tenant         |
| `POST /api/v1/tenant-modules/**`                | ADMIN, ADMIN_TENANT, SUPER_ADMIN                        | Admin Tenant         |
| `/api/v1/api-keys/**`                           | ADMIN, ADMIN_TENANT, SUPER_ADMIN                        | Admin Tenant         |
| `/api/v1/webhooks/**`                           | ADMIN, ADMIN_TENANT, SUPER_ADMIN                        | Admin Tenant         |
| `POST/PUT/PATCH /api/v1/marketplace/**`         | ADMIN, ADMIN_TENANT, SUPER_ADMIN                        | Admin/Super Admin (§8.11) |
| `POST /api/v1/blockchain/anchor/run`            | ADMIN, ADMIN_TENANT, SUPER_ADMIN, QUALITY_MANAGER       | Manager Qualité      |
| `POST /api/v1/audit/events`                     | ADMIN, ADMIN_TENANT, SUPER_ADMIN (+ `@PreAuthorize`)    | Admin Tenant         |
| `DELETE /api/v1/**` (générique)                 | ADMIN, ADMIN_TENANT, SUPER_ADMIN, QUALITY_MANAGER       | Manager Qualité      |

**Carve-outs (restent `authenticated()`) — écritures/captures terrain** : la
consigne interdit de gater NC/5S/audit terrain. Sont donc exclus de la règle
DELETE générique (matchers placés **avant**, premier-match gagne) :

- `DELETE /api/v1/nc/*/photos/**` (suppression d'une photo de NC capturée au terrain)
- `DELETE /api/v1/fives/**` (audits 5S terrain)
- `DELETE /api/v1/audit/**` (plans/findings d'audit)

**Non gardés volontairement (assumé `authenticated()`)** :

- **Toutes les lectures (GET)** : pas de besoin d'admin ; restent authentifiées.
- **Les écritures métier (POST/PUT/PATCH)** de NC, CAPA, 5S, audits, ishikawa,
  pdca, dmaic, circle, supplier, kpi, risk, change, training, standards… :
  doivent rester accessibles au `quality_manager` **et** au `user` terrain.
  Les gater casserait l'usage métier courant (risque de lock-out > bénéfice).

## Conséquences

- L'admin de tenant réel (`admin_tenant`) n'est plus verrouillé (régression de
  lock-out évitée). Defense-in-depth sur le marketplace (déjà gardé service-side
  par `KeycloakSuperAdminProvider`).
- L'acteur d'une activation module est non-répudiable (issu du JWT, audité,
  ancrable). Plus aucun `actor` falsifiable dans le corps tenant-modules.
- Aucune écriture terrain ni lecture n'est régressée.

## Reste à faire (suivi)

- **Aligner les alias de rôle** : `ROLE_ADMIN` (api-core, legacy) vs `ROLE_ADMIN_TENANT`
  (realm) vs `ROLE_TENANT_ADMIN` (module IoT) — unifier sur `admin_tenant` et
  retirer les alias, idéalement via un converter qui normalise.
- **Affiner par rôle métier fin** (Directeur Qualité / Auditeur / Externe) au
  niveau endpoint quand les besoins seront tranchés (ex. Auditeur = lecture
  étendue + génération de rapports ; Externe = accès limité dans le temps).
- **Propager H2** (acteur ⟵ JWT) aux modules encore porteurs d'un `actor` dans le
  corps (ex. `api-keys` `RevokeRequest/RotateRequest`).
