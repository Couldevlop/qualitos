# ADR 0030 — Inférence Edge : store-and-forward + ONNX-ou-repli déterministe

- **Statut** : Accepté
- **Date** : 2026-06-19
- **Owners** : @Couldevlop
- **Phase** : P3 (Module IoT/Edge Connectivity, CLAUDE.md §9.5)

## Contexte

CLAUDE.md §9.5 décrit l'Edge Gateway K3s avec, entre autres, « **Inférence IA
locale** : modèles ONNX/TFLite embarqués pour détection d'anomalies sans
aller-retour cloud », un « **buffer local 24-72 h** (store-and-forward) » et un
« **mode déconnecté** : fonctionne 100 % offline, sync différée intelligente ».

`docs/AUDIT-stub-vs-reel.md` notait « Edge Gateway K3s = ABSENT : aucun code
edge ». Le broker mosquitto local (`infra/edge/k3s`) faisait déjà le
store-and-forward du **transport MQTT**, mais aucun composant n'exécutait
d'**inférence applicative** au plus près du capteur, ni ne mettait en file des
**événements d'anomalie** de niveau métier.

Deux tensions :

1. **Dépendances lourdes vs testabilité** — `onnxruntime` est volumineux,
   multi-arch, et indisponible sur bien des postes/CI. Imposer cette dépendance
   pour faire tourner le composant (ou ses tests) est inacceptable pour une
   appliance edge à 512 Mo de RAM et pour la CI.
2. **Résilience réseau** — l'edge fonctionne en WAN intermittent ; une anomalie
   détectée hors-ligne ne doit jamais être perdue, et doit être rejouée **dans
   l'ordre** au retour du lien.

## Décision

Composant Python **autonome et isolé** sous `infra/edge/inference/` (projet à
part, `pyproject.toml` propre, packages top-level `edge_inference`), en Clean
Architecture hexagonale légère. Périmètre strictement edge : ne touche ni
`ai-service`, ni `ai-vision-5s`, ni `apps/web`, ni `api-iot-hub`.

### Pattern « ONNX réel, sinon repli déterministe » (calqué sur Vision 5S)

Comme `apps/ai-vision-5s/app/infrastructure/backend_factory.py` :

- `EDGE_ONNX_MODEL_PATH` pointe un modèle ONNX chargeable (`onnxruntime`
  importable) → `OnnxInferenceBackend` (**réel**) ;
- sinon → `ThresholdFallbackBackend` : repli **déterministe à seuils**
  `[low, high]` par métrique, score gradué `0..1`, sévérité WARNING/CRITICAL,
  pur stdlib, toujours disponible.

