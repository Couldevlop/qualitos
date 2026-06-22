# Architecture Decision Records (ADR)

Format inspiré de [Michael Nygard / adr-tools](https://github.com/npryce/adr-tools).

Chaque décision architecturale **structurante** (touchant l'architecture, la
sécurité, le multi-tenancy, le tooling transverse) doit donner lieu à un ADR.
Conformément à CLAUDE.md §22, toute remise en cause de décision tranchée
exige un nouvel ADR (ne pas modifier les ADR acceptés — créer un ADR de
supersession).

## Index

| # | Titre | Statut |
|---|---|---|
| [0001](./0001-multi-tenant-via-jwt-claim.md) | Multi-tenant via claim JWT, jamais via le body | Accepté |
| [0002](./0002-angular-ngmodules-no-standalone.md) | Angular en NgModules (pas de composants standalone) | Accepté |
| [0003](./0003-mock-first-frontend-services.md) | Services frontend en mode "mock-first" par défaut | Accepté |
| [0011](./0011-pq-crypto-agility-signing.md) | Crypto-agility & signatures post-quantiques (ML-DSA hybride) | Accepté |
| [0012](./0012-blockchain-anchoring-fabric.md) | Ancrage blockchain : reçus signés (Phase A) → Hyperledger Fabric (Phase B) | Accepté (Phase A) |
| [0013](./0013-ai-service-distroless-python-alignment.md) | ai-service : image distroless alignée sur Python 3.11 (sans venv) | Accepté |
| [0014](./0014-ai-gateway-integration.md) | Intégration passerelle IA : SPA → api-quality-engine → ai-service (dev X-Dev-Claims, prod aud qualitos-ai) | Accepté |
| [0015](./0015-hybrid-pq-tls.md) | TLS hybride post-quantique X25519+ML-KEM-768 | Accepté |
| [0016](./0016-iot-breach-to-capa.md) | Détection seuil IoT → ouverture CAPA | Accepté |
| [0017](./0017-ai-guardrails-llm-dos.md) | Garde-fous chemin LLM (OWASP LLM04 — Model DoS) | Accepté |
| [0018](./0018-spc-anomaly-detection.md) | Détection d'anomalies SPC (règles de Nelson) dans ai-service | Accepté |
| [0019](./0019-industry-packs-schema-canonique.md) | Industry Packs : schéma canonique des packs sectoriels | Accepté |
| [0020](./0020-role-authorization-matrix.md) | Matrice d'autorisation par rôles (§16) | Accepté |
| [0021](./0021-vision-bearer-service-token.md) | Jeton de service Bearer client_credentials engine → ai-vision-5s (tenant via X-Tenant-Id, azp de confiance) | Accepté |
| [0022](./0022-unsupervised-anomaly-detection.md) | Détection d'anomalies non-supervisée multivariée (Isolation Forest + reconstruction ACP) | Accepté |
| [0023](./0023-kpi-forecasting-holt-winters.md) | Prévision KPI par lissage exponentiel Holt-Winters (NumPy pur) | Accepté |
| [0024](./0024-shap-explainability.md) | Explicabilité par Kernel SHAP (NumPy pur) — anomalies | Accepté |
| [0025](./0025-complaint-nlp-lexical.md) | NLP réclamations : sentiment lexical + classification (sans dépendance lourde) | Accepté |
| [0026](./0026-iot-lorawan-connector.md) | Connecteur d'ingestion LoRaWAN (TTN/ChirpStack), webhook décodé, tenant fail-closed | Accepté |
| [0027](./0027-iot-sparkplug-and-timescale-rollups.md) | Connecteur Sparkplug B (JSON) + rollups télémétrie & continuous aggregate TimescaleDB | Accepté |
| [0028](./0028-iot-modbus-connector.md) | Connecteur d'ingestion Modbus TCP/RTU (lecture structurée par la passerelle Edge), tenant fail-closed | Accepté |
| [0029](./0029-vision-5s-toy-onnx-real-path.md) | Modèle ONNX jouet committé + script d'export → vrai chemin d'inférence Vision 5S exercé en test (modèle de prod à fournir) | Accepté |
| [0030](./0030-edge-inference-store-and-forward.md) | Inférence Edge : store-and-forward + ONNX-ou-repli déterministe (logique pure testée ; modèle/runtime à fournir) | Accepté |
| [0031](./0031-pluggable-ml-backends-opt-in.md) | Backends ML lourds enfichables opt-in (Prophet/LSTM/HDBSCAN/BERT/Whisper) : défaut léger réel intact, import paresseux, extra `ml` ; indisponible → 422/501 | Accepté |
| [0032](./0032-generation-doc-normatif-ia.md) | Standards Hub : génération assistée IA de documents normatifs + workflow de validation humaine (§8.8) | Accepté |
| [0033](./0033-audit-blanc-ia-avance.md) | Standards Hub : audit blanc IA avancé — 30-100 questions ciblées sur les clauses à risque, gap analysis confrontée aux preuves réelles + plan de remédiation auto (§8.4 onglet 7) | Accepté |
| [0034](./0034-dashboards-interactivite.md) | Interactivité premium des dashboards (cross-filtering, drill-down, annotations collaboratives) | Accepté |
| [0035](./0035-dashboards-time-travel.md) | Time-travel des dashboards (état as-of réel des KPIs) | Accepté |
| [0036](./0036-lms-academy-gamification.md) | Academy LMS-light + gamification (cours e-learning, quiz notés, badges/ceintures, certificats ML-DSA/blockchain, SCORM/xAPI, §19.3) | Accepté |
| [0037](./0037-generation-doc-ia-multi.md) | Standards Hub : génération documentaire IA multi-documents en lot (Manuel Qualité + procédures, §8.8) | Accepté |
| [0038](./0038-dashboard-builder-dragdrop.md) | Builder de dashboard drag&drop avancé (gridster2, palette de widgets, §7.3) | Accepté |
| [0039](./0039-dast-owasp-zap-ci.md) | DAST OWASP ZAP en CI (baseline + API scan OpenAPI, seuils HIGH/MEDIUM, §11/§14.2) | Accepté |
| [0040](./0040-perf-k6-slo-ci.md) | Tests de performance k6 avec budgets SLO bloquants en CI (chemins chauds api-quality-engine, §14.3) | Accepté |
| [0042](./0042-chaos-engineering.md) | Chaos Engineering (Chaos Mesh) — expériences PodChaos/NetworkChaos/StressChaos/IOChaos + runbook, CI manuelle (§14.3) | Accepté |
| [0043](./0043-export-pdf-dashboard-signe.md) | Export PDF d'un dashboard, signé ML-DSA + ancré blockchain (QR de vérification, §7.3/§7.4) | Accepté |

## Statuts possibles

- **Proposé** — en cours de revue.
- **Accepté** — décision tranchée, à appliquer.
- **Déprécié** — encore en place mais à éviter pour le nouveau code.
- **Superposé par 00XX** — décision remplacée par un autre ADR (lien obligatoire).

## Template

```markdown
# ADR NNNN — <titre court>

- **Statut** : Proposé | Accepté | Déprécié | Superposé par NNNN
- **Date** : YYYY-MM-DD
- **Owners** : @<github-handle>

## Contexte
Pourquoi cette décision ? Quel problème ?

## Décision
La décision retenue, sans ambiguïté.

## Justification
Pourquoi cette option et pas les autres ?

## Conséquences
- ✅ effets positifs
- ⚠ contraintes / dette

## Tests d'invariant
Comment vérifier (statiquement / via tests) que la décision est respectée.

## Références
Liens CLAUDE.md / spécifications externes / autres ADR.
```
