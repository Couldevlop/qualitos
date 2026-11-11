# Module Boîte à idées — Déposer, voter, arbitrer

[← Retour à l'index](../README.md) · Route : **`/idees`** · Menu : *Méthodes qualité › Boîte à idées*

## À quoi sert ce module

La boîte à idées est l'endroit où **n'importe qui** dans votre organisation dépose une
suggestion d'amélioration — l'opérateur qui voit un rebut à son poste, la secrétaire qui perd
dix minutes par dossier, le technicien qui connaît le réglage qui manque.

Elle existe pour une raison précise : avant elle, une proposition n'existait qu'**à l'intérieur
d'un cercle de qualité**. Qui n'était pas membre d'un cercle constitué n'avait nulle part où
parler. **Une boîte à idées qui exige un cercle pour y déposer n'en est plus une** : c'est
pourquoi l'écran reste accessible même si le module [Cercle de Qualité](circles.md) n'est pas
activé chez vous.

## Le tableau des idées

L'écran est un **tableau par colonnes**, une colonne par étape :

| Colonne | Ce qu'elle contient | Le bouton qui y mène |
| --- | --- | --- |
| **Proposée** | l'idée vient d'être déposée | *Déposer une idée* |
| **À l'étude** | elle est prise en considération | *À l'étude* |
| **Retenue** | l'organisation s'engage à la mettre en œuvre | *Retenir* |
| **Réalisée** | elle est en place | *Réalisée* |
| **Impact consigné** | son effet a été mesuré et écrit | *Consigner l'impact* |

Les idées **écartées** ne sont pas perdues : elles sont masquées par défaut et un bouton les
affiche. Chacune porte son **motif de rejet** — un motif est exigé, l'écarter sans rien dire
n'est pas possible.

## Parcours pas à pas

### Déposer une idée

1. **Ouvrir** `/idees`, puis **Déposer une idée**.
2. Donner un **titre** et, si utile, une description.
3. L'idée apparaît en colonne *Proposée*, à votre nom.

Vous pouvez la rattacher à un **cercle de qualité** si elle en est issue. Sans rattachement,
elle vit très bien seule.

### Voter

Un clic suffit, et il se retire aussi facilement. Deux règles, visibles à l'écran :

- **Une voix par personne et par idée.** La règle est tenue par la base de données, pas
  seulement par l'écran : deux clics simultanés ne produisent pas deux voix.
- **Le vote se ferme à la décision.** Dès qu'une idée est retenue, écartée, réalisée ou
  mesurée, le bouton disparaît. Le compteur dit alors l'adhésion **au moment où la décision a
  été prise** — ce qui est précisément ce qu'un arbitrage doit pouvoir montrer six mois plus
  tard. Sans cette fermeture, le score d'une idée approuvée continuerait de bouger et
  laisserait croire que voter a encore un effet.

Le nombre de voix est **recalculé à chaque affichage** à partir des voix réellement
enregistrées. Il n'y a pas de compteur mis de côté qui pourrait finir par mentir.

### Arbitrer

Mettre à l'étude, **retenir**, **écarter**, déclarer **réalisée** et **consigner l'impact**
sont réservés au **pilotage qualité** (manager qualité, directeur qualité, administrateur du
client). Déposer et voter, eux, sont ouverts à tout utilisateur de votre organisation.

Cette séparation n'est pas une hiérarchie de principe : **retenir** une idée engage
l'organisation à la mettre en œuvre, et **écarter** la retire de la vue de tous avec un motif
qui devra rester défendable en audit. Ce sont des actes de pilotage.

Dernière garantie : **l'arbitre ne peut pas être l'auteur**. L'identité de l'un et de l'autre
est lue du jeton de connexion, jamais fournie par l'écran — personne ne valide sa propre idée.

### Consigner l'impact

Une idée réalisée n'est pas une idée aboutie. La dernière étape demande une **note d'impact** :
ce que le changement a produit. C'est elle qui rend le dispositif crédible auprès de ceux qui
déposent — et qui alimente la revue de direction.

## Son lien avec les Cercles de Qualité

La boîte à idées et les propositions d'un [cercle de qualité](circles.md) sont **le même
objet**, vu par deux portes : même cycle de vie, même piste d'audit, même compteur de voix.

Concrètement : une idée née en cercle se vote et s'arbitre indifféremment depuis l'écran du
cercle ou depuis `/idees`, sur la **même ligne** — il n'existe pas deux vérités à rapprocher.
Et une idée déposée au comptoir, sans cercle, suit exactement la même machine à états.

## Bonnes pratiques

- **Répondez vite, même pour écarter.** Une colonne *Proposée* qui s'allonge sans décision tue
  le dispositif plus sûrement qu'un refus motivé.
- **Écrivez le motif pour l'auteur, pas pour l'auditeur.** Il sera lu par la personne qui a
  déposé l'idée ; l'auditeur viendra après.
- **Servez-vous du vote comme d'un signal, pas d'un verdict.** Une idée peu votée peut être la
  bonne : le vote dit l'adhésion, pas la pertinence technique.
- **Consignez l'impact même quand il est faible.** C'est la mesure, pas la promesse, qui fait
  revenir les contributeurs.
- **Rattachez au cercle quand l'idée en vient.** Le cercle garde ainsi la trace de ce qu'il a
  produit.