L'import d'`onnxruntime` est **paresseux** (dans l'adaptateur), donc la couche
domaine reste framework-free et le repli ne charge jamais la lib. Toute
défaillance de construction du backend ONNX **dégrade gracieusement** vers le
repli (journalisé). La sélection est tracée au démarrage
(`backend.factory selected=onnx|fallback reason=...`).

### Buffer store-and-forward (cœur pur)

`StoreAndForwardBuffer` (domaine, stdlib seul) :

- **FIFO** ordonné, **borné** (capacité protégeant l'appliance), politique
  d'éviction `DROP_OLDEST` (défaut, on garde la télémétrie la plus fraîche) ou
  `REJECT_NEW` ;
- **mode déconnecté** : `set_connected(false)` accumule, `drain()` est no-op tant
  que hors-ligne ; au retour du lien, rejeu **dans l'ordre** ; un `sender` qui
  échoue (retour `False` ou exception) **conserve** l'événement en tête (aucune
  perte) ;
- **persistance** via le port `EventStore` (hexagonal) : `InMemoryEventStore`
  par défaut (zéro I/O, 100 % testable), `JsonlFileEventStore` (infra) pour
  survivre aux redémarrages (append-only + `os.replace` atomique, tolérance à
  une dernière ligne tronquée par coupure d'alimentation).

### Contrat d'événement, PAS d'appel réseau

À la détection d'une anomalie, l'orchestrateur met en file un `HubEvent`
(`event_type`, `device_id`, `metric`, `score`, `severity`, `backend`, `reason`,
`timestamp_ms`). **Aucun appel réseau dans ce module** : la remontée mTLS
sortante reste assurée par le bridge mosquitto (§9.8). Conformément à §18.2 #2,
**aucun tenant** n'est porté par l'événement : le IoT Hub résout le tenant depuis
le device registry.

### Conteneurisation & K3s

- `Dockerfile` distroless/nonroot, multi-arch documenté (buildx amd64+arm64),
  `--build-arg WITH_ONNX=true` pour embarquer le runtime ONNX (sinon image
  minimale, repli déterministe).
- `infra/edge/k3s/edge-inference.yaml` : Deployment + ConfigMap (chemin modèle,
  seuil) + PVC (file persistée), pod durci (non-root, rootfs read-only,
  capabilities drop ALL, `automountServiceAccountToken: false`), **aucun Service
  entrant** (§9.8). Cohérent avec les manifestes mosquitto existants (namespace
  `qualitos-edge`).

## Justification

- **Calquer Vision 5S** garantit la cohérence du pattern « stub/repli déterministe
  derrière le même Protocol que le réel » déjà validé dans le repo.
- **Domaine pur sans dépendance** : la résilience (le plus critique) est testée
  à 100 % sans `onnxruntime`, et tourne sur une appliance nue.
- **Repli toujours disponible** : la ligne reste protégée même sans modèle
  provisionné — pas de point de défaillance « pas de modèle = pas de détection ».
- **Projet isolé** : aucun pipeline existant ne le compile (CI Maven + tests
  Python ciblés sur `apps/ai-service` uniquement, scan d'image sur une matrice
  fixe) → zéro risque de casse CI.

## Conséquences

- ✅ Brique §9.5 « inférence locale + store-and-forward + mode déconnecté »
  réelle et testée (37 tests, 96 % couverture, domaine ~100 %).
- ✅ Anomalie hors-ligne jamais perdue, rejouée en ordre ; survit au redémarrage.
- ⚠ Le **modèle ONNX entraîné** reste à fournir (hors repo) ; le chemin ONNX est
  testé par mock d'`onnxruntime`.
- ⚠ Le **runtime long-running** (souscription MQTT locale → orchestrateur →
  drain vers le bridge) est **délégué au déploiement** (cf. runbook) : ce module
  livre la bibliothèque (logique + contrat), pas un démon figé.
- ⚠ TFLite n'est pas implémenté (ONNX retenu, aligné §10.2 ONNX Runtime) ;
  ajoutable derrière le même `InferenceBackend` Protocol sans rupture.

## Tests d'invariant

- `test_buffer.py` (13 cas) — FIFO, borne DROP_OLDEST/REJECT_NEW, mode
  déconnecté→sync, arrêt/conservation en tête sur échec sender, exception sender
  sans perte, `max_events`, restauration depuis persistance, troncature backlog
  surdimensionné, roundtrip dict.
- `test_inference.py` (9 cas) — repli déterministe (within-band, above/below,
  score gradué, sévérité, métrique inconnue ±défaut), orchestrateur (anomalies
  seules mises en file, batch, event_type custom), entrée multivariée.
- `test_file_store.py` (8 cas) — append/load, survie au redémarrage, pop_left
  durable, replace atomique, drain vide le store, ligne corrompue ignorée,
  pop_left sur vide no-op, lignes blanches ignorées.
- `test_backend_factory.py` (7 cas) — pas de modèle → repli, fichier absent →
  repli, échec construction ONNX → repli, **ONNX sélectionné** (onnxruntime
  mocké), parsing seuil env, `OnnxBackendUnavailable` sans onnxruntime.
- Validation : `python -m pytest -p no:cacheprovider -o addopts=""` vert,
  `ruff check .` vert.

## Références

- CLAUDE.md §9.3-9.5 (Edge Gateway, inférence locale, store-and-forward, mode
  déconnecté), §9.8 (zero-trust, aucun entrant), §10.2 (ONNX Runtime), §18.2 #2
  (tenant jamais depuis la charge).
- Patron calqué : `apps/ai-vision-5s` (backend_factory ONNX-ou-stub, import
  paresseux, dégradation gracieuse).
- ADR 0016 (seuil IoT → CAPA, consommateur aval des événements d'anomalie),
  ADR 0028 (connecteur Modbus — décodage délégué à la passerelle Edge).
- `infra/edge/k3s` (mosquitto local + bridge mTLS sortant), runbook
  `docs/runbooks/edge-inference.md`.
