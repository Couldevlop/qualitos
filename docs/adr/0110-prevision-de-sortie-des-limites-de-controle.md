# ADR 0110 — Prévision de sortie des limites de contrôle, et la CAPA qu'elle ouvre

- **Statut** : Accepté
- **Date** : 2026-11-09
- **Owners** : @Couldevlop
- **Portée** : ai-service (`domain.model.predict.SeriesProjection`,
  `domain.service.forecasting.project`, `domain.model.spc.SpcBreachForecast`,
  `domain.service.spc_forecast`, `application.usecase.SpcForecastUseCase`,
  `POST /v1/ai/spc/forecast`), api-quality-engine (`spc` : `SpcService.forecast`,
  `SpcService.forecastKpi`, `SpcDto`, `SpcController`, `AiGatewayClient.forecastSpc`),
  écran `/spc`
- **Complète** : ADR 0018 (8 règles de Nelson) et ADR 0023 (prévision de KPI par
  Holt-Winters) — aucune des deux n'est révisée.

## Contexte

La carte de contrôle répond à une question : **le procédé est-il hors contrôle ?**
Les 8 règles de Nelson (ADR 0018) la tranchent sur les mesures déjà prises. C'est
une réponse sur le passé : quand elle arrive, les pièces sont faites, le lot est
coulé, le patient est sorti. Le §6.5 de la spécification promet l'autre question —
**quand le procédé va-t-il en sortir ?** — et c'est la seule des deux sur laquelle
on peut encore agir.

La plateforme savait déjà projeter une série (ADR 0023, Holt-Winters en numpy pur)
et savait déjà poser des limites de contrôle (ADR 0018, σ = MR̄/d₂). Ce qui
manquait était la jonction : projeter la série **dans le repère de ses propres
limites**, et dater le franchissement.

Trois pièges guettaient cette jonction.

1. **Deux ajustements pour une courbe.** Si la prévision de dérive refaisait son
   propre lissage, l'écran afficherait deux projections d'une même série qui ne
   coïncident pas — celle du KPI et celle de la carte. L'utilisateur n'aurait
   aucun moyen de savoir laquelle croire.
2. **Deux jeux de limites.** Si la prévision estimait ses limites autrement que
   la carte, elle annoncerait le franchissement d'un trait qui n'est pas celui
   dessiné. L'annonce serait invérifiable.
3. **Un dossier mal nommé.** Un écart annoncé n'est pas un écart survenu. Ouvrir
   une CAPA **corrective** sur une prévision ferait traiter comme un fait ce qui
   n'est qu'une projection, et occuperait la place du dossier correctif à ouvrir
   si la dérive se produit vraiment.

## Décision

**Un seul ajustement, exposé.** `forecasting.project()` est extrait de
`forecast()` et rend un `SeriesProjection` (niveau, tendance, σ résiduel, R²,
confiance, points avec intervalle). La prévision de KPI et la prévision de sortie
de limites appellent la **même** fonction : une seule courbe existe pour une
série donnée. `forecast()` reste inchangé pour ses appelants.

**Un seul jeu de limites.** `spc_forecast.forecast_breach()` ne calcule pas ses
limites : ou bien l'appelant fournit la baseline (`center`, `sigma`) et les
limites sont construites à ±3σ autour d'elle (`estimated = False`), ou bien elles
sont estimées par `spc_rules.estimate_limits` — la fonction même qu'emploie la
carte. L'annonce porte donc sur le trait qui est dessiné.

**Le risque, pas une date sèche.** Chaque période à venir porte
`p_above_ucl` et `p_below_lcl`, calculées depuis l'intervalle de la projection
(σ_h relu comme `(high − low) / 2z`, donc croissant avec l'horizon). La première
période dont le risque atteint le `threshold` (0,5 par défaut) est
`first_breach_step`, avec son `side`. Quand `ucl == lcl` — série constante,
σ nul — il n'y a pas de limite à franchir et rien n'est annoncé.

**Quatre mesures au minimum.** En dessous, une tendance n'est qu'un trait entre
deux points : le serveur refuse (422) et l'écran ne propose pas le bouton.

**La CAPA ouverte est PRÉVENTIVE**, avec `sourceType = SPC_ALERT` et
`sourceRef = "kpi:<id>:forecast"` — référence distincte de `"kpi:<id>"` du
chemin correctif, pour que l'annonce n'empêche jamais le dossier correctif quand
la dérive, elle, survient. Elle n'est ouverte que si `openCapa` est demandé, que
la sortie est annoncée, et qu'aucun dossier actif ne porte déjà la même
référence. **Sa criticité suit la confiance, pas le risque** : mobiliser en
« haute » sur une série de cinq points prêterait au chiffre une précision que le
modèle ne tient pas.

**L'écran montre son raisonnement** (§12.3) : la projection est tracée en
pointillé *dans* la carte, prolongeant l'axe, et part du dernier point mesuré ;
la phrase d'annonce est accompagnée de la confiance, de la tendance signée par
période, du R², du σ résiduel, du modèle et du nombre de mesures employées. Sans
cela, l'annonce serait un oracle.

## Conséquences

- Un procédé qui dérive est signalé **avant** d'avoir produit un écart, et le
  signalement est rattachable à un dossier préventif.
- La carte et la prévision ne peuvent plus divergir : même série, même
  ajustement, mêmes limites. Changer l'estimation des limites les change toutes
  les deux.
- Le coût est un appel d'inférence de plus, soumis aux mêmes garde-fous IA que
  les autres (ADR 0017, opération `spc-forecast`).
- Rien n'est automatique : aucune action n'est prise sur la base d'une prévision
  sans qu'un humain l'ait demandée (`openCapa`) et la valide ensuite.
