# ADR 0128 — Clore un écart, c'est dire ce qu'il a enseigné à l'analyse de risque

- **Statut** : accepté
- **Date** : 2026-11-19
- **Portée** : non-conformités (clôture, motifs de blocage), registre des demandes de révision (port de lecture)

## Contexte

Un écart traité enseigne quelque chose sur l'analyse des risques du produit :
soit un mode de défaillance que le PFMEA n'avait pas prévu, soit un contrôle que
le plan de surveillance ne portait pas. L'ISO 9001 §10.2 attend que la leçon
soit tirée ; l'IATF 16949 en fait une exigence explicite — toute non-conformité
se répercute sur la PFMEA et le plan de surveillance.

Rien ne le demandait. La clôture d'un écart était un franchissement muet : un
clic, et le constat se refermait. Personne n'avait à se prononcer sur l'analyse
de risque, et le PFMEA restait donc muet sur un défaut qui était survenu **pour
de bon** — l'écart exact qu'un auditeur cherche en premier, et le seul que le
registre ne savait pas montrer.

Un mécanisme existait, mais à l'autre bout du cycle : le registre des demandes
de révision **propose** une évolution du PFMEA dès qu'un écart est constaté sur
un produit. C'est une proposition, pas une obligation, et elle naît à la
création — quand on ne sait encore rien de la cause. Rien ne reliait ce qu'on
avait appris en traitant l'écart à ce que l'analyse de risque devait en retenir.

## Décision

**La clôture pose deux questions, et un « oui » rend la révision exigible.**

1. **Deux questions, obligatoires.** Le PFMEA du produit a-t-il été impacté ?
   Son plan de surveillance ? Les deux réponses sont dues avant la clôture.
   Deux questions plutôt qu'une : les deux documents se révisent séparément, par
   des gestes distincts, et une réponse unique laisserait chercher lequel des
   deux est en cause.

2. **`NULL` n'est pas `FALSE`.** `NULL` veut dire « la question n'a pas encore
   été posée » ; `FALSE` veut dire « elle l'a été, et on a jugé que non ». La
   première ne prouve rien. La seconde est une décision datée et imputable — et
   c'est **elle** que l'auditeur vient lire quand le PFMEA n'a pas bougé. Les
   écarts clos avant cette règle gardent donc `NULL` : la fiche n'invente pas
   une décision que personne n'a prise.

3. **Aucune réponse présélectionnée.** Un « non » proposé d'avance serait coché
   par le seul effet du clic suivant. Le bouton reste éteint jusqu'à ce que les
   deux questions soient tranchées.

4. **Un « oui » pose un verrou, levé par une révision *acceptée*.** Tant qu'une
   demande de révision déclenchée par cet écart et visant le document désigné
   n'est pas `ACCEPTED`, la clôture est retenue. Une proposition `PENDING` ne
   compte pas : le registre en crée une automatiquement, et la prendre pour une
   révision lèverait le verrou avant que personne n'ait rien décidé. Une
   proposition `REJECTED` ne compte pas non plus : elle dit qu'on ne révise pas,
   ce qui contredit la déclaration d'impact.

5. **Ce n'est pas une impasse : une réponse se corrige.** Un produit sans PFMEA
   n'a aucune révision à acter, et la sortie est de répondre « non » — ce qui
   est vrai — jamais de forcer la clôture. La fenêtre relit donc les réponses
   déjà données, pour qu'on voie ce qu'on avait dit et qu'on puisse le changer.

6. **La règle a UNE implémentation, lue par deux chemins.** Le motif annoncé
   avant le clic et le refus au clic appellent la même méthode. S'ils
   divergeaient, l'écran proposerait un geste que le serveur refuse — ou
   l'inverse.

7. **Le refus se lit.** Il passe par `NcGateException`, donc par le canal
   déclaré `userMessage` / `reasons`, comme le refus de clôture d'un projet
   DMAIC (ADR 0108). Un 409 nu se traduisait à l'écran par « État incompatible —
   rechargez la page » : un conseil faux, puisque recharger ne révise rien.

8. **Un port, et non un appel direct.** Le registre des révisions dépend déjà
   de la non-conformité — il écoute ses événements et lit ses fiches. L'appeler
   depuis la non-conformité fermerait le cycle. La question est donc posée à
   travers `NcRiskRevisionsPort`, déclaré côté non-conformité, et c'est le
   module des révisions qui vient brancher son adaptateur : la flèche garde son
   sens.

9. **Le refus de ce verrou-là écrit quand même la réponse** — un second essai
   n'a pas à la redonner. Les refus **antérieurs** (second regard, dossier CAPA
   en travail), eux, n'écrivent rien : ils retiennent la clôture pour une autre
   raison, et dater une décision que ce refus vient d'annuler la daterait d'un
   geste qui n'a pas eu lieu.

## Conséquences

- Une colonne de plus par document sur `non_conformities`, toutes deux
  nullables (`V176`). Pas de reprise de données : l'absence de réponse sur les
  écarts clos est l'information juste.
- `POST /api/v1/nc/{id}/close` exige désormais un corps. C'est une rupture
  d'API assumée : un corps optionnel aurait laissé un appelant clore sans se
  prononcer, ce qui est exactement ce que cette décision supprime.
- Deux motifs de plus dans `NcBlockerCode`. Comme les autres, ils portent un
  **décompte** et jamais un titre : un invariant ne doit pas dépendre de qui
  regarde (ADR 0081).
- Un écart déclaré impactant reste visible dans les listes de travail jusqu'à
  sa révision. C'est voulu : c'est le travail qui reste.

## Alternatives écartées

- **Déduire l'impact au lieu de le demander.** On aurait pu décider qu'un écart
  portant un produit et un mode de défaillance impacte le PFMEA. Mais la règle
  aurait reproché à quelqu'un de n'avoir pas fait ce qu'on ne lui avait pas
  demandé, et un défaut dont la cause est extérieure au process (un lot
  fournisseur conforme, une erreur de saisie) aurait été bloqué sans issue
  honnête.
- **Accepter une révision `PENDING` comme preuve.** Plus commode, et faux : la
  proposition est créée par le système, pas par une décision humaine. Le verrou
  se lèverait tout seul.
- **Poser la question à la résolution plutôt qu'à la clôture.** On ne sait pas
  encore, à la résolution, si ce qui a été fait tient — et la clôture est le
  moment où l'on dit « la leçon est tirée ».
- **Un seul booléen « analyse de risque impactée ».** Une colonne de moins, et
  un motif qui laisse chercher lequel des deux documents attend sa révision.
