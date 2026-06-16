# Module DMAIC + Poka-Yoke — Six Sigma

[← Retour à l'index](../README.md) · Route : **`/dmaic`** · Menu : *Méthodes qualité › DMAIC*

## À quoi sert ce module

Le **DMAIC** est la démarche structurée de Six Sigma pour réduire la variabilité d'un procédé,
en **5 phases** :

1. **Define** — définir le problème, le périmètre, l'objectif.
2. **Measure** — mesurer l'état actuel (données chiffrées).
3. **Analyze** — analyser les causes de variation.
4. **Improve** — améliorer, mettre en place des solutions.
5. **Control** — pérenniser et maîtriser dans la durée.

Le module gère des **projets DMAIC** que l'on fait **avancer de phase en phase**, avec saisie de
**mesures**, calcul de **capabilité** (Cp/Cpk) et association de dispositifs **Poka-Yoke**
(anti-erreur).

## Parcours pas à pas

1. **Ouvrir** `/dmaic` : la liste des projets, filtrable par statut et par **phase**.
2. **Créer un projet** : titre, problème, objectif.
3. **Saisir des mesures** dans la phase *Measure*.
4. **Consulter la capabilité** (Cp/Cpk) calculée à partir des mesures.
5. **Faire avancer** le projet de phase en phase (*advance*). Un projet peut aussi être mis en
   **attente** (*hold*) puis **repris** (*resume*), ou **annulé**.
6. **Associer des Poka-Yoke** : piochez dans la bibliothèque de dispositifs et rattachez-les au
   projet pour fiabiliser la solution.

## Liens avec les autres modules

- Un projet DMAIC peut découler d'un [Ishikawa](ishikawa.md) ou d'une
  [non-conformité](non-conformites.md) grave.
- Les mesures et la capabilité s'articulent avec les cartes de contrôle [SPC](spc.md).
- Les indicateurs du projet alimentent les KPIs (`/kpis`).

## Bonnes pratiques

- **Pas d'Improve sans Measure** : on n'améliore que ce que l'on a mesuré.
- **Poka-Yoke en priorité** : un dispositif anti-erreur vaut mieux qu'un contrôle a posteriori.
- **Phase Control = pérennité** : prévoyez le suivi qui empêchera la dérive de revenir.
