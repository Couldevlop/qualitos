# Audit « stub vs réel » — capacités IA / IoT / Blockchain-Crypto

> Date : 2026-05-29. Méthode : lecture du code décisif (logique effective vs simulée), pas de la doc.
> Classement : **RÉEL** (logique effective) · **STUB** (structure, sortie factice/déterministe) ·
> **SCAFFOLD** (squelette opt-in / non branché) · **DOCUMENTAIRE** (config/suite définie, non activée) ·
> **ABSENT** (aucun code).

## Tableau consolidé

| Capacité | Verdict | Preuve (code décisif) |
|---|---|---|
| **Crypto** ML-DSA-65 (signature) | RÉEL | `BouncyCastleSignatureProvider.java:119` `Signature.getInstance("ML-DSA", BC)` ; test « signature > 2000 octets » |
| **Crypto** ML-KEM-768 (KEM) | RÉEL | `BouncyCastleKemProvider.java:46` `MLKEMParameters.ml_kem_768` ; round-trip testé |
| **HybridSignatureService** (Ed25519+ML-DSA) | RÉEL | `HybridSignatureService.java:50-65` signe 2× réellement, vérifie tout |
| **Ancrage Phase A** (Merkle + reçus chaînés) | RÉEL | `SignedAnchorAdapter.java:50-85` + `MerkleTree` + `AnchorVerificationService` (7 tests) |
| **LLM providers** Ollama/Anthropic/Mistral | RÉEL | appels HTTP réels (`ollama_provider.py:40`, `anthropic_provider.py:47`, `mistral_provider.py:44`) |
| **RAG** Qdrant + BGE-M3 | RÉEL (fallback dev) | `rag_query.py:65-117` ; fallback in-memory + embedder déterministe si pas de GPU |
| **NLQ** text-to-SQL | RÉEL | `nlq_ask.py:102-151` + `sqlglot_validator.py` (SELECT-only, allow-list, tenant) + executor read-only |
| **AiGatewayClient** (relais engine→ai-service) | RÉEL | `AiGatewayClient.java:62-76` POST `/v1/ai/complete` |
| **IoT Device Registry** | RÉEL | JPA transactionnel, ISA-95, X.509 fingerprint, RLS tenant (`DeviceController`, `V1__iot_schema.sql`) |
| **IoT Ingestion télémétrie** | RÉEL | `IngestTelemetryUseCase.java:40-56` save JPA + batch ; **TimescaleDB déclaré mais inactif** |
| **Chaincode Go** AnchorAudit/VerifyEvidence | RÉEL | `qualitos_anchor.go:37-80` GetState/PutState réels, clé composite tenant |
| **Modules AI Act** aiconformity/aiincidents/aipmm | RÉEL (CRUD réglementaire) | workflows d'état ; **ce sont des modules de _gouvernance_ de l'IA (AI Act), pas des features prédictives** — pas d'appel LLM, et c'est normal |
| **Vision 5S (YOLOv8)** | STUB | `analyzer.py:34-40` scores dérivés du SHA-256 de l'image — « real YOLOv8 added in P5 » |
| **IoT protocoles terrain** (OPC-UA/MQTT/HL7/LoRaWAN…) | STUB | `Protocol.java` enum + colonne SQL ; **zéro connecteur**, entrée unique = REST HTTP |
| **IoT Digital Twin / Shadow** | STUB | `twin_json` stocké mais **réhydraté vide** en lecture (`JpaDeviceRepository:86`, TODO P4) |
| **Federated learning** (Flower) | SCAFFOLD | `opt_in_federated_client.py:27` retourne `samples_used=0` synthétique |
| **IoT Stream rule engine → NC** | ~~SCAFFOLD~~ → **RÉEL (in-engine)** | ✅ Résolu (ADR 0016, V65) : détection dans `TelemetryIngestionService` → CAPA `IOT_ALERT` (seuils configurables `/api/v1/iot/thresholds`, anti-spam, tenant via JWT). Le chemin `api-iot-hub→/nc/from-iot` reste hors périmètre. |
| **Ancrage Phase B (Fabric)** | SCAFFOLD | `FabricAnchorService` propose→endorse→submit réel **mais** `@Profile("fabric")` désactivé, **0 test**, **réseau Fabric absent** (réutilise fabric-samples externe), fallback auto → Phase A |
| **TLS hybride X25519+ML-KEM-768 (WS5)** | DOCUMENTAIRE | suite `tls-hybrid-p3` définie + `infra/tls/application-tls.yml` ref ; **aucun endpoint ne l'utilise**, `bctls-jdk18on` non importé, 0 test handshake |
| **Edge Gateway K3s (§9.5)** | ABSENT | aucun artefact (ni Helm, ni manifests, ni code edge, ni ONNX/TFLite) |
| **Prédiction LSTM/Prophet/TFT (§12)** | ABSENT | aucun import |
| **Anomalies ML (Isolation Forest/Autoencoder)** | ABSENT | aucun import |
| **Clustering NC (HDBSCAN)** | ABSENT | aucun import |
| **Explicabilité SHAP/LIME** | ABSENT | aucun import |
| **NLP audits / sentiment (BERT/Whisper)** | ABSENT | aucun import transformers/whisper |

