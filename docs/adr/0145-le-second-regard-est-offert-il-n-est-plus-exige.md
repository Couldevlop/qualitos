# ADR 0145 — Le second regard est offert, il n'est plus exigé

- **Statut** : accepté
- **Date** : 2026-11-28
- **Amende** : ADR 0140 (le second regard clôture ou renvoie en modification)

## Contexte

Depuis l'ADR 0140, clore un écart était refusé à la personne qui l'avait résolu.
Trois mécanismes portaient cette règle :

1. `NcService.exigerUnSecondRegard` refusait la clôture et le renvoi à l'auteur
   de la résolution, par un `NcStateException` explicite ;
2. le motif `SECOND_LOOK_REQUIRED` éteignait le bouton avant le clic — le seul
   motif de blocage qui dépendait de **qui lit** la fiche ;
3. `exigerLeDroitDeDesigner` interdisait à cette même personne de nommer le
   valideur, pour qu'elle n'y place pas un complaisant.

Le motif invoqué était la séparation des tâches : personne ne valide son propre
travail, et le registre ne doit pas déclarer un écart traité **et** vérifié sans
qu'aucun contrôle indépendant n'ait eu lieu (ISO 9001 §10.2).

La recette a montré ce que cette règle coûte en exploitation. Une organisation
qui ne compte qu'une personne en charge de la qualité constate, traite et clôt :
elle n'a personne à qui passer le relais. Ses écarts entièrement résolus
restaient ouverts, non parce qu'il manquait un travail, mais parce qu'il manquait
un second utilisateur disponible. La désignation d'un valideur, prévue comme
échappatoire, ne suffisait pas : elle n'exemptait pas de la séparation, donc
l'écart restait bloqué dès que le valideur désigné était aussi celui qui avait
résolu.

## Décision

**Le second regard reste une fonctionnalité de la plateforme. Il cesse d'être
une condition.**

Ce qui demeure, inchangé :

- le **valideur se désigne** sur l'écart, et cette personne peut le clore **même
  sans rôle qualité** — c'est ce qui ouvre le geste à un regard venu d'ailleurs ;
- le **renvoi en modification** (ADR 0140) reste la seconde réponse de ce
  regard : qui peut dire oui doit pouvoir dire non ;
- le **droit** de clore reste vérifié : les rôles de pilotage, ou la personne
  désignée. Rien n'est ouvert à tout le monde.

Ce qui disparaît :

- le refus opposé à l'auteur de la résolution, pour la clôture comme pour le
  renvoi ;
- le motif de blocage `SECOND_LOOK_REQUIRED`, qui n'est plus jamais émis et dont
  le code est retiré plutôt que laissé inatteignable ;
- l'interdiction faite à l'auteur de la résolution de désigner le valideur :
  elle n'empêche plus rien, puisqu'il peut clore lui-même.

## Ce que nous perdons, et ce que nous gardons

**Nous perdons une garantie technique.** La plateforme ne peut plus affirmer, du
seul fait qu'un écart est clos, qu'un contrôle indépendant a eu lieu. Un
auditeur qui cherchait cette preuve dans le comportement du logiciel ne l'y
trouvera plus.

**Nous gardons la trace, qui est ce dont l'audit a réellement besoin.**
`resolvedById` et `closedById` restent écrits et datés, et l'écran les montre.
Savoir si les deux gestes sont d'une même main reste une question à laquelle le
registre répond — mais c'est désormais l'organisation qui décide si cela lui
convient, et non le logiciel qui le décide à sa place. Une organisation qui veut
la séparation la pratique et la démontre par ses enregistrements ; une
organisation qui ne peut pas la pratiquer n'est plus empêchée de travailler.

C'est le bon niveau : la séparation des tâches est une règle d'organisation,
variable selon la taille et le secteur. Elle relève du paramétrage d'un client,
pas d'un invariant du moteur.

## Conséquences

- `NcService.exigerUnSecondRegard` devient `exigerLeDroitDeClore` : le nom dit ce
  que la méthode fait, c'est-à-dire le seul contrôle de droit qui subsiste.
- Plus aucun motif de blocage ne dépend de qui lit la fiche. Le front perd le
  prédicat `jeSuisLeResolveur` et les deux branches qui en dépendaient.
- Les libellés `nc.blocker.second-look` et `nc.8d.validator-own-work` sont
  retirés des tables i18n ; l'aide du sélecteur de valideur ne promet plus un
  refus qui n'existe plus.
- Cinq bancs qui affirmaient le refus sont **inversés**, et non supprimés : ils
  tiennent désormais la règle nouvelle, y compris le fait que les deux
  signatures restent écrites.

## Reste ouvert

Rendre la séparation **paramétrable par client** — exigée chez qui le veut,
facultative ailleurs — est la suite naturelle de cette décision, et rejoint la
règle maison selon laquelle les règles métier chiffrées se paramètrent par
tenant. Non décidé ici.
