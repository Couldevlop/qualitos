# Runbook — Inférence locale Edge (`edge-inference`)

Déploiement et exploitation du composant d'inférence de l'Edge Gateway K3s
(CLAUDE.md §9.5). Code : `infra/edge/inference/`. Manifeste :
`infra/edge/k3s/edge-inference.yaml`. Décision : ADR 0029.

## 1. Rôle

Sur l'appliance K3s du site, à côté du broker mosquitto local :

- consomme la télémétrie capteurs (réseau OT) ;
- exécute l'inférence **locale** (modèle ONNX si fourni, sinon repli
  déterministe à seuils) ;
- met en **store-and-forward** les anomalies (file persistée, rejeu ordonné,
  résilient aux coupures WAN) vers le IoT Hub.

Aucune connexion **entrante** (§9.8) : le composant sort uniquement (via le
bridge mTLS de mosquitto). Aucun Service Kubernetes n'est exposé.

## 2. Construire l'image (multi-arch ARM64 / x86)

```bash
# Repli déterministe seul (image minimale, pas d'onnxruntime) :
docker buildx build --platform linux/amd64,linux/arm64 \
  -t <registry>/qualitos/edge-inference:0.1.0 \
  infra/edge/inference --push

# Avec runtime ONNX embarqué (inférence réelle) :
docker buildx build --platform linux/amd64,linux/arm64 \
  --build-arg WITH_ONNX=true \
  -t <registry>/qualitos/edge-inference:0.1.0-onnx \
  infra/edge/inference --push
```

L'image est distroless/nonroot (pas de shell). Le `ENTRYPOINT` par défaut fait un
self-check (imports + backend sélectionné) ; le runtime long-running (souscription
MQTT → orchestrateur → drain) est wiré par déploiement (cf. §6).

## 3. Déployer sur K3s

```bash
kubectl apply -f infra/edge/k3s/namespace.yaml      # si pas déjà fait
kubectl apply -f infra/edge/k3s/edge-inference.yaml
kubectl -n qualitos-edge rollout status deploy/edge-inference
```

Vérifier le backend sélectionné dans les logs :

```bash
kubectl -n qualitos-edge logs deploy/edge-inference | grep backend.factory
# selected=fallback reason=no-model-path   -> repli déterministe (pas de modèle)
# selected=onnx path=/models/anomaly.onnx  -> inférence ONNX réelle
```

## 4. Fournir un modèle ONNX (active l'inférence réelle)

Le modèle n'est **pas** livré dans le repo. Trois options pour le monter sur
`/models/anomaly.onnx` (chemin = `EDGE_ONNX_MODEL_PATH` du ConfigMap) :

1. **Secret/ConfigMap binaire** (petit modèle) :
   ```bash
   kubectl -n qualitos-edge create secret generic edge-model \
     --from-file=anomaly.onnx=./anomaly.onnx
   ```
   puis remplacer le volume `model` (`emptyDir`) par
   `secret: { secretName: edge-model }` dans le manifeste.
2. **Volume hostPath** provisionné par GitOps (ArgoCD edge) — recommandé pour les
   gros modèles.
3. **OTA** : pousser le modèle via le pipeline GitOps edge, redéployer.

Contrat du modèle (V1) : entrée `[1, n_features]` float32 (n_features =
`TelemetrySample.model_input()`), sortie un score `0..1` (un vecteur est réduit
par `max`). Sans modèle valide → repli déterministe automatique (journalisé).

## 5. Configuration

| Variable (ConfigMap) | Défaut | Rôle |
| --- | --- | --- |
| `EDGE_ONNX_MODEL_PATH` | `/models/anomaly.onnx` | chemin du modèle ; absent/vide → repli |
| `EDGE_ONNX_ANOMALY_THRESHOLD` | `0.5` | seuil d'anomalie du backend ONNX (0..1) |
| `EDGE_BUFFER_PATH` | `/var/lib/edge-inference/queue.jsonl` | file store-and-forward (sur le PVC) |

Les seuils du repli déterministe (`[low, high]` par métrique) sont fournis au
`build_backend(...)` lors du wiring du runtime (§6) ; les calibrer par
Industry Pack (§5) plutôt qu'en dur.

## 6. Wiring du runtime (à intégrer côté déploiement)

Le module livre la **bibliothèque** ; brancher la boucle MQTT → orchestrateur :

```python
from edge_inference.domain.buffer import StoreAndForwardBuffer
from edge_inference.domain.inference import InferenceOrchestrator, ThresholdConfig
from edge_inference.infrastructure.backend_factory import build_backend
from edge_inference.infrastructure.file_store import JsonlFileEventStore

buffer = StoreAndForwardBuffer(
    capacity=100_000, store=JsonlFileEventStore("/var/lib/edge-inference/queue.jsonl")
)
backend = build_backend({"temperature": ThresholdConfig(2, 8)})  # ONNX si modèle présent
orch = InferenceOrchestrator(backend, buffer)

# Boucle MQTT (mosquitto local) : à chaque message -> TelemetrySample -> orch.process(sample)
# Au retour du WAN -> buffer.set_connected(True); buffer.drain(send_to_hub)
```

`send_to_hub` doit retourner `True` sur succès (événement retiré de la file) ou
`False`/lever pour **conserver** l'événement en tête (rejeu ultérieur). En
pratique la remontée passe par le bridge mosquitto sortant ; `send_to_hub` peut
republier sur un topic local repris par le bridge.

## 7. Sécurité (zero-trust, §9.8)

- **Aucun entrant** : pas de Service, le pod sort uniquement.
- **mTLS sortant** assuré par le bridge mosquitto (`mosquitto-config.yaml`,
  `bridge_cafile`/`bridge_certfile`/`bridge_keyfile`, rotation ≤ 90 j).
- Pod durci : non-root, rootfs read-only, capabilities drop ALL, seccomp
  RuntimeDefault, pas de token de service monté.
- **Aucun tenant** dans l'événement : le IoT Hub le résout depuis le device
  registry (§18.2 #2).

## 8. Dépannage

| Symptôme | Cause probable | Action |
| --- | --- | --- |
| `selected=fallback reason=model-missing` | modèle absent au chemin attendu | vérifier le volume `model` et `EDGE_ONNX_MODEL_PATH` |
| `selected=fallback reason=onnx-unavailable` | image sans onnxruntime, ou modèle illisible | rebuild `--build-arg WITH_ONNX=true` ; valider le `.onnx` |
| File qui grossit sans se vider | WAN coupé ou `send_to_hub` qui échoue | vérifier le bridge mosquitto ; la borne protège (DROP_OLDEST) |
| Perte d'événements au reboot | PVC non monté / `EDGE_BUFFER_PATH` hors PVC | vérifier le `volumeMount` `buffer` et le PVC |

## 9. Tests

```bash
cd infra/edge/inference
python -m pytest -p no:cacheprovider -o addopts=""          # 37 tests
python -m pytest --cov=edge_inference --cov-report=term-missing  # gate 90 %
ruff check .
```
