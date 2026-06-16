# ADR 0024 — Explicabilité par Kernel SHAP (NumPy pur)

- **Statut** : Accepté
- **Date** : 2026-06-16
- **Owners** : Architecte principal

## Contexte

L'invariant §12.3 / §22.13 exige que « toute prédiction/recommandation montre ses
sources et sa confiance ». La plupart des modèles QualitOS sont déjà explicables par
construction (scoring fournisseur = contributions additives ; forecast = pente/niveau/
R²/IP ; SPC = règle de Nelson + points ; NC = top-termes). **L'exception** était la
**détection d'anomalies par Isolation Forest** : un score d'anomalie sans attribution
par feature (la reconstruction ACP exposait déjà `top_feature`, mais pas l'Isolation
Forest). L'audit `docs/AUDIT-stub-vs-reel.md` marquait « Explicabilité SHAP/LIME » ABSENT.

## Décision

Implémenter **Kernel SHAP** (Lundberg & Lee, NeurIPS 2017), model-agnostic, en
**NumPy pur**, et le brancher sur l'Isolation Forest :

- `domain/service/shap_kernel.py` : `shapley_values(x, background, predict)` estime les
  valeurs de Shapley par **régression linéaire pondérée** (noyau SHAP) sur des coalitions
  de features, l'« absence » d'une feature étant imputée par le background (espérance
  marginale). Espérance **exacte** (moyenne sur tout le background) tant que le budget de
  lignes synthétiques le permet, sinon estimée par échantillonnage. Coalitions exhaustives
  si d ≤ 12, échantillonnées (poids du noyau) au-delà. Contrainte d'efficacité imposée par
  substitution : **Σ φ_i = f(x) − E[f(background)]**. Déterministe (graine fixe).
- `domain/service/isolation_forest.py` : refactor `build_forest()` + `IsolationForest.score()`
  (séparation fit/score) pour scorer des points ARBITRAIRES contre une forêt FIXE — requis
  par SHAP qui évalue des points synthétiques. `score_samples()` conserve l'API historique.
- `application/usecase/anomaly_detect.py` : `AnomalyExplainUseCase` — entraîne la forêt sur
  la matrice, prend l'échantillon `index`, le background (la matrice, bornée à 256), retourne
  les `FeatureContribution` triées. v1 = Isolation Forest (la reconstruction expose déjà la
  feature dominante).
- Présentation : `POST /v1/ai/anomaly/explain` (tenant via JWT, jamais du body).
- Engine : `AiGatewayClient.explainAnomaly` (garde-fou LLM04 op « anomaly-explain ») +
  `AnomalyController#/explain` + `AnomalyService#explain` (mapping tolérant).
- UI : bouton « Pourquoi ? » par anomalie → graphe des contributions (rouge = pousse vers
  l'anormalité, bleu = vers la normale).

## Justification

- **NumPy pur, aucune dépendance lourde** : la lib `shap` (C++/TreeSHAP) est écartée, comme
  sklearn/torch ailleurs (contrats import-linter, image distroless, surface CVE). Kernel SHAP
  est l'estimateur model-agnostic standard, codable fidèlement.
- **Théoriquement fondé** : les valeurs de Shapley sont l'unique attribution vérifiant
  efficacité, symétrie, nullité, additivité ; la propriété d'efficacité est **garantie par
  construction** (et testée).
- **Model-agnostic** : le même service explique n'importe quelle fonction de score
  (réutilisable pour la reconstruction, le forecast, etc. ultérieurement).

## Alternatives écartées

- **Lib `shap`** : dépendance lourde, contraire au pattern domaine.
- **LIME** : explication locale par perturbation linéaire — moins fondée que Shapley, pas de
  propriété d'efficacité. Kernel SHAP couvre le même besoin avec de meilleures garanties.
- **TreeSHAP exact** : spécifique aux arbres, plus rapide, mais complexe ; Kernel SHAP suffit
  vu nos tailles bornées (anti-DoS) et reste réutilisable hors arbres.

## Conséquences

- ✅ L'Isolation Forest devient explicable par feature ; ligne d'audit ABSENT → RÉEL.
- ✅ Tests : `test_shap_kernel.py` (efficacité, exactitude sur modèle additif, feature nulle,
  déterminisme, échantillonnage grande dimension, validations) ; explain via router
  (efficacité Σφ = score − base) ; engine `AnomalyServiceTest`/`AnomalyControllerTest`/
  `AiGatewayClientTest` (cas explain) ; UI service spec. pytest 222 (cov 90,9%), engine vert.
- ⚠ Kernel SHAP est en O(coalitions × background) ; bornes de taille + budget de lignes
  synthétiques + sous-échantillonnage maîtrisent le coût. Pour de très nombreuses features,
  l'espérance est échantillonnée (variance maîtrisée par le grand nombre de coalitions).
- ⚠ v1 branchée sur l'Isolation Forest uniquement (autres modèles : lot ultérieur, même
  service).

## Tests d'invariant

- `tests/domain/test_shap_kernel.py` : Σφ = f(x) − E[f(bg)] ; additif exact ; déterminisme.
- `tests/presentation/test_anomaly_router.py` : `/explain` (auth, efficacité, index hors borne).
- Engine : cas `explain` dans `AnomalyServiceTest`, `AnomalyControllerTest`, `AiGatewayClientTest`.
- `lint-imports` : contrats hexagonaux verts (numpy seul autorisé en domaine).

## Références

CLAUDE.md §12.3, §22.13 ; Lundberg & Lee, *A Unified Approach to Interpreting Model
Predictions*, NeurIPS 2017 ; [ADR 0022](./0022-unsupervised-anomaly-detection.md) (anomalies).
