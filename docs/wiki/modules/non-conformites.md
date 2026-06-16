# Module Non-conformités (NC)

[← Retour à l'index](../README.md) · Route : **`/nc`** · Menu : *Qualité opérationnelle › Non-conformités*

## À quoi sert ce module

Une **non-conformité (NC)** est un écart entre une exigence (norme, procédure, spécification) et
la réalité observée. Le module permet de **déclarer**, **qualifier** et **suivre** les NC, avec
possibilité d'y joindre des **photos**.

## Parcours pas à pas

1. **Ouvrir** `/nc` : la liste des non-conformités et leur statut.
2. **Déclarer une NC** : décrire l'écart (quoi, où, quand), sa gravité et le périmètre concerné.
3. **Joindre des photos** pour documenter l'écart.
4. **Suivre le traitement** : la NC évolue jusqu'à sa résolution.
5. **Relier à une action** : une NC peut déclencher une [CAPA](capa.md), un cycle
   [PDCA](pdca.md) ou un projet [DMAIC](dmaic.md) selon la gravité.

## L'IA au service des NC

- [Clustering de NC](nc-clusters.md) (`/nc-clusters`) : regroupe automatiquement les NC similaires
  pour révéler des familles de problèmes récurrents, invisibles à l'œil nu.

## Bonnes pratiques

- **Factuel et daté** : une NC bien décrite se traite plus vite.
- **Cause-racine, pas pansement** : reliez la NC à une analyse de cause ([Ishikawa](ishikawa.md))
  avant d'agir.
- **Surveillez les récurrences** : une même NC qui revient signale une cause non traitée — vérifiez
  les clusters.
