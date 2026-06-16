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
