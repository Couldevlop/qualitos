# Module Prévision KPI

[← Retour à l'index](../README.md) · Route : **`/forecast`** · Menu : *Méthodes qualité › Prévision KPI*

## À quoi sert ce module

La **prévision KPI** projette la trajectoire future d'un indicateur à partir de son historique,
grâce au lissage exponentiel **Holt-Winters** (qui capte tendance et saisonnalité). L'objectif :
anticiper si une cible sera tenue, plutôt que de constater l'écart après coup.

## Parcours pas à pas

1. **Ouvrir** `/forecast`.
2. **Sélectionner / fournir** l'historique du KPI à projeter.
3. **Lancer la prévision** : le module renvoie les valeurs prévues sur l'horizon demandé.
4. **Décider** : si la tendance s'éloigne de la cible, anticipez une action (CAPA, cycle PDCA…).

## Liens avec les autres modules

- Les KPIs proviennent du catalogue d'indicateurs (`/kpis`).
- Une prévision défavorable peut déclencher un cycle [PDCA](pdca.md) ou un projet [DMAIC](dmaic.md).

## Bonnes pratiques

- **Une prévision n'est pas une certitude** : c'est une projection à partir du passé ; un
  changement de contexte la rend caduque.
- **Plus l'historique est riche, meilleure est la prévision** : surtout pour capter la saisonnalité.
- **Agissez tôt** : l'intérêt de la prévision est d'anticiper, pas de confirmer.
