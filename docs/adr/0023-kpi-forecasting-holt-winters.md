# ADR 0023 — Prévision KPI par lissage exponentiel Holt-Winters (NumPy pur)

- **Statut** : Accepté
- **Date** : 2026-06-14
- **Owners** : Architecte principal

## Contexte

Le CLAUDE.md promet une **prédiction KPI** explicable (§6.5 « Atteindras-tu ton KPI
cible ? », §12.1) avec, comme cibles techniques, LSTM / Prophet / Temporal Fusion
Transformer. L'audit `docs/AUDIT-stub-vs-reel.md` marquait « Prédiction LSTM/Prophet/TFT »
comme **ABSENT**. Une première brique existait — `forecasting.py` — mais en **tendance OLS
linéaire** seulement : pas de niveau/tendance adaptatifs, pas de saisonnalité. De plus le
endpoint n'était **pas câblé** au produit (ni relais engine, ni UI).

Il fallait un vrai modèle de séries temporelles (niveau, tendance, saisonnalité),
déterministe et explicable, **sans dépendance lourde** (ni PyTorch pour LSTM, ni
cmdstanpy/Stan pour Prophet — incompatibles avec la couche domaine NumPy pur, l'image
distroless et la surface CVE), et le rendre **utilisable de bout en bout**.

## Décision

Remplacer la tendance OLS par un **lissage exponentiel de Holt-Winters** en **NumPy pur**,
dans `domain/service/forecasting.py`, derrière le contrat existant `KpiForecast` :

- **Holt linéaire** (double ES) : niveau `l_t` + tendance `b_t` adaptatifs.
- **Holt-Winters additif** : ajoute une composante saisonnière `s_t` quand un
  `seasonal_period` est fourni et que la série couvre ≥ 2 périodes.
- **Paramètres `alpha, beta(, gamma)`** choisis par **grid-search** minimisant la somme des
  carrés des erreurs one-step → ajustement réel et reproductible (déterministe).
- σ résiduel sur l'ajustement en échantillon ; intervalle de prédiction à 95 % élargi en
  √h avec l'horizon ; `P(cible atteinte)` via la CDF normale à l'horizon (selon la
  direction). Champs d'explicabilité ajoutés : `model` (`holt_linear` |
  `holt_winters_additive`) et `seasonal_period`.
- **Câblage produit** : relais engine `quality/forecast/{ForecastController,ForecastService,
  ForecastDto}` (`POST /api/v1/ai/forecast/kpi`) + `AiGatewayClient.forecastKpi`
  (réutilise le garde-fou LLM04, op « forecast », ADR 0017) ; UI Angular `/forecast`
  (livrée dans le même lot).

## Justification

- **NumPy pur, aucune nouvelle dépendance** : même ligne directrice que `spc_rules`,
  `nc_clustering`, `isolation_forest` (ADR 0022) et l'import-linter qui interdit à
  `domain/` d'importer un framework. Holt-Winters est un modèle de référence, simple à
  coder fidèlement, déployable on-prem sans GPU.
- **Rétro-compatibilité** : une série parfaitement linéaire est reproduite **exactement**
  par Holt (erreur one-step nulle dès l'initialisation l₀=y₀, b₀=y₁−y₀), donc la baseline
  linéaire reste un cas particulier propre — les tests OLS historiques (pente, R², valeur
  projetée) restent verts.
- **Explicable** (§12.3) : modèle retenu, période saisonnière, pente/niveau finaux,
  σ résiduel, R², intervalles de prédiction, probabilité d'atteinte.

## Alternatives écartées

- **LSTM (PyTorch) / Prophet (Stan) / TFT** : dépendances lourdes, entraînement, image et
  surface d'attaque ; incompatibles avec le pattern domaine NumPy pur. Pourront se brancher
  derrière le **même contrat `KpiForecast`** quand un budget GPU existera (même logique que
  HDBSCAN derrière `nc_clustering`).
- **Garder l'OLS** : ne capte ni saisonnalité ni niveau/tendance adaptatifs ; insuffisant
  pour la promesse §6.5.
- **Auto-détection de la saisonnalité** : repoussée (déterminisme/simplicité) ; la période
  est fournie explicitement en v1.

## Conséquences

- ✅ Prévision KPI réelle (niveau+tendance+saisonnalité) ; ligne d'audit ABSENT → RÉEL.
- ✅ Utilisable de bout en bout : ai-service → engine → UI `/forecast`.
- ✅ Tests ai-service (`test_forecasting.py`) : compat linéaire exacte, champ `model`,
  saisonnalité (engagement Holt-Winters, motif restitué, fallback si série trop courte) ;
  suite complète ≥ 85 %, ruff vert. Engine : `ForecastServiceTest`, `ForecastControllerTest`,
  cas `forecastKpi` dans `AiGatewayClientTest` ; `verify` JaCoCo vert.
- ⚠ Pas de dérivation automatique depuis `kpi_measurements` (l'utilisateur fournit la
  série / la cible) — un mode « depuis un KPI » comme le SPC pourra suivre.
- ⚠ Saisonnalité fournie explicitement (pas d'auto-détection en v1).

## Tests d'invariant

- `tests/domain/test_forecasting.py` : compat OLS (cas linéaire exact), `model`/
  `seasonal_period`, Holt-Winters additif (motif restitué, fallback série courte).
- `tests/presentation/test_predict_router.py` : endpoint `/v1/ai/predict/kpi` (auth, 422).
- Engine : `ForecastServiceTest` (mapping tolérant), `ForecastControllerTest` (200/400/502),
  `AiGatewayClientTest` (cas `forecastKpi`).
- `lint-imports` : contrats hexagonaux verts (numpy seul autorisé en domaine).

## Références

CLAUDE.md §6.5, §12.1, §12.3 ; Holt (1957) / Winters (1960), lissage exponentiel ;
[ADR 0017](./0017-ai-guardrails-llm-dos.md) (garde-fous LLM04) ;
[ADR 0018](./0018-spc-anomaly-detection.md) (SPC) ;
[ADR 0022](./0022-unsupervised-anomaly-detection.md) (anomalies non-supervisées).
