# Module Non-conformités (NC)

[← Retour à l'index](../README.md) · Route : **`/nc`** · Menu : *Qualité opérationnelle › Non-conformités*

## À quoi sert ce module

Une **non-conformité (NC)** est un écart entre une exigence (norme, procédure, spécification) et
la réalité observée. Le module permet de **déclarer**, **qualifier** et **suivre** les NC, avec
possibilité d'y joindre des **photos**.

## Parcours pas à pas

1. **Ouvrir** `/nc` : la liste des non-conformités et leur statut.
   Le filtre **Ouvertes** rassemble tout ce qui court — *ouverte*, *en analyse*,
   *action définie* et *résolue* — et non le seul premier de ces quatre états. Un écart ne
   quitte pas le travail en cours parce qu'on vient d'y définir une action ; un écart
   *résolu* attend encore sa vérification d'efficacité et sa clôture. Les issues —
   *clôturée*, *annulée*, *rejetée* — se demandent une par une.
2. **Déclarer une NC** : décrire l'écart (quoi, où, quand), sa gravité et le périmètre concerné.
   Vous choisissez sa **nature** dans une liste unique, où les natures sont rangées sous leur
   famille (produit, processus, documentaire…) : la famille se déduit de la nature retenue, il n'y
   a donc qu'une question à répondre au lieu de deux enchaînées. La nature est **obligatoire** —
   c'est elle qui regroupera cet écart avec ses semblables.
   Vous **nommez l'équipe** qui le prend en charge — au moins une personne, choisie dans l'annuaire.
   Vous y êtes posé d'office en tête : retirez-vous si vous ne suivez pas l'écart.
3. **Joindre des photos** pour documenter l'écart.
4. **Suivre le traitement** : la NC évolue jusqu'à sa résolution. *Résoudre* ne demande plus
   rien : ce qui a résolu l'écart est déjà écrit, et mieux — les actions menées au D5 et au D7
   du [rapport 8D](rapport-8d.md), les preuves de leur efficacité au D6.
5. **Relier à une action** : *Nouvelle action* ouvre le dossier CAPA qui traite l'écart et sa
   première action (source verrouillée ; le titre et la description partent **vides** — ce sont
   des phrases à écrire, pas le constat à recopier). Une fois le dossier ouvert, le même bouton
   devient *Voir les actions* et liste **ce qui reste à faire** ; une ligne mène au dossier.
   Les actions suivantes s'ajoutent, s'avancent et se prouvent dans le **tableau des actions**
   de la fiche, sous l'analyse de cause. Une NC peut déclencher une [CAPA](capa.md), un cycle
   [PDCA](pdca.md) ou un projet [DMAIC](dmaic.md) selon la gravité.
6. **Faire relire, puis clôturer** : une fois l'écart *résolu*, c'est à quelqu'un d'autre de
   trancher — voir « Le second regard » ci-dessous. Le [rapport 8D](rapport-8d.md) rassemble
   alors les huit disciplines attendues par le client, dont six sont reprises automatiquement
   de ce dossier.

## Le second regard : clôturer, ou renvoyer

**Personne ne valide son propre travail.** La personne qui a écrit la résolution ne peut pas
clôturer l'écart : ce serait déclarer le problème traité *et* vérifié d'une seule signature,
et l'organisation n'aurait rien à montrer à un auditeur (ISO 9001 §10.2). La fiche le dit
avant le clic, et le bouton reste éteint avec son explication.

Qui porte ce second regard ? Les rôles de pilotage, **ou** la personne **désignée** sur
l'écart (champ *Valideur* de la fiche) — même sans rôle qualité. C'est ce qui permet à une
petite organisation, ou à celle qui veut un regard venu d'un autre service, de confier la
relecture à un chef d'atelier ou à un pilote de processus.

Ce second regard a **deux réponses** :

- **Clôturer** : plus rien à faire ici. La clôture pose les deux questions d'analyse de risque
  (le PFMEA et le plan de surveillance sont-ils impactés ?) et refuse tant qu'un dossier CAPA
  de l'écart reste en travail.
- **Renvoyer** : le traitement revient à son pilote, et l'écart repasse à *action définie*.
  L'analyse de cause, l'équipe, les actions, les preuves et le brouillon du rapport 8D restent
  en place : on corrige ce qui ne va pas, puis on déclare l'écart résolu à nouveau. Le message
  est **facultatif** — mais sans lui, le pilote devra deviner ce qui ne va pas.

Deux points qui comptent :

- **Le renvoi n'est pas une reprise.** *Renvoyer* dit « le traitement n'est pas encore bon »
  sur un écart jamais clos ; *Rouvrir* dit « le problème est revenu » sur un écart refermé, et
  ramène l'instruction à son début. Les deux se comptent séparément, pour que le taux de
  récidive ne compte pas les refus de clôture.
- **Rien n'est effacé.** La résolution précédente reste datée et signée ; le renvoi s'y ajoute.
  Le bandeau de la fiche montre le fait le plus récent, et l'historique ne perd rien.

> Si le dossier CAPA de l'écart a déjà été **clos**, ses actions ne se modifient plus : un
> dossier clos est figé. Après un renvoi, on corrige la fiche, l'analyse et le rapport, mais
> une action déjà close demande un nouveau dossier.

## L'équipe en charge

L'équipe nommée à la déclaration est la **première discipline du rapport 8D** (D1 « Constituer
l'équipe ») : elle n'est donc pas à retaper à la clôture.

- **L'ordre compte** : le premier nommé est le **pilote** de l'écart. Les flèches de la fiche
  remontent quelqu'un d'un rang, c'est ainsi qu'on change de pilote.
- **Douze personnes au plus** : au-delà, ce n'est plus une équipe de résolution mais une liste de
  diffusion.
- **Les noms sont figés à la saisie** : un rapport relu trois ans plus tard porte les noms de
  l'époque, même si la personne a quitté l'organisation.
- **L'équipe se remanie** tant que l'écart est ouvert (bouton *Modifier l'équipe* sur sa fiche).
  Sur un écart clos, annulé ou rejeté, elle se lit sans se modifier : c'est l'archive du D1.
- **Un écart ouvert par la plateforme** — un constat d'audit, un relevé venu d'un connecteur —
  n'a pas d'équipe : la fiche le dit, et c'est à vous de la nommer en reprenant le dossier.
- Être nommé dans une équipe **ne donne aucun droit supplémentaire** : la visibilité des dossiers
  reste celle de votre rôle.

## Ce que la fiche montre sans qu'on en sorte

La fiche d'un écart rassemble ce que le rapport 8D rassemble, mais **pendant** le dossier et non
à sa clôture. Après les faits et l'équipe, deux cartes :

### Analyse de cause (ce que le 8D appelle D4)

Chaque **chaîne 5 Pourquoi** et chaque **diagramme Ishikawa** rattachés à l'écart s'affichent
dessinés — la cascade et l'arête de poisson, pas une liste de libellés. C'est la même figure que
dans l'écran de la méthode : les familles de causes y sont dans l'ordre des M, qui est l'ordre de
lecture d'un qualiticien.

- Les cartes sont **en lecture**. Pour modifier une cause, suivez le lien vers l'écran du
  diagramme ou de la chaîne : c'est là qu'est l'outillage d'édition.
- **Aucune analyse rattachée ?** La carte le dit explicitement. Un blanc se lirait comme un
  défaut d'affichage ; l'écart sans analyse de cause est une information en soi — et un écart
  significatif qu'on clôture sans analyse revient.

### Actions correctives en cours (D3, D5, D7)

Les actions du dossier CAPA lié s'affichent sur place, **en tableau** : une ligne par action, avec
sa nature (corrective, préventive ou d'endiguement), son intitulé, son responsable, son échéance,
son statut, et le bouton qui la **fait avancer d'un cran** — *Démarrer*, puis *Terminer* — sans
quitter l'écart.

- **Un écart se traite en plusieurs gestes**, et le 8D les nomme séparément : endiguer (D3),
  corriger la cause (D5), prévenir la récidive (D7). Le dossier porte autant d'actions qu'il en
  faut.
- **Cliquez une ligne** pour corriger l'action : son intitulé, sa nature, son responsable, son
  échéance. Sur un dossier terminé, la ligne s'ouvre quand même, **en lecture seule** : relire ce
  qui a été fait reste légitime après la clôture.
- Le responsable et l'échéance sont donnés pour **chaque** action, même quand ils sont ceux du
  dossier : c'est précisément ce qu'on vient comparer d'une ligne à l'autre.
- Le **pilote du dossier** est le responsable de la *première* action. Les suivantes ne le
  déplacent pas.
- Une colonne **Preuve** porte la pièce qui atteste que *ce geste-là* a été fait : un bon de
  travail, une photo d'après, un procès-verbal. Une pièce par action, déposée et retirée sur
  place. À ne pas confondre avec les **preuves du dossier**, restées sur la fiche CAPA, qui
  attestent que le problème a cessé : un dossier peut très bien avoir toutes ses actions prouvées
  et aucune preuve d'efficacité.
- Le lien vers le dossier reste là pour ce que le tableau ne montre pas : l'historique du dossier
  et le verdict d'efficacité.
- **Un dossier sans action** — ancien, ou ouvert par un automate — le dit, et propose d'en ajouter
  une : sans action, il ne pourra pas être clôturé.
- Si le dossier CAPA ne peut pas être lu (service indisponible), la carte **le dit** et propose de
  réessayer. Elle ne reste jamais vide : « vide » voudrait dire « aucune action ».

## L'IA au service des NC

- [Clustering de NC](nc-clusters.md) (`/nc-clusters`) : regroupe automatiquement les NC similaires
  pour révéler des familles de problèmes récurrents, invisibles à l'œil nu.

## Bonnes pratiques

- **Factuel et daté** : une NC bien décrite se traite plus vite.
- **Cause-racine, pas pansement** : reliez la NC à une analyse de cause ([Ishikawa](ishikawa.md))
  avant d'agir.
- **Surveillez les récurrences** : une même NC qui revient signale une cause non traitée — vérifiez
  les clusters.
- **Nommez l'équipe tout de suite** : c'est la première question posée en revue, et la retrouver
  des semaines plus tard, de mémoire, donne un D1 approximatif.
- **Décidez l'endiguement tôt**, en action du dossier : le [rapport 8D](rapport-8d.md) le lit
  là (D3), et c'est ce qu'un client veut savoir en premier. Il s'oublie dès que la cause est
  trouvée.
