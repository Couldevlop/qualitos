# ADR 0146 — La revue se confronte seule aux mesures

- **Statut** : accepté
- **Date** : 2026-11-28
- **Complète** : ADR 0133 (la règle d'un objectif qualité), ADR 0143 (l'objectif propose d'agir, il ne rédige pas la CAPA)

## Contexte

L'écran « Objectifs vs KPI réels » portait un bouton **« Confronter aux mesures »**.
Le pressant, le pilote déclenchait la règle de l'ADR 0133 sur tous les objectifs
suivis : les écarts tenus assez longtemps devenaient des propositions à trancher,
et un bandeau disait combien.

Le geste était explicite par choix : la règle ne devait pas tourner dans le dos
de l'utilisateur, et c'était à lui de décider quand la revue se tenait.

La recette a retourné cet argument. Un bouton qu'il faut presser laisse la revue
**en retard sur les relevés** tant que personne n'y pense — et pendant ce temps
un écart déjà mesuré, déjà qualifié par la règle, n'attend que l'attention de
quelqu'un pour exister. L'écran affiche alors des objectifs en alerte sans la
proposition qui devrait les accompagner, ce qui se lit comme une absence de
règle plutôt que comme une règle non déclenchée. Pire : le même écran, ouvert
par deux personnes, dit deux choses différentes selon qui a cliqué.

Le « dos de l'utilisateur » n'était d'ailleurs pas le bon souci. La règle ne
décide rien : elle **propose**, et la proposition attend explicitement une
décision humaine (ADR 0143). Rien ne se fait sans qu'on le voie.

## Décision

**Ouvrir la revue confronte les objectifs à leurs mesures.** Le bouton
disparaît, avec son gestionnaire, ses libellés et son bandeau de résultat.

**La confrontation reste un ACTE DE PILOTAGE.** C'est la correction d'un défaut
que la première version de cette décision avait introduit, et qu'un test de
contournement a mis au jour : la confrontation vivait dans la méthode de lecture,
or la lecture de la revue est **ouverte à tout authentifié** — c'est ce qu'une
organisation montre à ses équipes — tandis que l'écriture porte
`objective.manage`. La lecture ouverte exécutait donc ce que l'écriture refuse,
et un simple utilisateur devenait auteur de propositions.

Fermer la lecture aurait été le mauvais remède : il aurait supprimé une fonction
voulue. C'est l'écriture qui sort du chemin de lecture. `confronterAuxMesures()`
**vérifie le droit elle-même**, dans le service et non à l'annotation du point
d'entrée : la garde voyage avec le comportement, donc tout appelant futur la
rencontre. Sans le droit, la méthode ne fait rien **et ne refuse rien** — elle
n'est pas demandée par qui lit, elle est seulement déclenchée à l'occasion de sa
visite ; lever un 403 fermerait la revue par un autre chemin.

Trois bornes, sans lesquelles la décision serait fausse :

1. **Rejouer une revue PASSÉE n'écrit rien.** La confrontation ne porte que sur
   l'état courant (`asOf == null`). Confronter pendant une relecture d'archive
   daterait d'aujourd'hui des propositions nées d'une lecture d'hier — et la
   relecture d'une revue tenue doit dire ce qu'on y avait lu, pas ce qu'on y
   lirait maintenant.
2. **Un objectif qu'on ne peut pas confronter est passé, pas fatal.** KPI retiré
   du catalogue, sens d'amélioration absent, mesures illisibles : l'objectif
   paraît sans proposition, exactement comme avant que la règle n'existe, et le
   défaut part dans les traces. Tant que la confrontation était un geste, son
   échec se disait dans un bandeau et l'écran restait lisible ; devenue
   automatique, la même erreur emporterait la page entière et masquerait les
   trente objectifs qui, eux, se lisent très bien.
3. **Rafraîchir ne multiplie rien.** La règle rend la proposition déjà présente
   pour la même période, et la clé `uk_qo_proposals_periode` dit la même chose
   en base. Un écran ouvert trois fois ne propose pas trois fois le même dossier.

   Deux visites **simultanées** peuvent en revanche se croiser, et la clé est
   alors violée **au commit** — hors de portée d'un `try/catch` posé autour de la
   boucle, ce que la première version de cette décision avait manqué. C'est
   pourquoi la confrontation tourne dans **sa propre transaction**, distincte de
   celle de la revue, et pourquoi son échec est avalé au point d'appel, hors de
   cette transaction : elle échoue seule, la lecture reste servie, et la
   proposition paraît à la visite suivante.

## Conséquences

- `review` **reste** `@Transactional(readOnly = true)` et n'écrit rien : c'est ce
  qui rend la lecture sûre de pouvoir demeurer ouverte. Un banc le tient
  explicitement (`laRevue_nEcritJAMAIS`), parce que c'est la propriété qu'une
  régression future ferait disparaître sans bruit.
- Le point d'entrée manuel **reste** côté serveur : il est ce que la règle
  expose à un appelant qui n'est pas l'écran, et c'est par lui que les bancs de
  la règle la jouent isolément. Seul le **bouton** disparaît.
- Les libellés `objectives.evaluate-all`, `objectives.evaluated.some`,
  `objectives.evaluated.none` et `objectives.error.evaluate` sont retirés des
  tables i18n : ils nommaient un geste et un résultat qui n'existent plus.

## Reste ouvert

Confronter **hors de toute visite d'écran** — par une tâche périodique, pour que
la proposition naisse le jour du relevé et non le jour où quelqu'un regarde —
est la suite naturelle. Elle demande un acteur et un client hors requête, donc
un arbitrage à part. Non décidé ici.
