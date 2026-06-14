# ADR 0022 — Détection d'anomalies non-supervisée multivariée (Isolation Forest + reconstruction ACP)

- **Statut** : Accepté
- **Date** : 2026-06-13
- **Owners** : Architecte principal

## Contexte

Le CLAUDE.md promet explicitement, pour DMAIC (§3.4) et dans la matrice IA (§12.1),
la « **détection d'anomalies multivariées (Isolation Forest, Autoencoders, LSTM-AE)** »,
au-delà des règles SPC univariées. L'ADR 0018 a livré le SPC réel (8 règles de Nelson,
I-chart) mais restait **univarié** ; l'audit `docs/AUDIT-stub-vs-reel.md` marquait la
ligne « Anomalies ML (Isolation Forest/Autoencoder) » comme **ABSENT** (aucun import).

Il fallait livrer une capacité **multivariée** (matrice échantillons × features) réelle,
déterministe et explicable, sans introduire de dépendance lourde ni d'entraînement GPU.

## Décision

Implémenter deux détecteurs non-supervisés multivariés dans `ai-service`, en
**NumPy pur**, en architecture hexagonale stricte (contrats import-linter) :

- **Isolation Forest** (`domain/service/isolation_forest.py`) — implémentation fidèle de
  Liu, Ting & Zhou (ICDM 2008) : forêt d'arbres d'isolation (coupe sur attribut aléatoire
  + valeur aléatoire dans [min, max] récursivement), sous-échantillonnage sans remise
  (`sample_size` défaut 256), `n_trees` (défaut 100), hauteur limite
  `ceil(log2(sample_size))`. Longueur de chemin moyenne `E[h(x)]` puis score
  `s(x) = 2^(-E[h(x)]/c(n))` avec `c(n) = 2·H(n−1) − 2(n−1)/n` (longueur de chemin
  moyenne d'un BST). RNG seedé (`np.random.default_rng(seed)`) → **déterministe**.
- **Reconstruction par ACP** (`domain/service/reconstruction.py`) — auto-encodeur
  **linéaire** via SVD : centrage, projection sur les *k* premières composantes
  (auto ~95 % de variance, borné), reconstruction, **erreur L2 par échantillon**.
  Explicabilité : feature de plus grand résidu absolu. Honnête : ce n'est **pas** un
  réseau de neurones.
- **Domaine** (`domain/model/anomaly.py`) : `AnomalyPoint`, `AnomalyResult` (value objects
  immuables, slots).
- **Application** (`application/usecase/anomaly_detect.py`) : `AnomalyDetectUseCase` —
  porte le tenant (auditabilité/multi-tenancy) + logging ; valide la matrice (lignes de
  même longueur, finitude, bornes ≤ 50 000 × 200) ; drapeau d'anomalie par **seuil**
  explicite ou **quantile de contamination** (∈ (0, 0.5]).
- **Présentation** (`presentation/routers/anomaly_router.py`, `schemas/anomaly.py`) :
  `POST /v1/ai/anomaly/detect`, authentifié via `current_user` (JWT/X-Dev-Claims), tenant
  issu du `UserContext` jamais du body (règle 18.2 #2).
- **Engine** (`quality/anomaly/{AnomalyController,AnomalyService,AnomalyDto}`,
  `AiGatewayClient.detectAnomaly`) : relais `POST /api/v1/ai/anomaly/detect` qui réutilise
  le garde-fou LLM04 (op « anomaly », ADR 0017), mapping tolérant comme le SPC.

## Justification

- **NumPy pur, aucune nouvelle dépendance** : scikit-learn et PyTorch sont écartés. Le
  reste de la couche domaine (`forecasting`, `spc_rules`, `nc_clustering`) le prouve déjà ;
  l'import-linter **interdit** à `domain/` d'importer fastapi/pydantic/httpx, et numpy
  reste la seule brique de calcul autorisée. On garde la couche domaine légère, auditable,
  testable sans framework et déployable on-prem sans GPU.
- **Déterministe & explicable** (§12.3) : score par point + indicateur `is_anomaly` +
  feature contributrice (reconstruction). À graine fixe, résultats reproductibles.
- **Multivarié réel** : l'entrée est une matrice (≠ SPC univarié), comblant la promesse.

## Alternatives écartées

- **scikit-learn `IsolationForest` / autoencoder PyTorch** : nouvelle dépendance lourde,
  contraire au pattern domaine NumPy pur et aux contrats import-linter ; surcoût d'image
  et de surface d'attaque. L'algorithme d'isolation et l'ACP sont simples à coder
  fidèlement.
- **LSTM-AE** : nécessite séries temporelles + entraînement ; gardé pour un lot ultérieur.
- **Calcul côté engine (Java)** : l'analytique/ML vit dans `ai-service` (§10.2, §12.2) ;
  l'engine relaie et réutilise garde-fous/observabilité, comme pour SPC et NLQ.

## Conséquences

- ✅ Anomalies multivariées réelles ; ligne d'audit « Isolation Forest/Autoencoder »
  passe ABSENT → RÉEL.
- ✅ Tests ai-service : isolation forest (point aberrant injecté détecté, déterminisme,
  bornes, dégénérés), reconstruction (off-manifold détecté, normalisation, dégénérés),
  use case et routeur (auth, méthodes, seuil, validations 422). Couverture des nouveaux
  modules ≈ 98 %, suite complète ≥ 85 %. import-linter « 2 kept, 0 broken ».
- ✅ Engine : `AnomalyService`/`AnomalyController`/`AiGatewayClient.detectAnomaly` +
  tests (mapping tolérant, 200/400/502, garde-fou). `verify` JaCoCo vert.
- ⚠ Pas d'UI (livrée séparément). Pas de dérivation auto depuis la télémétrie IoT /
  `kpi_measurements` ni d'ouverture CAPA automatique sur anomalie (réutilisera le schéma
  SPC→CAPA d'ADR 0016 dans un lot ultérieur).
- ⚠ Isolation Forest est O(n_trees · n · hauteur) ; bornes de taille (50 000 × 200) +
  sous-échantillonnage maîtrisent le coût.

## Tests d'invariant

- `tests/domain/test_isolation_forest.py`, `tests/domain/test_reconstruction.py` :
  récupération d'anomalies connues + déterminisme + cas dégénérés.
- `tests/application/test_anomaly_detect_usecase.py` : validation + branches de drapeau.
- `tests/presentation/test_anomaly_router.py` : auth, méthodes, seuil, 422.
- `lint-imports` : les contrats hexagonaux restent verts (numpy seul autorisé en domaine).
- Engine : `AnomalyServiceTest`, `AnomalyControllerTest`, cas `detectAnomaly` dans
  `AiGatewayClientTest`.

## Références

CLAUDE.md §3.4, §12.1, §12.3 ; Liu, Ting & Zhou, *Isolation Forest*, ICDM 2008 ;
[ADR 0008](./0008-ai-service-architecture.md) (archi ai-service) ;
[ADR 0017](./0017-ai-guardrails-llm-dos.md) (garde-fous LLM04) ;
[ADR 0018](./0018-spc-anomaly-detection.md) (SPC univarié, prédécesseur).