## Lecture d'ensemble

- **Socle confiance (crypto PQ + notarisation signée Phase A)** : réellement production-ready et testé. C'est le point le plus solide.
- **Pipeline IA = LLM générique** (providers + RAG + NLQ) réel ; **toute l'IA « ML classique/prédictive » annoncée au §12 est absente** (0 LSTM, 0 anomalie, 0 clustering, 0 SHAP).
- **IoT** : registre + ingestion réels ; **universalité protocoles, edge, twin, et chaîne capteur→NC = stub/scaffold/absent**.
- **Fabric (Phase B)** : code livré et compilable, mais **non opérationnel** (profil off, pas de réseau, pas de tests).

## Dette technique priorisée

**P0 — écarts « annoncé/livré » vs réel (crédibilité & sécurité)**
1. **TLS hybride** : §11.4 annonce « TLS hybride sur flux entrants » — en réalité non activé. → soit l'activer (importer `bctls-jdk18on`, profil `tls`, test handshake `X25519MLKEM768`), soit reclasser la promesse.
2. **blockchain-service** : 0 test + Fabric non opérationnel. → test-network en CI (Testcontainers) + tests `FabricAnchorService`, ou assumer Phase A comme cible prod et marquer Phase B « expérimental ».

**P1 — profondeur des features phares**
3. ~~**Chaîne capteur→NC cassée**~~ → ✅ **Résolu** (2026-05-29, ADR 0016) : détection de seuil in-engine ouvrant une CAPA `IOT_ALERT`, seuils configurables, anti-spam. Reste : enrichir §9.9 (lien FMEA, cycle PDCA auto).
4. **Vision 5S** : stub SHA-256 → vrai YOLOv8 (swap `InferenceBackend` déjà prévu), sinon dépromettre la CV.
5. **Connecteurs protocoles IoT** : 0 connecteur (OPC-UA/MQTT/HL7/LoRaWAN). → en livrer au moins 1 (MQTT/EMQX) pour étayer l'universalité.

**P2 — capacités ML annoncées absentes**
6. **IA prédictive** : implémenter au moins 1-2 modèles réels (prédiction KPI, anomalies SPC) pour étayer « IA épine dorsale », ou recadrer §12.
7. **Federated learning** : laisser en opt-in mais ne pas survendre.

**P3 — hygiène**
8. Digital Twin réhydraté vide (TODO P4).
9. Garde-fous IA (rate-limit / circuit breaker / quotas budgétaires) non branchés sur le chemin LLM.
10. Couverture front (10 specs / 38 features) — hors périmètre de cet audit.
