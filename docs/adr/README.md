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
