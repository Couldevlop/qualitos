# ADR 0037 — Standards Hub : génération documentaire IA AVANCÉE multi-documents (dossier en lot)

- **Statut** : Accepté
- **Date** : 2026-06-22
- **Périmètre** : `apps/api-quality-engine/.../standards/normdoc/dossier/**`,
  `apps/web/src/app/features/standards-doc-gen/**`
- **Réf. CLAUDE.md** : §8.8 (Génération IA assistée des documents normatifs),
  §8.9 (Multi-certifications & IMS — réutilisation transversale), §12.1 (LLM
  structured output), §18.2 #2/#4/#5/#9, §16 (rôles).
- **S'appuie sur** : ADR 0032 (génération mono-document + workflow de validation
  humaine), ADR 0011 (signature hybride), ADR 0012 (ancrage blockchain),
  ADR 0014 (passerelle IA).

## Contexte

L'ADR 0032 a livré la génération **mono-document** (un Manuel, OU une Politique,
OU une procédure), section par section, avec son cycle de validation humaine
(`BROUILLON_IA → EN_VALIDATION → APPROUVE`). Mais §8.8 promet la génération en
**quelques minutes d'un dossier documentaire COMPLET** pour l'adoption d'une
norme : Manuel Qualité multi-sections **+** Politique Qualité **+** les
procédures documentées requises (maîtrise des informations documentées, audit
interne, actions correctives, revue de direction). Il manquait :

1. l'**orchestration en lot** d'un ensemble de pièces à partir d'un seul contexte
   tenant + d'un plan documentaire dérivé de la norme ;
2. un **suivi de progression par pièce** (statut de génération, % d'avancement,
   résilience aux pannes IA) ;
3. la **réutilisation transversale** (§8.9) : proposer une pièce déjà approuvée
   d'une autre norme couverte plutôt que de tout régénérer ;
4. le **scellement d'intégrité du dossier** (signature ML-DSA + ancrage
   blockchain) une fois toutes les pièces validées humainement (§18.2 #5).

## Décision

### 1. Nouvel agrégat d'orchestration `DocumentationDossier`

Sous-module clean/hexagonal `standards.normdoc.dossier`, **à côté** du module
mono-document (réutilisé, pas dupliqué) :

- **domain** : agrégat `DocumentationDossier` (pur) portant N `DossierDocument`
  (clé, type, plan de sections, statut de génération
  `EN_ATTENTE → EN_GENERATION → GENERE | ECHEC`, lien `normDocId`, réutilisation
  suggérée). Machine d'états du dossier
  `GENERATION_EN_COURS → GENERE → FINALISE`. Garde-fou : la finalisation exige
  que **toutes les pièces générées soient approuvées** (validation humaine) et
  pose l'empreinte SHA-256 + signature + ancrage une seule fois. Ports :
  `DossierRepository`, `DossierPlanProvider`.
- **application** : `DossierService` (orchestration), ports `DossierReuseLookup`,
  `DossierIntegrityPort`, `DossierEventPublisher` ; réutilise les ports 0032
  (`NormDocGenerator`, `NormDocRepository`, `NormDocStandardLookup`,
  `NormDocTenantProvider`, `NormDocActorProvider`).
- **infrastructure** : `StaticDossierPlanProvider` (plan HLS industry-agnostic),
  adapter JPA (pièces en JSON `TEXT` via `@JdbcTypeCode(LONGVARCHAR)`, jamais
  `@Lob`), `NormDocReuseLookupAdapter` (documents approuvés du tenant, même type,
  autres normes), `SignedAnchorDossierIntegrityAdapter` (réutilise
  `HybridSignatureService` + `BlockchainAnchorPort`, contexte de suite
  `audit-report`), publisher d'audit chaîné.
- **presentation** : `DossierController` (`/api/v1/standards/doc-dossiers`),
  `@PreAuthorize` par endpoint, mapping RFC 7807 scopé au contrôleur.

### 2. Génération en lot résiliente

Chaque pièce est rédigée par le **générateur IA réel de 0032**
(`NormDocGenerator` → passerelle `AiGatewayClient` → ai-service) puis persistée
comme `NormativeDocument` en `BROUILLON_IA`. Une **panne IA sur une pièce** la
marque en `ECHEC` sans faire échouer le lot ; un endpoint `retry` relance les
pièces en attente/échec (résilience aux pannes transitoires, ADR 0014). Le suivi
de progression (`generatedCount/totalCount`, `progressPercent`) est calculé sur
l'agrégat.

### 3. Réutilisation transversale (§8.9)

À la planification, `DossierReuseLookup` cherche, parmi les documents
**APPROUVÉS** du tenant sur d'**autres** normes, une pièce équivalente (même
`NormDocKind`) et la propose (`reuseSuggestedNormDocId`) — base de l'économie
d'effort IMS (30–50 %).

### 4. Sécurité (OWASP / §18.2)

- Le **tenant** provient TOUJOURS du JWT (`TenantContext`), jamais du body
  (#2) ; l'adapter refuse les écritures cross-tenant et masque l'existence
  cross-tenant par un 404.
- L'**acteur** (démarreur, finaliseur) est le sujet du JWT (`CurrentUser`),
  jamais un identifiant du body (#5, A01).
- Démarrage/retry réservés au **Manager Qualité+** ; finalisation (signature
  globale + ancrage) au **Directeur Qualité / Admin** (§16).
- Tout appel LLM passe par la passerelle (#4) ; aucun contenu en dur.
- Le plan documentaire est **industry-agnostic** (#9) : trame HLS commune, aucune
  exigence sectorielle codée en dur (l'adaptation reste le rôle des Industry
  Packs et de la rédaction IA bornée à la norme résolue).

### 5. Front (Angular 18, NgModules)

Feature lazy `standards-doc-gen` (HTML/SCSS séparés, design premium, i18n) :
écran de configuration + génération en lot avec suivi de progression par pièce,
relance, finalisation signée ; et une vue de revue/validation d'une pièce
(`documents/:id`) pilotant le workflow humain de 0032
(soumettre/approuver/rejeter).

## Conséquences

- **Positif** : différenciateur majeur vs MasterControl/ETQ (§1.4) — dossier
  complet pré-rempli en minutes, intégrité audit-ready (signée + ancrée),
  mutualisation IMS. Réutilise intégralement l'infra 0032/0011/0012/0014.
- **Limite** : génération séquentielle pièce-par-pièce (latence linéaire au
  nombre de pièces × sections) — acceptable sur CPU avec bornage de tokens ;
  parallélisation possible ultérieurement sans changer le contrat.
- **Migration** : `V93__create_standard_doc_dossiers.sql` (CHECK dupliquant le
  garde-fou « finalisé ⇒ scellé »).

## Alternatives écartées

- **Étendre l'agrégat mono-document de 0032 en multi** : aurait couplé deux
  cycles de vie distincts (génération-en-lot vs validation-par-pièce) — écartée
  au profit d'un agrégat d'orchestration qui RÉFÉRENCE les pièces.
- **Finalisation sans exiger l'approbation de toutes les pièces** : viole
  §18.2 #5 — écartée.
- **Plan documentaire en dur par secteur** : viole §18.2 #9 — écartée au profit
  d'un plan HLS générique + Industry Packs.
