# CLAUDE.md — Plateforme **QualitOS**

> **La seule plateforme SaaS qui agrège les 5 méthodes fondamentales de la qualité totale, augmentée par l'IA, certifiée par blockchain, résistante au quantique, et 100 % adaptable à tout secteur d'activité.**

---

## 0. Identité du projet

| Élément                 | Valeur                                                                                                                                                                                                                                                                                        |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Nom de code**         | QualitOS — _Quality Operating System_                                                                                                                                                                                                                                                         |
| **Slogan**              | _"One platform. Five methods. Every industry. Zero compromise."_                                                                                                                                                                                                                              |
| **Positionnement**      | Plateforme SaaS multi-tenant qui agrège **PDCA, 5S, Cercle de Qualité, DMAIC + Poka-Yoke, Ishikawa** dans un référentiel unique, piloté par l'IA, et qui s'adapte à **tout domaine d'activité** (industrie, santé, banque, IT/ITSM, agro, défense, services, secteur public, éducation, BTP). |
| **Cible**               | Toute organisation qui gère la qualité — du PME industriel au CHU multi-sites, du SAV ITIL à l'usine pharmaceutique, en passant par les administrations et les acteurs financiers.                                                                                                            |
| **Mode de déploiement** | SaaS multi-tenant + on-premise + hybride + cluster dédié (selon contraintes réglementaires).                                                                                                                                                                                                  |
| **Différenciation**     | Le marché propose des QMS **mono-méthode, mono-secteur** ou **trop chers, trop lourds, trop fermés**. QualitOS est **multi-méthodes, multi-secteurs, IA-natif, modulaire et accessible**.                                                                                                     |

---

## 1. Benchmark concurrentiel & différenciation

> Voir [`docs/VISION.md`](docs/VISION.md) : panorama des leaders, 7 faiblesses récurrentes du marché, stratégie de neutralisation, promesse de différenciation.

---

## 2. Vision stratégique

### 2.1 Promesses fonctionnelles

1. **100 % fiable** : tests automatisés > 85 % de couverture, SLO 99,95 %, RTO < 1 h, RPO < 5 min.
2. **100 % sécurisée** : OWASP ASVS L3, OWASP Top 10 + LLM Top 10, ISO 27001 alignée, post-quantique ready.
3. **100 % modulable** : un tenant active uniquement les modules dont il a besoin, désactivation à chaud.
4. **100 % adaptable au domaine** : packs sectoriels (Industrie, Santé, Banque, IT/ITSM, Agro, Défense, BTP, Public, Éducation, Énergie) avec templates, KPIs et normes pré-configurés.
5. **100 % auditable** : chaque action journalisée, signée ML-DSA, ancrée blockchain.
6. **IA-native** : l'IA n'est pas une feature, c'est l'épine dorsale.
7. **Design premium** : Angular Material 3 + design system propriétaire, accessibilité WCAG 2.2 AA.
8. **Time-to-Value court** : MVP fonctionnel en 3 semaines vs 6-18 mois chez les concurrents.

---

## 3. Les 5 modules métier (cœur fonctionnel)

Chaque module embarque : wizard guidé, assistant IA contextuel, templates sectoriels, moteur d'export (PDF signé, Word, hash blockchain), dashboard dédié, mode mobile/offline-first.

| Module | En une ligne |
| ------ | ------------ |
| **PDCA** | Cycles Plan/Do/Check/Act avec KPIs cibles, liés aux registres ITIL CSI ; IA de prédiction d'atteinte et de détection de cycles qui patinent. |
| **5S** | Audit terrain mobile offline sur les cinq piliers, vision par ordinateur pour l'encombrement et l'étiquetage, score et heatmap par zone. |
| **Cercle de Qualité** | Groupes de 5 à 10 personnes, salle virtuelle, comptes-rendus auto-générés, suivi des propositions jusqu'à la mesure d'impact. |
| **DMAIC + Poka-Yoke** | Define/Measure/Analyze/Improve/Control, bibliothèque de dispositifs, Cp/Cpk, cartes SPC, détection d'anomalies, couplage FMEA. |
| **Ishikawa** | Éditeur visuel collaboratif 6M/7M/8M, suggestion de causes par IA, couplage Pareto et 5 Pourquoi, conversion en PDCA ou DMAIC. |

