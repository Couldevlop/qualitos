# Module Clustering de non-conformités

[← Retour à l'index](../README.md) · Route : **`/nc-clusters`** · Menu : *Méthodes qualité › Clustering NC*

## À quoi sert ce module

Quand les [non-conformités](non-conformites.md) s'accumulent, des **familles de problèmes**
récurrents passent souvent inaperçues. Le **clustering** regroupe automatiquement les NC dont la
description se ressemble, pour faire émerger ces motifs. L'analyse repose sur une vectorisation du
texte (**TF-IDF**) suivie d'un regroupement par densité (**DBSCAN**).

## Parcours pas à pas

1. **Ouvrir** `/nc-clusters`.
2. **Lancer le clustering** sur le périmètre de NC choisi.
3. **Explorer les groupes** : chaque cluster rassemble des NC proches ; les NC isolées
   apparaissent à part.
4. **Décider** : un cluster volumineux signale une cause-racine commune à traiter en priorité.

## Liens avec les autres modules

- Un cluster identifié peut être pris en charge globalement par un [Ishikawa](ishikawa.md) suivi
  d'une [CAPA](capa.md) ou d'un projet [DMAIC](dmaic.md).

## Bonnes pratiques

- **Traitez la famille, pas l'unité** : corriger la cause commune d'un cluster évite des dizaines
  de NC futures.
- **Des descriptions homogènes** améliorent le regroupement : encouragez une saisie claire des NC.
- **Réévaluez périodiquement** : les clusters évoluent à mesure que de nouvelles NC arrivent.
