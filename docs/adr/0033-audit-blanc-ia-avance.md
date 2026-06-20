# ADR 0033 — Standards Hub : audit blanc IA avancé (questions ciblées + gap analysis + plan de remédiation)

- **Statut** : Accepté
- **Date** : 2026-06-20
- **Périmètre** : `apps/api-quality-engine/.../standards/auditblanc/**`, `apps/ai-service/**`
  (méthode `AiGatewayClient.mockAudit` + endpoint `ai-service`).
- **Réf. CLAUDE.md** : §8.4 onglet 7 (Audit blanc IA), §8.7 (moteur d'alignement),
  §12.1 (LLM structured output), §12.3 (IA explicable), §18.2 #2/#4/#5, §16 (rôles).

## Contexte

Le Standards Hub (§8) promet, onglet 7, un **audit blanc IA** : avant l'audit de
certification officiel, l'IA **simule un audit** sur la norme adoptée. Elle doit
(1) générer **30 à 100 questions ciblées** sur les **clauses à risque**, (2)
**confronter** chaque clause aux **preuves disponibles** du tenant, (3) restituer
un **rapport d'écarts** (gap analysis) avec criticité, et (4) créer
**automatiquement** un **plan de remédiation** actionnable.

L'amorce existante couvrait la **préparation** (`StandardsService.computeAuditBlanc`,
écarts par exigence) et la **décision** (`CertificationBlancService`, verdict
ISO/IEC 17021-1), mais **aucune génération IA de questions** ni **gap analysis
rédigée par l'IA** confrontée aux preuves, ni **plan de remédiation persisté**.

## Décision

### 1. Génération guidée côté `ai-service`

Nouveau cas d'usage hexagonal `MockAuditUseCase` + endpoint
`POST /v1/ai/standards/mock-audit`. Le domaine (`domain/model/mock_audit.py`)
porte les règles **déterministes** : score de priorité d'audit
(`AuditClause.risk_score`, combinant caractère obligatoire × gravité du risque ×
défaut de couverture) et **criticité** (grille ISO/IEC 17021-1). Le LLM reçoit la
matière à risque (clauses + état de preuve) et rend, en **JSON structuré**, des
questions ciblées et des constats par clause. Un parser **défensif**
(`mock_audit_parser`) projette la sortie sur les **clauses connues**
(anti-hallucination, LLM09) ; une clause sans constat IA reçoit un **constat
déterministe** dérivé du seul état de preuve (aucun trou silencieux). Garde-fous
existants : anti-injection (LLM01), redaction PII (LLM06), audit caviardé (A09),
provider allow-listé (A10). Domaine pur ; contrats import-linter respectés.

### 2. Orchestration + persistance côté `api-quality-engine`

Nouveau module clean/hexagonal `standards.auditblanc` :

- **domain** (pur, sans Spring/JPA) : value objects `MockAuditClause` (avec
  `riskScore`/`criticality`), `MockAuditQuestion`, `ClauseGapFinding`,
  `RemediationAction` ; agrégat `MockAuditRun` ; services purs `MockAuditAssembler`
  (gap analysis — **criticité par la règle, texte par l'IA**, tri par risque) et
  `RemediationPlanner` (plan actionnable, observations exclues, écart orienté vers
  le module QualitOS pertinent — DOCUMENT_CONTROL / TRAINING / AUDIT / PDCA selon
  l'état de preuve et le libellé de clause). Ports : `MockAuditGenerator`,
  `MockAuditRunRepository`.
- **application** : `MockAuditService` (cas d'usage `run`/`get`/`history`), ports
  `MockAuditAdoptionLookup`, `MockAuditTenantProvider`, `MockAuditActorProvider`,
  `MockAuditEventPublisher`.
- **infrastructure** : `TenantEvidenceAdoptionLookup` (calcule la matière à partir
  des **preuves réelles** liées par le tenant — table `requirement_evidences` —
  via le moteur d'alignement §8.7), `AiGatewayMockAuditGenerator` (appelle la
  passerelle IA réelle `AiGatewayClient.mockAudit` → ai-service, **aucune
  question/écart en dur**), adapter JPA (questions/écarts/plan en JSON `TEXT` via
  `@JdbcTypeCode(LONGVARCHAR)`, jamais `@Lob`), providers tenant/acteur, publisher
  d'audit chaîné.
- **presentation** : `MockAuditController`
  (`/api/v1/standards/adoptions/{adoptionId}/audit-blanc-ia`), `@PreAuthorize` par
  endpoint, mapping d'erreurs RFC 7807 scopé au contrôleur.

### 3. Sécurité (OWASP / §18.2)

- Le **tenant** provient TOUJOURS du JWT (`TenantContext`), jamais du body (#2) ;
  l'adoption est chargée par `findByIdAndTenantId` → une adoption d'un autre tenant
  donne un 404 (A01) ; les preuves confrontées sont **strictement** celles de
  l'adoption du tenant courant (testé : non-fuite cross-tenant).
- L'**acteur** (auteur de l'audit blanc) est le **sujet du JWT** (`CurrentUser`),
  jamais un identifiant du body (#5, A01).
- Le lancement (effet de bord : appel LLM + persistance + journalisation) est
  réservé au **Manager Qualité / Directeur Qualité / Auditeur+** ; la relecture à
  tout authentifié du tenant (§16).
- Tout appel LLM passe par la passerelle `AiGatewayClient` (#4, op `mock-audit`
  cloisonnée par le garde-fou de débit/quota par tenant).

## Conséquences

- **Positif** : différenciateur fort (§1.4) ; l'auditeur prépare l'audit réel avec
  des questions et un plan d'action générés depuis ses **propres preuves** ;
  rapport persisté, journalisé (auditable, ancrable). IA explicable (§12.3) :
  questions/constats par l'IA, criticité/plan déterministes et reproductibles.
- **Limite** : la pertinence des questions dépend du modèle servi (CPU/Ollama en
  dev) ; le bornage de tokens (ADR 0014) et le parser défensif garantissent une
  dégradation propre (constats déterministes si la sortie LLM est inexploitable).
- **Migration** : `V89__create_standard_mock_audits.sql` (CHECK sur readiness ∈
  [0,100] et décomptes ≥ 0).

## Tests d'invariant

- **Golden-master** `MockAuditGoldenPathTest` : adoption + jeu de preuves réelles →
  matière reflétant l'état de preuve → questions ciblées → gap analysis (8.1
  majeure, 7.5 mineure, 4.1 observation) → plan de remédiation (2 actions, majeure
  en tête, modules corrects) → persistance + relecture identiques.
- Tenant-from-JWT et non-fuite cross-tenant testés (`MockAuditServiceTest`,
  `TenantEvidenceAdoptionLookupTest`, `MockAuditRunRepositoryAdapterTest`).
- Couverture ≥ 98 % (lignes ET branches) sur le module ajouté (engine JaCoCo +
  ai-service pytest `--cov-branch`).

## Références

- CLAUDE.md §8.4 onglet 7, §8.7, §12.1, §12.3, §16, §18.2 ; ADR 0014 (passerelle
  IA, bornage tokens), ADR 0032 (génération doc normatif IA — même patron hexagonal).