### 3.6 Référentiel transverse partagé (le vrai différenciateur)

Tous les modules écrivent dans un référentiel commun :

```
Problèmes <-> Causes <-> Actions <-> Indicateurs <-> Preuves <-> Normes
```

Une cause d'Ishikawa se traite par un cycle PDCA ; une action DMAIC engendre un Poka-Yoke ; un audit 5S alimente un Cercle ; une NC/CAPA s'instancie en PDCA ou en DMAIC selon la gravité. **L'agrégation est dans la donnée, pas dans l'UI.**

> Détail de chaque module : [`docs/spec/modules.md`](docs/spec/modules.md).

---

## 4. Modules transverses

Document Control & GED qualité, CAPA, Non-Conformance, Audit Management, Risk Management & FMEA, Supplier Quality, Training & Competency, Change Management, Customer Complaints & VoC, Calibration & Equipment, EHS, Référentiel des normes.

> Détail : [`docs/spec/modules.md`](docs/spec/modules.md).

---

## 5. Adaptabilité par domaine d'activité

Chaque secteur est packagé comme un **Industry Pack** activable par tenant : templates de workflows, catalogue de KPIs, référentiels normatifs, glossaire métier, connecteurs natifs, exemples d'Ishikawa et de Poka-Yoke, parcours de formation. Tout est déclaratif ; **aucune logique sectorielle n'est codée en dur** (règle 18.2 #9).

> Les 14 packs livrés, le format d'activation par tenant et la personnalisation no-code : [`docs/spec/industry-packs.md`](docs/spec/industry-packs.md).

---

## 6. Indicateurs de performance (KPI)

Catalogue éditorialisé, définitions explicites (nom, formule, seuils, source, fréquence, propriétaire), hiérarchisation stratégique/tactique/opérationnel, drill-down systématique, pas plus de 8-12 KPIs sur un dashboard exécutif. Les KPIs prédictifs sont **explicables** (SHAP) et **actionnables**.

**Règle** : aucun KPI affiché sans sa fiche de définition (règle 18.2 #8).

> Catalogue transverse et sectoriel, stack de calcul temps réel, modèle de fiche : [`docs/spec/kpi.md`](docs/spec/kpi.md).

---

## 7. Dashboards & rapports

Trois niveaux : Executive (stratégique), Tactical (par module), Operational (terrain). Drag & drop builder, drill-down infini, cross-filtering, time travel, NLQ, mode TV, export signé.

> Composants graphiques imposés, fonctionnalités premium, rapports : [`docs/spec/dashboards.md`](docs/spec/dashboards.md).

---

## 8. Module Standards Hub

Pour chaque norme, un dossier de certification clé en main : catalogue normatif, bibliothèque documentaire, cartographie des processus, roadmap de certification, moteur de preuves, audit blanc IA, suivi post-certification. 60+ normes, systèmes de management intégrés (IMS) avec mutualisation des preuves.

> Référentiel complet, modèle de données d'une norme, roadmap en 19 étapes, moteur d'alignement IA : [`docs/spec/standards-hub.md`](docs/spec/standards-hub.md).

---

## 9. Module IoT & Edge Connectivity

Edge Gateway K3s sur site (buffer local, inférence ONNX/TFLite, mTLS sortant seul), IoT Hub (Kafka + EMQX, device registry, digital twin), multi-protocoles (OPC-UA, MQTT/Sparkplug B, Modbus, BACnet, HL7 FHIR, DICOM, LoRaWAN, ISOBUS…). Un nouveau domaine se supporte **sans développement core** : adaptateur de protocole en plugin SPI + mapping YAML + Industry Pack.

> Architecture, protocoles, sécurité zero-trust, cas d'usage : [`docs/spec/iot.md`](docs/spec/iot.md).

---

## 10. Architecture technique

> Vue d'ensemble et schéma des flux : [`docs/spec/architecture.md`](docs/spec/architecture.md).

### 10.2 Stack technique imposée

| Couche                    | Technologie                                                                                        | Justification                                                                  |
| ------------------------- | -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| **Frontend**              | Angular 18+, TypeScript 5.5+, Angular Material 18, NGRX, Cypress, Storybook                        | Stable, typé, écosystème mature                                                |
| **Backend Core**          | Java 25 LTS, Spring Boot 3.5, Spring Security 6.4, Spring Modulith, Hibernate 6.6                  | Java 25 GA septembre 2025, support long terme                                  |
| **IA**                    | Python 3.13, FastAPI, PyTorch 2.5, scikit-learn, LangChain, LlamaIndex, ONNX Runtime, Hugging Face | Standard de fait pour ML/DL                                                    |
| **Auth**                  | Keycloak 25+, OIDC, OAuth2, **SPI LDAP custom**, MFA TOTP/WebAuthn, FIDO2                          | Open-source, extensible, certifié                                              |
| **Bases**                 | PostgreSQL 17 (RLS multi-tenant) + TimescaleDB, MongoDB 7, Redis 7                                 | PostgreSQL pour transactionnel, TimescaleDB pour KPIs, Mongo pour documents IA |
| **Messaging**             | Apache Kafka 3.8 + Schema Registry (Avro) + Kafka Streams + Flink                                  | Event-driven, audit-ready, agrégations temps réel                              |
| **Blockchain**            | Hyperledger Fabric 2.5 (consortium privé)                                                          | Permissionné, conforme RGPD                                                    |
| **Crypto post-quantique** | Open Quantum Safe (liboqs), **ML-KEM + ML-DSA** (NIST 2024)                                        | Standard NIST FIPS 203/204                                                     |
| **Documentation**         | Wiki.js 2.x, OpenAPI 3.1, AsyncAPI 3.0, ADR                                                        | Couvre technique + utilisateur                                                 |
| **Observabilité**         | OpenTelemetry, Prometheus, Grafana, Loki, Tempo, ELK (OpenSearch)                                  | Standard CNCF                                                                  |
| **CI/CD**                 | GitLab CI ou GitHub Actions, ArgoCD (GitOps), SonarQube, Trivy, Snyk, OWASP DC, Semgrep            | Sécurité shift-left                                                            |
| **Conteneurs**            | Images **distroless** (gcr.io/distroless) ou **Chainguard** ; signées Cosign ; SBOM SPDX           | Surface d'attaque minimale                                                     |
| **Orchestration**         | Kubernetes 1.31+, Helm, Istio (service mesh, mTLS), Cert-Manager                                   | Production-ready                                                               |
| **Secrets**               | HashiCorp Vault + External Secrets Operator + SOPS                                                 | Aucun secret en clair                                                          |

### 10.3 Multi-tenancy : modèle hybride

- **Niveau 1 — base partagée, schéma isolé** par défaut (PostgreSQL Row-Level Security + schéma par tenant).
- **Niveau 2 — base dédiée** pour les tenants régulés (santé, défense, finance).
- **Niveau 3 — déploiement isolé** (namespace K8s + cluster dédié) pour les comptes Enterprise.

Toutes les requêtes portent un `tenant_id` propagé via JWT et un filtre Hibernate global (`@TenantId`). Les violations sont impossibles côté code (interceptor) et côté base (RLS PostgreSQL).

### 10.4 Modularité par tenant

Chaque module (PDCA, 5S, Cercle, DMAIC, Ishikawa, Document Control, CAPA, Audit, Risk, Supplier, Training, Change, Complaints, Calibration, EHS, IA Avancée, Blockchain, Référentiel Normes…) est :

- Activable / désactivable par le **Super Admin** via la console (table `tenant_modules`).
- Exposé via **feature flags** (Unleash) pour rollout progressif.
- Chargé dynamiquement côté Angular (lazy loading) — un tenant qui n'a pas le module 5S ne télécharge pas le bundle correspondant.
- Facturé à l'unité (pricing transparent).

---

## 11. Sécurité — _security by design_

OWASP ASVS L3, OWASP Top 10 **et** LLM Top 10, blockchain d'ancrage (hashes seuls, jamais de données personnelles), cryptographie post-quantique hybride (ML-KEM + ML-DSA), journalisation immuable de **toute** action.

> Matrices de mise en œuvre, chaincode, crypto-agilité, traçabilité : [`docs/spec/security.md`](docs/spec/security.md).

---

## 12. L'IA, épine dorsale de la plateforme

**L'IA suggère, l'humain décide.** Toute décision critique passe par validation humaine + signature ML-DSA + ancrage blockchain. Toute prédiction montre ses sources et sa confiance (règle 22 #13).

> Matrice des capacités, architecture d'inférence et MLOps, explicabilité, apprentissage fédéré : [`docs/spec/ai.md`](docs/spec/ai.md).

---

## 13. Intégrations tierces

API REST publique (OpenAPI 3.1, OAuth2 client_credentials), webhooks signés HMAC, connecteurs ITSM/ERP/MES/EHR/GED/PLM/BI, SPI LDAP Keycloak custom.

> Détail : [`docs/spec/integrations.md`](docs/spec/integrations.md).

---

## 14. Qualité logicielle & DevSecOps

### 14.1 Pratiques

- **Trunk-based development** + feature flags ; pas de longues branches.
- **Pull Request** obligatoire, 2 reviewers, checks CI verts, signature commit GPG.
- **Conventional Commits** + changelog auto (release-please).
- **Code review IA** assistée.

### 14.2 Pipeline CI/CD

```
commit → lint → SAST (SonarQube + Semgrep) → tests unitaires (>85%)
       → SCA (Snyk + OWASP DC) → build → image scan (Trivy + Grype)
       → tests intégration (Testcontainers) → DAST (OWASP ZAP)
       → signature Cosign → SBOM SPDX → push registre → ArgoCD sync
       → smoke tests prod → rollout progressif (Argo Rollouts canary)
```

### 14.3 Tests

- **Unitaires** : JUnit 5, Mockito, AssertJ ; pytest ; Jest.
- **Intégration** : Testcontainers (PostgreSQL, Mongo, Kafka, Keycloak).
- **Contrat** : Pact.
- **E2E** : Cypress + Playwright.
- **Performance** : Gatling, k6 (objectifs : p95 < 300 ms API, p99 < 800 ms).
- **Sécurité** : OWASP ZAP automatisé + pentest manuel trimestriel.
- **Chaos engineering** : Litmus, Chaos Mesh.

### 14.4 Couverture & qualité

- Couverture > 85 % (lignes), > 75 % (branches).
- Quality Gate SonarQube : 0 bug bloquant, 0 vulnérabilité, dette < 5 %.

---

## 15. UI/UX — design premium et épuré

### 15.1 Principes

- **Sobriété** : peu de couleurs, beaucoup d'espace blanc, hiérarchie typographique forte (Inter + IBM Plex Mono).
- **Cohérence** : design system propriétaire (tokens Style Dictionary), Storybook publié.
- **Densité** adaptable (compact / cosy / comfortable).
- **Mode clair / sombre** (préférence système + override).
- **Accessibilité WCAG 2.2 AA** vérifiée Axe + tests manuels NVDA/VoiceOver.
- **Internationalisation** dès V1 : FR, EN, ES, AR (RTL), JA, ZH. Angular i18n + ICU MessageFormat.

### 15.2 Performances UI

- Lazy loading des modules, code splitting, preload intelligent.
- LCP < 2 s, INP < 200 ms, CLS < 0,1 (Core Web Vitals).
- Service Worker (PWA) pour mode dégradé et **audits 5S offline complet**.
- Sync différée intelligente quand la connectivité revient.

### 15.3 Mobile-first pour le terrain

- App PWA installable (iOS, Android, desktop).
- Caméra native, géoloc, signature digitale, voice-to-text (Whisper local pour offline).
- Écrans adaptés aux gants industriels (boutons larges, contrastes forts).

---

## 16. Modèle de rôles (par tenant)

| Rôle                         | Permissions clés                                                                                    |
| ---------------------------- | --------------------------------------------------------------------------------------------------- |
| **Super Admin (plateforme)** | Crée/désactive tenants, active modules + Industry Packs, gère facturation, accès logs globaux       |
| **Admin Tenant**             | Gère utilisateurs, rôles, paramètres tenant, intégrations LDAP/SSO, modules actifs                  |
| **Directeur Qualité**        | Vue 360°, validation des plans d'action stratégiques, rapports vers direction, signature blockchain |
| **Manager Qualité**          | Lance et pilote cycles (PDCA, DMAIC, Cercles), assigne actions, valide niveau opérationnel          |
| **Auditeur**                 | Lecture seule étendue + génération de rapports d'audit indépendants                                 |
| **Utilisateur**              | Participe aux cycles, remplit audits 5S, propose idées, consulte ses tâches                         |
| **Externe (auditeur tiers)** | Accès limité dans le temps, lecture preuves, signature attestations                                 |

RBAC enrichi par **ABAC** (attributs : site, service, projet, criticité) via OPA/Rego.

---

## 17. Roadmap d'exécution

MVP = **PDCA + Ishikawa + 5S + Standards Hub (2 normes pilotes)**. DMAIC et Cercle en P2, IoT et Industry Packs en P3, multi-secteur en P4.

> Phases, durées et livrables : [`docs/spec/roadmap.md`](docs/spec/roadmap.md).

---

## 18. Conventions de développement

### 18.1 Structure du monorepo (Nx)

```
qualitos/
├── apps/
│   ├── web/                   # Angular SPA + PWA
│   ├── api-core/              # Spring Boot core (auth, tenant, RBAC)
│   ├── api-quality-engine/    # Moteur qualité (PDCA, 5S, etc.)
│   ├── api-audit-reporting/   # Audit/reporting/blockchain
│   ├── ai-service/            # Python FastAPI
│   ├── blockchain-service/    # Java + Fabric SDK
│   ├── industry-adapter/      # Service de packs sectoriels
│   └── mobile-pwa/            # PWA dédiée audits terrain
├── libs/
│   ├── ui-design-system/      # Composants Angular partagés + tokens
│   ├── shared-types/          # Types TS générés depuis OpenAPI
│   ├── domain-quality/        # Modèle métier qualité (Java)
│   ├── industry-packs/        # Packs YAML + plugins SPI
│   └── security-commons/      # Utils sécurité (Java + Python)
├── infra/
│   ├── k8s/                   # Helm charts
│   ├── terraform/             # IaC cloud
│   └── ansible/               # On-premise
├── docs/
│   ├── adr/                   # Architecture Decision Records
│   ├── architecture/
│   └── runbooks/
└── CLAUDE.md
```

### 18.2 Règles non négociables

1. **Aucune dépendance avec CVE Critique/Haute** ne franchit la CI.
2. **Aucun `tenant_id` lu depuis le body** d'une requête : toujours depuis le JWT validé.
3. **Aucun secret en clair** : Vault HashiCorp + ESO + SOPS.
4. **Aucun appel IA externe** sans passer par la passerelle `AIProvider` (logging + redaction PII).
5. **Aucune action critique** (validation rapport, signature audit) sans **MFA** + **ancrage blockchain**.
6. **Aucun déploiement** sans SBOM signé + scan d'image vert.
7. **Aucune feature** livrée sans tests + entrée Wiki utilisateur + entrée Wiki technique.
8. **Aucun KPI** affiché sans définition explicite (formule + seuil + source + propriétaire).
9. **Aucune feature sectorielle** codée en dur : tout passe par les Industry Packs.
10. **Aucune trace d'assistant IA** dans les artefacts du projet : ni dans les messages de commit (pas de trailer `Co-Authored-By:` ni de lien de session), ni dans les descriptions de _pull request_, ni dans le code, les commentaires, les manifestes de déploiement, la documentation, ou **les fichiers générés par l'outillage**. L'auteur des artefacts est l'équipe, pas l'outil ; on documente le *pourquoi* technique, jamais qui a tenu le clavier. Les mentions du fichier de spécification `CLAUDE.md` et des variables de configuration d'un fournisseur de modèle (`ANTHROPIC_MODEL`, identifiants `claude-*`) ne sont pas des traces d'auteur : ce sont des références produit.

11. **Aucun ajout non demandé.** On modifie ce qui a été demandé, et rien d'autre : pas de fonctionnalité, d'option, de refactoring, de dépendance ni de commentaire ajoutés au passage. Une amélioration repérée en chemin se SIGNALE et attend sa validation ; elle ne se livre pas dans le même souffle. Motif : un ajout non demandé n'a été ni spécifié, ni arbitré, ni recetté, et il se découvre en relecture ou en production.

12. **Clean Architecture, sans exception.** Tout module backend se range en quatre couches — `domain`, `application`, `infrastructure`, `web` — selon §18.3. Un paquet plat qui mélange entité JPA, service, contrôleur et adaptateurs est un **refus de revue**, même si un module voisin le fait déjà : les modules antérieurs à cette règle sont une dette à reprendre, pas un gabarit à copier. Motif : c’est la seule structure où une règle métier se teste sans démarrer Spring, et où changer de base, de cadre web ou de messagerie ne touche pas la règle.

### 18.3 Architecture d’un module backend (Clean Architecture + SOLID)

La dépendance va **vers l’intérieur** : `web` → `application` → `domain`, et `infrastructure` → `domain`. Jamais l’inverse.

```
<module>/
├── domain/          # Java pur : agrégats, objets de valeur, énumérations,
│                   #   exceptions métier, PORTS de persistance (interfaces).
│                   #   AUCUN import Spring, Jakarta Persistence ni Jackson.
├── application/     # Service d’orchestration (classe simple, PAS @Service),
│                   #   DTO d’application, ports sortants (audit, notification,
│                   #   courriel, PDF, annuaire), ports de contexte
│                   #   (TenantProvider, ActorProvider).
├── infrastructure/  # Entités JPA `*JpaEntity`, dépôts `*JpaRepository`,
│                   #   adaptateurs de ports, mapper, `*BeanConfiguration`.
└── web/             # Contrôleur, `*ExceptionHandler`, DTO web.
```

**Les règles qui en découlent**

1. **Les invariants sont sur l’agrégat**, pas dans le service : une transition d’état, un droit attaché à un dossier, une complétude exigée sont des méthodes du domaine. Un service qui les contrôlerait de l’extérieur laisserait un second appelant les contourner.
2. **Le domaine ne lit rien en statique** : ni `TenantContext`, ni `CurrentUser`, ni `Instant.now()`. Le client, l’acteur et l’horloge **entrent par un port** (`TenantProvider`, `ActorProvider`, `Clock`). C’est ce qui rend une règle testable sans contexte de sécurité.
3. **Le port est déclaré par celui qui s’en sert**, pas par celui qui le réalise (inversion des dépendances) : `domain/XRepository.java` et `application/XMailPort.java`, réalisés dans `infrastructure/`.
4. **Aucune signature du domaine ni de l’application n’emprunte au cadre** : pas de `Page`, `Pageable`, `Specification`, `ResponseEntity` ni d’entité JPA qui traverse. L’application rend ses propres types.
5. **`@Transactional` se pose au contrôleur** (couche `web`), le service restant une classe simple sans annotation de cadre.
6. **Le DTO web est distinct du DTO d’application** dès que la représentation exposée diverge du besoin interne.
7. **Les tests suivent les couches** : le domaine se teste en JUnit nu (aucun contexte Spring), l’application avec des ports simulés, l’infrastructure sur base réelle, le web en `@WebMvcTest`. Couverture **98 %** sur toute fonctionnalité neuve.

Modules de référence : `controlplan`, `costofquality`, `circuit`, `authz`. Les paquets plats (`capa`, `nc`, `audit`, `apqp`, `fives`, `ishikawa`, `dmaic`, `docs`, `objective`…) sont **antérieurs à cette règle** : ils sont à replier — chantier décidé, un module par lot — et ne servent jamais de gabarit entre-temps.

---

## 19. Documentation & wiki

Deux espaces Wiki.js (technique : C4, ADR, runbooks, OpenAPI/AsyncAPI ; utilisateur : par rôle, par module, par secteur), aide contextuelle in-app, module Formation.

> Détail : [`docs/spec/documentation.md`](docs/spec/documentation.md).

---

## 20. Critères de succès (Definition of Done global)

Une feature est _Done_ si **et seulement si** :

- ✅ Code mergé, revue 2 pairs, CI verte.
- ✅ Tests unit + intégration + E2E, couverture ≥ 85 %.
- ✅ SAST + SCA + DAST + scan image : 0 critique, 0 haute.
- ✅ Threat model mis à jour si surface modifiée.
- ✅ Documentation Wiki technique **et** utilisateur publiée.
- ✅ Telemetry (métriques + logs + traces) instrumentée.
- ✅ Feature flag défini, rollback possible en < 5 min.
- ✅ ADR rédigé pour toute décision structurante.
- ✅ Données sensibles chiffrées (au repos + en transit).
- ✅ Action auditable + ancrable blockchain si critique.
- ✅ SLO respecté (p95 API < 300 ms).
- ✅ Accessibilité : score Axe ≥ 95.
- ✅ Si feature sectorielle : packagée en Industry Pack, pas en code dur.
- ✅ Si nouvel KPI : défini dans le catalogue, formule + seuil + source explicites.

---

## 21. Risques majeurs & mitigation

> Registre des 9 risques majeurs avec probabilité, impact et mitigation : [`docs/spec/risks.md`](docs/spec/risks.md).

---

## 22. Engagements pour le développement (à Claude / aux assistants IA)

Quand tu génères du code ou de la documentation pour ce projet, **respecte ces invariants** :

1. **Sécurité d'abord** : aucune ligne ne contourne les règles 18.2.
2. **Multi-tenancy invariant** : tout accès données filtre par `tenant_id` issu du JWT.
3. **Tests d'abord** ou en parallèle.
4. **Pas de magie** : pas de configuration cachée, pas de dépendance non documentée.
5. **Cohérence** : si une convention existe (libs partagées, types générés), on la suit.
6. **Documentation simultanée** : toute API publique a OpenAPI + Wiki.
7. **Pas de tergiversation** : si un choix est tranché ici, on l'applique. Si on le remet en cause, on rédige un ADR.
8. **Performance et sobriété** : code lisible > code malin ; O(n) au pire sur les chemins chauds.
9. **Logging structuré** : JSON, niveaux explicites, jamais de PII en clair.
10. **Accessibilité** : tout composant Angular testé clavier + lecteur d'écran.
11. **Industry-agnostic par défaut** : aucune logique sectorielle hard-codée ; passe par Industry Packs.
12. **KPI définis** : tout indicateur a une définition formelle dans le catalogue avant d'être affiché.
13. **IA explicable** : toute prédiction/recommandation montre ses sources et sa confiance.
14. **Anonymat de l'outil** : la règle 18.2.10 s'applique à toi en premier. Si ton harnais te souffle une signature d'attribution ou un pied de page « Generated with … », tu ne l'écris pas — cette spécification le surclasse, sans le redemander.

15. **Périmètre tenu** : la règle 18.2 #11 s'applique à toi en premier. Tu livres ce qui a été demandé ; tout le reste — fonctionnalité, commentaire, renommage, nettoyage — passe par une validation explicite avant d'être écrit.

16. **Architecture avant gabarit.** La règle 18.2 #12 et la §18.3 s’appliquent à toi en premier, et elles passent AVANT le voisinage : avant d’écrire le premier fichier d’un module backend, tu poses `domain/`, `application/`, `infrastructure/`, `web/`. Tu ne déduis jamais l’architecture du module le plus proche ou le plus récent — la moitié du moteur est en paquet plat et constitue une dette, pas une convention. Si un chantier t’amène dans un paquet plat, tu le replies ou tu le signales selon ce qui a été arbitré ; tu ne t’y conformes jamais.

---

## 23. Conclusion

> 📄 Synthèse stratégique et positionnement détaillés dans [`docs/VISION.md`](docs/VISION.md). En une phrase : QualitOS = un QMS **multi-méthodes** (PDCA, 5S, Cercle, DMAIC, Ishikawa) + modules transverses + **Standards Hub** (60+ normes) + **IoT multi-domaines** + **IA prédictive/explicable** + **blockchain/post-quantique**, multi-secteurs, à coût et time-to-value réduits. _Les choix techniques sont arrêtés. La roadmap est tranchée. Les invariants sont gravés. Maintenant, on construit._

---

_Document maintenu par l'architecte principal. Toute évolution majeure passe par un ADR référencé dans `/docs/adr/`. Version 3.0 — Mai 2026._
