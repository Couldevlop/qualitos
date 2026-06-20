# edge-inference — Inférence locale de l'Edge Gateway (CLAUDE.md §9.5)

Composant Python autonome qui exécute l'inférence **au plus près du capteur**,
sur l'appliance K3s du site, et met en **store-and-forward** les anomalies vers
le IoT Hub. Il comble la brique « Inférence IA locale : modèles ONNX/TFLite
embarqués, store-and-forward, mode déconnecté » du §9.5.

## Principe : ONNX réel, sinon repli déterministe

Même pattern que le service Vision 5S (`apps/ai-vision-5s`) :

- si `EDGE_ONNX_MODEL_PATH` pointe vers un modèle ONNX chargeable (avec
  `onnxruntime` installé) → backend **ONNX réel** ;
- sinon → backend **déterministe à seuils** (`[low, high]` par métrique), pur,
  sans dépendance, toujours disponible. La ligne reste protégée même sans modèle.

La sélection est journalisée au démarrage (`backend.factory selected=...`).

## Architecture (hexagonale légère)

```
edge_inference/
├── domain/                 # PUR — stdlib uniquement, 100 % testable
│   ├── models.py           # TelemetrySample, InferenceResult, HubEvent
│   ├── buffer.py           # StoreAndForwardBuffer (FIFO, borné, persistable, mode déconnecté)
│   └── inference.py        # ThresholdFallbackBackend + InferenceOrchestrator
└── infrastructure/         # ADAPTATEURS — I/O et deps optionnelles
    ├── file_store.py       # EventStore durable (JSONL append-only, atomic replace)
    ├── onnx_backend.py     # OnnxInferenceBackend (import paresseux d'onnxruntime)
    └── backend_factory.py  # composition root : ONNX-ou-repli
```

Flux : `TelemetrySample → orchestrateur → InferenceResult` ; si anomalie →
`HubEvent` mis en file (store-and-forward). **Aucun appel réseau ici** : la
remontée mTLS est assurée par le bridge mosquitto (`infra/edge/k3s`). Ce module
ne fournit que la file + le contrat de l'événement.

## Ce qui est réel vs délégué

| Élément | État |
| --- | --- |
| Buffer store-and-forward (FIFO, borne, rejeu ordonné, mode déconnecté, persistance) | **réel, testé** |
| Repli déterministe à seuils (score gradué, sévérité) | **réel, testé** |
| Sélection ONNX-vs-repli + dégradation gracieuse | **réel, testé** (ONNX mocké) |
| Adaptateur ONNX (préproc + scoring) | **réel** ; chemin testé par mock d'`onnxruntime` |
| Modèle ONNX entraîné | **à fournir** (hors repo) |
| Runtime long-running (souscription MQTT → orchestrateur) | **délégué** au déploiement (cf. runbook) |

## Lancer les tests

```bash
cd infra/edge/inference
python -m pytest -p no:cacheprovider -o addopts=""
# avec couverture (gate 90 % sur les parties pures) :
python -m pytest --cov=edge_inference --cov-report=term-missing
ruff check .
```

`onnxruntime` n'est pas requis : les imports sont paresseux et le chemin ONNX est
testé par mock. La logique store-and-forward + repli est testée 100 % sans
dépendance lourde.

## Conteneur & déploiement

- `Dockerfile` : image distroless multi-arch (`--build-arg WITH_ONNX=true` pour
  embarquer le runtime ONNX).
- `../k3s/edge-inference.yaml` : Deployment + ConfigMap + PVC, pod durci
  (non-root, rootfs read-only, capabilities drop ALL, pas de Service entrant).
- Runbook : `docs/runbooks/edge-inference.md`.
- Décision : `docs/adr/0029-edge-inference-store-and-forward.md`.
