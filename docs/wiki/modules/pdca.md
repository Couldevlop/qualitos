# Module PDCA — Roue de Deming

[← Retour à l'index](../README.md) · Route : **`/pdca`** · Menu : *Méthodes qualité › PDCA*

## À quoi sert ce module

Le **PDCA** (*Plan-Do-Check-Act*) structure l'amélioration continue en un cycle de 4 étapes :

1. **Plan** — planifier : analyser, fixer un objectif et un indicateur cible.
2. **Do** — faire : mettre en œuvre les actions.
3. **Check** — vérifier : mesurer les résultats par rapport à la cible.
4. **Act** — agir : standardiser ce qui marche, corriger le reste, relancer un cycle si besoin.

Dans QualitOS, un PDCA prend la forme d'un **cycle** que l'on fait **avancer** d'étape en étape,
avec une trace de chaque transition.

## Parcours pas à pas

1. **Ouvrir** le module via `/pdca` : la liste affiche les cycles existants et leur statut.
2. **Créer un cycle** (bouton de création) : titre, description, objectif.
3. **Ajouter des étapes** au cycle pour détailler les actions de chaque phase.
4. **Faire avancer** le cycle (action *advance*) lorsque la phase courante est terminée :
   le cycle passe à l'étape suivante (Plan → Do → Check → Act).
5. **Joindre la preuve** de chaque étape réalisée : colonne *Preuve* du tableau
   (voir ci-dessous).
6. **Suivre** l'avancement depuis la fiche du cycle ; chaque transition est historisée.
7. Si un cycle n'a plus lieu d'être, il peut être **annulé**.

## La colonne *Preuve* — le document qui atteste l'étape

Une étape marquée *Terminée* sans document ne prouve rien : elle **affirme**. Ce
qu'un auditeur demande en face d'une ligne cochée, c'est la pièce — un relevé, une
procédure signée, une photo du dispositif en place.

La colonne *Preuve*, située juste après l'échéance, porte **un document par
étape** :

- **Joindre** : le bouton *Joindre* ouvre le sélecteur de fichier. Formats
  acceptés : PDF, image (JPEG, PNG, WEBP, HEIC), Word (`.docx`), Excel (`.xlsx`).
  **10 Mo au maximum par pièce**, 25 Mo cumulés sur l'ensemble du cycle.
- **Consulter** : une fois versé, le document s'affiche avec son icône et son nom,
  cliquable pour l'ouvrir dans un nouvel onglet.
- **Remplacer** : retirez la pièce en place, puis versez la nouvelle. Il n'y a pas
  de remplacement en un geste, et c'est voulu — les deux opérations sont
  journalisées, un remplacement silencieux ne le serait pas.
- **Retirer** : la croix rouge, après confirmation. C'est le seul geste qui fait
  disparaître une preuve d'un dossier ; il est tracé au journal d'audit du tenant
  avec son auteur.

Quelques comportements attendus :

- Le contenu du fichier est vérifié contre le format annoncé : un exécutable
  renommé en `.pdf` est refusé.
- Un **cycle clos ou annulé** ne se regarnit plus. Ses preuves restent
  consultables — c'est même à ce moment-là qu'on les regarde — mais rien ne s'y
  ajoute ni ne s'en retire. Un dossier qu'on peut compléter après coup ne prouve
  plus rien.
- Le **retrait** d'une preuve est réservé au *Manager Qualité* et au-dessus. Tout
  utilisateur authentifié peut en revanche en verser une.
- Si l'environnement n'a pas de stockage de pièces jointes configuré, un bandeau
  le dit et la colonne n'affiche aucun bouton, plutôt qu'un bouton sans effet.

## Liens avec les autres modules

- Une cause identifiée dans un [Ishikawa](ishikawa.md) peut donner lieu à un cycle PDCA.
- Une [non-conformité](non-conformites.md) peut s'instancier en cycle PDCA selon sa gravité.
- L'avancement des cycles alimente les indicateurs (`/kpis`).

## Bonnes pratiques

- **Un cycle = un objectif mesurable** : sans indicateur cible, la phase *Check* n'a pas de sens.
- **Ne sautez pas le Check** : vérifiez avant de standardiser (*Act*).
- **Bouclez** : un PDCA bien mené se traduit soit par une standardisation, soit par un nouveau cycle.
- **Versez la preuve au moment où l'étape se termine**, pas à la veille de l'audit :
  une pièce produite après coup a pu être fabriquée pour l'occasion, et cela se voit.
