# ADR 0041 — Marketplace de packs normatifs (CLAUDE.md §8.11)

- Statut : Accepté
- Date : 2026-06-22
- Portée : `apps/api-quality-engine` (package `quality.marketplace`), `apps/web` (feature `marketplace`)

## Contexte

CLAUDE.md §8.11 prévoit un **marketplace** où des consultants partenaires publient
des packs normatifs/sectoriels avancés (ex. « Pack ISO 13485 pour startup MedTech »),
soumis à **validation de l'éditeur principal avant publication**, puis installables
par les tenants. Un socle (commit `b7a36b0`, P5) existait : un agrégat
`MarketplacePack` avec un simple booléen `verified` (catalogue cross-tenant, table
`marketplace_packs` en V56), sans cycle de vie, sans soumission partenaire, sans
installation par tenant et sans frontend.

Cet ADR acte la livraison **à 100 %** du module : cycle de vie complet, soumission
partenaire, modération par l'éditeur, installation par tenant et UI premium.

## Décision

### 1. Cycle de vie explicite (machine à états)

Le booléen `verified` est remplacé par un statut
`SUBMITTED → IN_REVIEW → PUBLISHED / REJECTED → DEPRECATED` (enum
`MarketplacePackStatus`). Les transitions sont portées par l'agrégat
(`takeForReview`, `publish`, `reject`, `deprecate`) ; toute transition invalide lève
`MarketplacePackStateException` (→ 409). **Aucun pack n'atteint `PUBLISHED` sans
passer par `IN_REVIEW` puis un appel explicite `publish()` par l'éditeur** —
validation humaine garantie côté domaine ET côté RBAC.

### 2. Rôles et RBAC par endpoint (OWASP A01)

- **Soumission** (`POST /api/v1/marketplace/packs`) : rôle `PARTNER` (éditeur
  partenaire) + admins. Carve-out d'URL dans `SecurityConfig` (matcher plus
  spécifique avant la règle générique `POST /api/v1/marketplace/**`) + `@PreAuthorize`.
- **Modération** (take-review / publish / reject / deprecate / queue / detail éditeur) :
  `SUPER_ADMIN` uniquement (`@PreAuthorize("hasRole('SUPER_ADMIN')")`).
- **Installation / désinstallation / notation** : `ADMIN_TENANT` (tenant du JWT).
- **Lecture catalogue** : tout authentifié, **uniquement les packs `PUBLISHED`**
  (les brouillons/rejets ne fuient pas — 404 sur un id non publié).

Double rempart : règles d'URL (`SecurityConfig`) + sécurité de méthode
(`@PreAuthorize`) + ports d'acteur résolus depuis le JWT (jamais le body).

### 3. Multi-tenant strict (CLAUDE.md §10.3, règle 18.2)

Le catalogue des packs est **platform-level** (cross-tenant). Les **installations**
sont **par tenant** : nouvelle table `marketplace_installations`, `tenant_id`
**toujours issu du JWT** (`TenantProvider`), jamais du body. Une installation
ACTIVE unique par `(tenant, pack)` via index partiel `WHERE status='INSTALLED'` ;
l'historique est conservé (jamais de suppression physique — audit ISO 19011, comme
les activations d'Industry Packs §5).

### 4. Articulation avec les Industry Packs (sans casse)

Le mécanisme `IndustryPack*` (catalogue YAML + `TenantIndustryPackActivation`) n'est
**pas modifié**. L'installation marketplace est un agrégat distinct
(`MarketplaceInstallation`) qui copie le `packId` (code) du pack — clé d'articulation
naturelle avec un Industry Pack homonyme — sans coupler les deux modèles.

### 5. Scan de manifeste à la soumission (OWASP A01/A10)

`ManifestScanner` (couche application, port pur Jackson) valide le manifeste inline
AVANT toute persistance : JSON objet, taille ≤ 64 Kio, champs requis (`name`,
`version`), slugs de normes `[a-z0-9-]`, et **garde anti path-traversal** sur toute
référence de fichier (`..`, chemin absolu, drive Windows, schémas `file:`/`jar:`/
`classpath:`/`://`). Le scanner **ne télécharge jamais** le contenu distant
(anti-SSRF). Un échec bloquant refuse la soumission (409).

### 6. Architecture hexagonale (ArchUnit)

Strict respect des couches `domain` / `application` / `infrastructure` / `web` :
`domain` sans framework ; `application` (service, scanner, ports `SuperAdminProvider`,
`CurrentActorProvider`, `TenantProvider`) sans Spring/JPA — câblée par
`MarketplacePackBeanConfiguration`. `HexagonalArchitectureTest` (ArchUnit) couvre le
package `marketplace.*` et passe.

## Conséquences

- **Migrations** : `V94__marketplace_pack_lifecycle.sql` (évolution de
  `marketplace_packs` : `status`, `norms_csv`, `submitted_by/at`, `reviewed_by/at`,
  `review_notes`, `manifest_json` TEXT, `rating_avg/count` ; backfill depuis
  `verified` puis drop de la colonne), `V95__marketplace_installations.sql`.
- **API** : 14 endpoints sous `/api/v1/marketplace/packs` (catalogue, soumission,
  modération, installation, notation).
- **Frontend** : feature lazy `marketplace` (catalogue, soumission, modération),
  design premium, i18n par attributs `i18n`.
- **Notation** : un tenant ne peut noter qu'un pack publié **qu'il a installé**
  (anti-vote fantôme) ; moyenne recalculée de façon incrémentale.
- **Tests** : domaine + application (Mockito) + web (`@WebMvcTest` + sécurité de
  méthode) côté backend ; service + 3 composants côté front (Karma).

## Alternatives écartées

- **Conserver le booléen `verified`** : insuffisant pour un workflow de validation
  réel (pas d'état « en revue », pas de rejet motivé, pas de dépréciation).
- **Réutiliser `TenantIndustryPackActivation` pour les installations marketplace** :
  couplerait deux cycles de vie distincts et risquerait de casser les Industry Packs.
- **Scanner le contenu distant du manifeste** : risque SSRF ; on se limite au
  manifeste inline fourni à la soumission.
- **Rôle de soumission = `ADMIN_TENANT`** : un partenaire éditeur n'est pas un
  administrateur de tenant ; on introduit un rôle `PARTNER` dédié.
