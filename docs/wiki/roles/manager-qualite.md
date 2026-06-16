# Rôle — Manager Qualité

[← Retour à l'index](../README.md)

## À quoi sert ce rôle

Le **Manager Qualité** est le pilote opérationnel au quotidien. Il lance et anime les cycles
d'amélioration, assigne les actions, et valide au niveau opérationnel. C'est le rôle qui utilise
le plus largement les modules métier.

## Permissions clés (CLAUDE.md §16)

- **Lancer et piloter** les cycles : [PDCA](../modules/pdca.md), [DMAIC](../modules/dmaic.md),
  [Cercles de Qualité](../modules/circles.md).
- **Assigner des actions** aux membres de l'équipe.
- **Valider** au niveau opérationnel (étapes, propositions, clôtures).

## Écrans et parcours typiques

### Lancer une démarche d'amélioration

1. Décrire le problème, puis choisir la méthode :
   - un problème de causes multiples → [Ishikawa](../modules/ishikawa.md) ;
   - un projet structuré et chiffré → [DMAIC](../modules/dmaic.md) ;
   - une amélioration itérative → [PDCA](../modules/pdca.md).
2. Lancer le cycle, définir les étapes et les indicateurs cibles.
3. Assigner les actions et suivre l'avancement.

### Traiter les écarts

- Saisir et qualifier les [non-conformités](../modules/non-conformites.md) (`/nc`).
- Ouvrir et suivre des [CAPA](../modules/capa.md) (`/capa`).
- Programmer et exploiter les [audits](../modules/audits.md) (`/audits`).

### S'appuyer sur l'IA

- [SPC](../modules/spc.md) (`/spc`) pour détecter une dérive de procédé.
- [Détection d'anomalies](../modules/anomaly.md) (`/anomaly`) sur des données multivariées.
- [Clustering NC](../modules/nc-clusters.md) (`/nc-clusters`) pour repérer des familles de NC.
- [Réclamations IA](../modules/complaints-nlp.md) (`/complaints-nlp`) pour prioriser par sentiment.

## Bonnes pratiques

- **Une cause = une action traçable** : reliez chaque action à sa cause (Ishikawa → CAPA → PDCA).
- **Mesurez l'efficacité** : une CAPA n'est close que lorsque son efficacité est vérifiée.
- **Ne dupliquez pas** : avant de traiter une NC, vérifiez si elle appartient à un cluster connu.

## Liens utiles

- Toutes les méthodes : [PDCA](../modules/pdca.md), [5S](../modules/fives.md),
  [Ishikawa](../modules/ishikawa.md), [DMAIC](../modules/dmaic.md), [Cercles](../modules/circles.md).
- [Directeur Qualité](directeur-qualite.md) pour la validation stratégique.
