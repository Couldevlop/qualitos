# Statut P1 — MVP démontrable end-to-end

> Mise à jour : 2026-05-15. Cf. CLAUDE.md §17 (roadmap).

## ✅ Livré

### Backend (Java 21 + Spring Boot 3.5)

- **api-core** : multi-tenant, users, JWT/Keycloak.
- **api-quality-engine** : 8 modules métier
  - PDCA, Ishikawa, 5S, CAPA
  - Document Control, Audit Management
  - Standards Hub (ISO 9001 + ISO 27001 seedés)
  - Cercle de Qualité
- **551 tests Java** (unitaires + slice + callbacks). JaCoCo gate ≥85% lignes / 75% branches.
- 11 migrations Flyway versionnées.
- RFC 7807 ProblemDetail uniforme.
- Multi-tenant invariant : `tenant_id` **toujours** via JWT, jamais via body (cf. ADR 0001).

### Frontend (Angular 18 + Material 3)

- 8 features lazy-loaded : Home, Dashboard, PDCA, Ishikawa, 5S, CAPA, Audits, Standards Hub, Cercles.
- Architecture NgModules + HTML/SCSS séparés (cf. ADR 0002), aucun standalone.
- Mode "mock-first" via `environment.useMockApi` (cf. ADR 0003).
- Shell Material avec sidenav + toolbar + nav.
- Dashboard exécutif : 4 KPIs (COQ, taux NC, audits, alignement ISO) + barres d'alignement normatif.
- AuthService dual mode (dev fake JWT / oidc-ready).
- ApiInterceptor Bearer.

### Infrastructure

- **Docker Compose dev** (`docker-compose.dev.yml`) :
  - Postgres 17 + init multi-bases
  - Keycloak 25 avec realm seedé (roles + users demo/admin + mapper tenant_id)
  - Distroless Java 21 pour api-core et api-quality-engine
  - nginx pour le frontend (CSP, headers OWASP)
- **CI GitHub Actions** :
  - build-test (Maven, JaCoCo gate)
  - sast-semgrep
  - sca-owasp (gaté par `vars.OWASP_DC_ENABLED`)
  - frontend-build (npm + Karma, Chrome explicite)
  - **auto-commit du package-lock.json** en premier run (solution pérenne)
  - release.yml sur tags vX.Y.Z (SBOM CycloneDX + GitHub release)
- **Branch protection docs** + PR template + CODEOWNERS + git-workflow.

### Documentation

- README top-level (démarrage 1 commande)
- `CLAUDE.md` spécification complète
- `docs/git-workflow.md` (GitFlow, conventions commit, branch protection)
- `docs/adr/` : 3 ADR (multi-tenant JWT, NgModules, mock-first)
- `.github/CI_SETUP.md` (variables/secrets requis)

## 🟡 Connu, à reprendre

- **Frontend Karma tests** : step `Tests (Chrome headless)` en `continue-on-error`.
  Cause non diagnostiquée (logs Karma à inspecter une fois repro local possible).
  Tâche #15 dans le backlog.
- **OWASP DC** : désactivé par défaut (`vars.OWASP_DC_ENABLED`).
  À activer après obtention d'une clé NVD API (cf. `.github/CI_SETUP.md`).
- **Cache npm CI** : `cache: 'npm'` retiré temporairement (était sur lockfile inexistant).
  À ré-activer maintenant que le lockfile est committé.

## 🚧 P2 — Reste à faire pour "100% pro/premium"

### Priorité immédiate (sprint courant)

1. **Auth Keycloak réelle** : finir wiring `angular-oauth2-oidc` + remove dev mode par défaut.
2. **Backend ↔ frontend connecté** : `environment.production.ts` doit pointer sur le vrai backend ;
   tester via `docker compose up` end-to-end avec login + appels API authentifiés.
3. **Création + édition** côté UI : actuellement tous les boutons "Nouveau …" sont disabled.
4. **PDF reports** : génération côté backend (OpenPDF) + signature ML-DSA placeholder.
5. **Tests E2E** : Cypress sur le golden path (login → ouvrir PDCA → créer cycle → ajouter step).

### Méthodes restantes (CLAUDE.md §3-4)

- **DMAIC + Poka-Yoke** : projets Six Sigma, SPC charts (X-R, EWMA), Cp/Cpk, librairie Poka-Yoke.
- **Non-Conformance Management (NC)** : entité dédiée + workflow.
- **Risk/FMEA** : FMEA dynamique + bow-tie pour risques critiques.
- **Training & Competency** : parcours par rôle + certificats blockchain.
- **Change Management, Supplier Quality, Customer Complaints, Calibration, EHS**.

### Plateforme & sécurité (CLAUDE.md §11-12)

- **AI Service** (Python FastAPI) : RAG via Qdrant, suggestion cause-racine, NLQ.
- **Blockchain ancrage** : Hyperledger Fabric chaincode (Go) + service Java SDK.
- **Crypto post-quantique** : ML-KEM + ML-DSA via Bouncy Castle FIPS / liboqs.
- **OWASP ASVS L3** complet : threat models STRIDE, DAST ZAP, abuse cases.
- **Observabilité** : OpenTelemetry → Prometheus + Grafana + Loki + Tempo.
- **Multi-tenancy avancé** : RLS PostgreSQL + Hibernate `@TenantId` filter.

### UX premium (CLAUDE.md §15)

- **Design system QualitOS** : tokens Style Dictionary, Storybook publié.
- **WCAG 2.2 AA** : audit Axe + NVDA/VoiceOver.
- **PWA offline complet** pour audits 5S terrain (caméra + signature digitale + sync différé).
- **Dark mode + densités** (compact/cosy/comfortable).
- **i18n** : FR, EN, ES, AR (RTL), JA, ZH.
- **Visualisations** : Apache ECharts (cartes SPC, Pareto, heatmaps, Sankey).
- **NLQ + storyboards IA**.

### Industry Packs (CLAUDE.md §5)

- Manufacturing, Healthcare, IT/ITSM (P3)
- Pharma, Auto, Aéro, Banque, Agro, Public (P4)

### IoT (CLAUDE.md §9)

- OPC-UA + MQTT broker + HL7 FHIR
- Edge Gateway K3s blueprint
- Vision YOLOv8 (5S, EHS, défauts produit)
