# ADR 0032 — Standards Hub : génération assistée IA de documents normatifs + validation humaine

- **Statut** : Accepté
- **Date** : 2026-06-20
- **Périmètre** : `apps/api-quality-engine/.../standards/normdoc/**`, `apps/ai-service/**`
- **Réf. CLAUDE.md** : §8.8 (Génération IA assistée des documents normatifs, onglet 3),
  §12.1 (LLM structured output), §18.2 #2/#4/#5, §16 (rôles).

## Contexte

Le Standards Hub (§8) doit livrer, pour chaque norme, un dossier de certification
clé en main. L'onglet 3 « Bibliothèque documentaire » (§8.4/§8.8) promet la
**génération IA** de documents normatifs complets (Manuel Qualité, Politique
Qualité, Procédures documentées) **pré-remplis** à partir du **contexte tenant**
(nom, secteur, taille, langue, processus connus) et des modèles du référentiel.

L'amorce existante (`AiDraftService` / `StandardsController.aiDraft`) ne produisait
qu'un **brouillon d'un seul paragraphe** par modèle, sans persistance ni cycle de
vie. Il manquait : (1) un document **complet, multi-sections** ; (2) un **workflow
de validation humaine** garantissant qu'**aucun document n'est publié sans
signature humaine** (§18.2 #5 — « l'IA suggère, l'humain décide »).

## Décision

### 1. Génération structurée côté `ai-service`

Nouveau cas d'usage hexagonal `GenerateNormDocUseCase` + endpoint
`POST /v1/ai/standards/generate-document`. Le document est rédigé **section par
section** via le port `AIProvider` (un appel LLM par section, pour borner le
contexte et **tracer les clauses** couvertes). Les garde-fous existants
s'appliquent : bouclier anti-injection (LLM01), redaction PII (LLM06), journal
d'audit caviardé (A09). Le domaine (`domain/model/normdoc.py`) reste pur (aucun
framework) ; les contrats import-linter sont respectés.

### 2. Cycle de vie + validation humaine côté `api-quality-engine`

Nouveau module clean/hexagonal `standards.normdoc` :

- **domain** : agrégat `NormativeDocument` (pur, sans Spring/JPA) avec la machine
  à états
  `BROUILLON_IA → EN_VALIDATION → APPROUVE` (et `EN_VALIDATION → REJETE →
  BROUILLON_IA` pour la reprise). La transition directe `BROUILLON_IA → APPROUVE`
  est **interdite** : la revue est obligatoire. L'approbation exige une
  **signature humaine** non vide et un **approbateur ≠ soumetteur** (séparation
  des tâches). Ports : `NormDocRepository`, `NormDocGenerator`.
- **application** : `NormDocService` (cas d'usage), ports `NormDocTenantProvider`,
  `NormDocActorProvider`, `NormDocStandardLookup`, `NormDocEventPublisher`.
- **infrastructure** : adapter JPA (sections en JSON `TEXT` via
  `@JdbcTypeCode(LONGVARCHAR)`, jamais `@Lob`), `AiGatewayNormDocGenerator`
  (appelle la passerelle IA réelle `AiGatewayClient` → ai-service, **aucun
  document en dur**), `StandardLookupAdapter`, providers tenant/acteur,
  publisher d'audit chaîné.
- **presentation** : `NormDocController` (`/api/v1/standards/norm-documents`),
  `@PreAuthorize` par endpoint, mapping d'erreurs RFC 7807 scopé au contrôleur.

### 3. Sécurité (OWASP / §18.2)

- Le **tenant** provient TOUJOURS du JWT (`TenantContext`), jamais du body
  (#2) ; l'adapter refuse toute écriture cross-tenant et masque l'existence
  cross-tenant par un 404.
- L'**acteur** (soumetteur, approbateur) est le **sujet du JWT** (`CurrentUser`),
  jamais un identifiant du body (#5, A01) — le DTO d'entrée ne porte ni
  `tenant_id` ni identifiant d'approbateur.
- L'approbation (signature) est réservée au **Directeur Qualité / Admin** ; la
  génération/édition/soumission au **Manager Qualité+** (§16).
- Tout appel LLM passe par la passerelle `AiGatewayClient` (#4) — redaction PII +
  bouclier injection appliqués par `ai-service`.

## Conséquences

- **Positif** : différenciateur fort vs MasterControl/ETQ (§1.4) ; documents
  complets pré-remplis en minutes ; intégrité du circuit d'approbation (auditable,
  ancrable blockchain via le journal d'audit chaîné).
- **Limite** : la génération reste section-par-section (latence linéaire au nombre
  de sections) — acceptable sur CPU avec bornage de tokens (ADR 0014).
- **Migration** : `V88__create_standard_norm_documents.sql` (contrainte CHECK
  dupliquant le garde-fou « approuvé ⇒ signé »).

## Alternatives écartées

- **Génération mono-appel de tout le document** : moins de traçabilité par
  clause, sortie LLM plus difficile à structurer et à valider — écartée.
- **Publication directe sans revue** : viole §18.2 #5 — écartée.
