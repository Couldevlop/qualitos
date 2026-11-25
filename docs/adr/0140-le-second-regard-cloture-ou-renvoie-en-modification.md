# 0140 — Le second regard clôture, ou renvoie en modification

- **Statut** : accepté
- **Date** : 2026-11-25
- **Décideurs** : direction qualité, architecte principal
- **Concerne** : `nonconformity` (moteur qualité), fiche de l'écart (SPA)
- **Révise** : ADR 0120 (gravité d'un écart), complète la règle du second regard sur la clôture

## Contexte

Clore un écart, c'est déclarer « plus rien à faire ici ». La plateforme exige depuis
longtemps que ce ne soit pas la personne qui a écrit la résolution qui le déclare :
sans cela le registre affirmerait l'écart traité **et** vérifié sans qu'aucun contrôle
indépendant n'ait eu lieu (ISO 9001 §10.2). Le droit de clore appartient donc aux rôles
de pilotage **et** à la personne désignée sur cet écart.

Ce second regard n'avait qu'une réponse : **oui**. S'il jugeait que l'action corrective
ne répondait pas à la cause racine, qu'une preuve d'efficacité manquait, que le rapport
8D se trompait, aucun geste de la plateforme ne le disait. Dans les faits, on
téléphonait au pilote — et le registre gardait un écart **« résolu »** que personne ne
clôturait, sans qu'aucune fiche ne porte la raison de cette attente. Un écart bloqué et
muet ressemble exactement à un écart oublié.

La reprise (`reopen`) ne tenait pas ce rôle. Elle existe pour la **récidive** : un écart
*refermé* qui revient, et elle ramène l'instruction à son début parce que la cause ou
l'action a manqué. S'en servir pour un refus de clôture ferait compter comme récidive un
écart qui n'a jamais été clos, et obligerait le pilote à retraverser l'analyse pour
corriger une ligne d'action.

## Décision

**Le second regard a deux réponses : clôturer, ou renvoyer en modification.**

1. **Même porte que la clôture.** `POST /api/v1/nc/{id}/send-back` applique
   `exigerUnSecondRegard` : rôles de pilotage ou personne désignée, et refus pour qui a
   résolu l'écart. Qui peut dire oui doit pouvoir dire non — un second regard qui ne peut
   que valider n'en est pas un. Comme la clôture, le point d'entrée n'est pas annoté :
   l'intercepteur de permissions tranche avant de savoir quel écart est visé, et ne peut
   donc pas connaître la désignation.
2. **Retour à `ACTION_DEFINED`**, et non à `OPEN`. Le renvoi dit que le traitement n'est
   pas encore bon, pas que l'instruction est à refaire : l'analyse de cause, l'équipe,
   les actions et les preuves restent en place. C'est aussi le seul état depuis lequel
   `resolve` se reprend, donc celui qui rend l'écart à son pilote sans lui faire
   retraverser ce qui tenait.
3. **Message facultatif.** L'exiger découragerait le renvoi lui-même, et un renvoi sans
   phrase vaut mieux qu'une clôture de complaisance. Le nom de qui renvoie, lui, est
   toujours copié : le second regard ne se démontre que signé.
4. **Rien n'est effacé.** `resolvedAt`, la note de résolution et la signature de qui a
   résolu restent ce qu'elles furent ; `sentBackAt` est un fait de plus. Le bandeau de la
   fiche montre le **plus récent** des faits, en comparant les dates : un renvoi précède
   la clôture qui finit par venir, et une fiche close le 25 ne doit pas annoncer
   « renvoyé le 20 ».
5. **Le bouton ignore la liste d'obstacles.** Un dossier CAPA encore en travail ou une
   révision d'analyse de risque qui manque éteignent la *clôture* — et ce sont
   précisément les situations qui appellent un renvoi. Les opposer au renvoi ne
   laisserait au valideur aucune réponse du tout.

## Conséquences

- Un écart résolu porte deux gestes pour son valideur, et le registre ne garde plus
  d'écart « résolu » en attente muette.
- Le renvoi se compte à part de la récidive (`sent_back_at` contre `reopened_at`) : la
  mesure de ce que le second regard attrape devient possible, et le taux de récidive
  n'est pas pollué.
- **Ce que cette décision ne règle pas** : un **dossier CAPA clos** reste gelé
  (`updateCase`, les actions, les preuves et les verdicts refusent toute modification) et
  aucune réouverture de dossier n'existe. Comme un dossier peut se clore dès que son
  écart est résolu, le pilote qui reçoit un renvoi peut retrouver ses actions figées : il
  corrige alors la fiche, l'analyse et le rapport, mais pas les actions. L'arbitrage sur
  ce point — réouverture explicite du dossier, ou levée du verrou tant que l'écart vit —
  est à prendre.
- Le module `nonconformity` reste un **paquet plat**, antérieur à la règle §18.3. Ce lot
  s'y ajoute sans l'aggraver et sans le replier : le repli est un chantier décidé, un
  module par lot (CLAUDE.md §22 #16).
- **Dette repérée, non traitée** : le bouton *Clôturer* de la fiche est enveloppé dans
  `*qosCan="'nc.close'"`, donc invisible à la personne **désignée** qui n'a pas le rôle —
  alors que le serveur l'admet. Le bouton *Renvoyer* suit la même porte pour rester
  cohérent avec lui ; corriger les deux demande une condition qui interroge la
  désignation, et c'est un lot à part.
