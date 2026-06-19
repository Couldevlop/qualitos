# ADR 0031 — Backends ML lourds enfichables, opt-in, à import paresseux

- **Statut** : Accepté
- **Date** : 2026-06-19
- **Owners** : Architecte principal

## Contexte

Le CLAUDE.md promet des modèles **lourds** à plusieurs endroits : prédiction KPI
LSTM/Prophet/TFT (§6.5, §12.1), clustering NC **HDBSCAN** sur embeddings (§4.3,
§12.1), sentiment **BERT** (§4.9, §12.1) et **transcription Whisper** des Cercles de
Qualité / notes vocales terrain (§3.3, §15.3). L'audit `docs/AUDIT-stub-vs-reel.md`
les marquait ABSENT (LSTM/Prophet, HDBSCAN, BERT/Whisper).

Or les chemins **par défaut** livrés (ADR 0023 Holt-Winters, ADR 0018/0022 anomalies,
ADR 0025 NLP lexical, DBSCAN dans `predict.py`) sont réels, déterministes, **NumPy
pur**, déployables on-prem sans GPU, et respectent l'import-linter (domaine sans
framework), l'image distroless et la surface CVE minimale. Il ne faut **pas** les
casser, ni alourdir le runtime/CI par défaut avec PyTorch, Stan ou Cython.

## Décision

Introduire un paquet `domain/service/ml_backends/` de **backends lourds enfichables**,
sélectionnés par un paramètre explicite, derrière les **contrats existants**
(`KpiForecast`, `NcClusteringResult`, `ComplaintAnalysis`, + nouveau `Transcription`) :

- **Forecast** : `forecasting.forecast(..., model=...)` — `holt_winters` (défaut,
  réel) | `prophet` (`forecast_prophet`) | `lstm` (`forecast_lstm`).
- **Clustering NC** : `nc_clustering.cluster(..., method=...)` — `dbscan` (défaut) |
  `hdbscan` (`cluster_hdbscan`, réutilise la même vectorisation TF-IDF).
- **NLP sentiment** : `complaint_nlp.analyze(..., backend=...)` — `lexical` (défaut) |
  `bert` (`sentiment_bert` ; la classification/criticité restent les heuristiques du
  défaut, seul le sentiment passe au Transformer).
- **Transcription** : nouvel endpoint `POST /v1/ai/transcribe` (Whisper,
  `transcribe_whisper`) — **pas** de chemin léger par défaut.

Invariants du pattern :

1. **Vrai code** : chaque backend appelle réellement sa lib (Prophet/torch/hdbscan/
   transformers/whisper). Jamais de faux résultat.
2. **Import paresseux** : la lib lourde est importée **dans la méthode** (jamais en
   tête de module). Importer `ml_backends` ne tire aucune lib lourde ; le domaine
   reste framework-free pour l'import-linter.
3. **Opt-in & isolé** : les dépendances vivent dans
   `[project.optional-dependencies] ml` du `pyproject.toml` (ajout de `prophet`,
   `hdbscan`, `openai-whisper`, `pandas` aux côtés de torch/transformers existants).
   Absentes du runtime par défaut **et de la CI**.
4. **Indisponibilité = erreur claire** : si le backend est sélectionné mais sa lib
   absente, on lève `MlBackendUnavailableError(backend, package)` (message « installer
   l'extra ml »). La présentation la mappe en **422** (forecast/clustering/NLP, où un
   défaut léger existe) et en **501** pour `/v1/ai/transcribe` (aucun défaut léger
   possible pour l'audio). **Jamais** une inférence factice.
5. **Validation en amont** : les bornes d'entrée (série trop courte, direction,
   min_samples, audio vide, taille fichier) sont vérifiées **avant** l'import lourd.

## Justification

- **Le défaut qui marche reste intact** : aucune signature cassée ; tous les tests
  historiques restent verts. Le `model`/`method`/`backend` par défaut reproduit le
  comportement antérieur (champs `model`/`method` inchangés).
- **CI verte sans GPU ni lib lourde** : les tests « backend indisponible » vérifient
  le **message d'erreur** (pas l'inférence), exactement parce que les libs ne sont
  pas installées. Skip automatique si une lib venait à être présente.
- **Honnêteté du scaffold** : le corps lourd (post-import) est du vrai code mais non
  exécuté en CI → non couvert ; c'est assumé et documenté (cf. audit). Le gate de
  couverture global (`fail_under=85`) reste tenu par les chemins par défaut + erreurs.
- **Sécurité/surface** : pas d'élargissement de la surface d'attaque ou de l'image par
  défaut ; les tenants régulés activent l'extra `ml` en connaissance de cause.

## Alternatives écartées

- **Mettre les libs lourdes dans les deps par défaut** : alourdit image/CI/CVE pour
  tous, contraire au pattern domaine NumPy pur — rejeté.
- **Imports en tête de module gardés par try/except** : tirerait quand même la lib si
  installée et brouillerait l'import-linter ; l'import dans la méthode est plus net.
- **Faux résultat de repli si lib absente** : interdit (l'IA doit être honnête et
  explicable, §12.3) — on lève une erreur claire à la place.

## Conséquences

- ✅ LSTM/Prophet, HDBSCAN, BERT, Whisper passent de ABSENT à « OPT-IN câblé, défaut
  réel » dans l'audit. Endpoints : `model`/`method`/`backend` sur les schémas
  existants + `POST /v1/ai/transcribe`.
- ✅ `domain.service.ml_backends.MlBackendUnavailableError` centralise le contrat
  d'indisponibilité ; 422 (forecast/cluster/NLP) / 501 (transcribe).
- ✅ `python-multipart` ajouté aux deps de base (requis par `UploadFile`).
- ⚠ Corps des backends lourds non couverts en CI (libs absentes) — scaffold assumé.
- ⚠ Pas de relais engine/UI dans ce lot (ai-service uniquement) — à suivre.

## Tests d'invariant

- `tests/domain/test_ml_backends.py` : défaut inchangé (3 capacités), rejet de nom de
  backend inconnu, indisponibilité → `MlBackendUnavailableError` (message « extra
  ml », `backend`/`package`), validation d'entrée avant import lourd.
- `tests/presentation/test_predict_router.py` : `model`/`method` par défaut, 422 sur
  backend inconnu (pattern schéma) et sur prophet/lstm/hdbscan indisponibles.
- `tests/presentation/test_complaint_router.py` : défaut lexical, 422 BERT indisponible.
- `tests/presentation/test_transcription_router.py` : auth, audio vide (422), trop
  gros (413), Whisper indisponible (501 « extra ml »).
- `lint-imports` : contrats hexagonaux verts (les libs ml ne sont pas importées par le
  domaine au niveau module).

## Références

CLAUDE.md §3.3, §4.3, §4.9, §6.5, §12.1, §12.3, §15.3 ;
[ADR 0023](./0023-kpi-forecasting-holt-winters.md) (Holt-Winters, défaut forecast) ;
[ADR 0022](./0022-unsupervised-anomaly-detection.md) ;
[ADR 0025](./0025-complaint-nlp-lexical.md) (NLP lexical, défaut sentiment) ;
[ADR 0017](./0017-ai-guardrails-llm-dos.md) (garde-fous LLM04).
