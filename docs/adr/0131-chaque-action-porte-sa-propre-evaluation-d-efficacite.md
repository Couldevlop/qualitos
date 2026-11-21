# ADR 0131 — Chaque action porte sa propre évaluation d'efficacité

- **Statut** : accepté
- **Date** : 2026-11-21
- **Portée** : actions d'un dossier CAPA, fenêtre d'une action, ouverture d'un
  dossier, traitement 8D (D3, D5, D7)
- **Révise** : l'ADR 0130 sur son seul point 5

## Contexte

Le critère d'efficacité (ADR 0108) et l'exigence de vérification (ADR 0073)
appartenaient au **dossier**, et ne se posaient **qu'une fois**, à son
ouverture. L'ADR 0130 s'en est tenu là : il a étendu la demande à la nature
préventive et rappelé en lecture ce qui était déjà décidé, mais la question
restait posée une seule fois par dossier.

Un dossier porte plusieurs actions, de natures différentes (ADR 0115). Trois
conséquences, relevées en recette :

1. **La deuxième action d'un dossier, et toutes les suivantes, s'écrivaient sans
   que la question soit posée.** Comme la réponse est donnée à l'ouverture, elle
   était « déjà tranchée » dès la première action : aucune autre ne la voyait.
2. **Une seule mesure pour des gestes qui ne se mesurent pas pareil.**
   « Recalibrer la presse » se juge au taux de rebut ; « former les opérateurs
   de nuit » se juge à l'absence de récidive sur l'équipe formée. Un critère
   unique en couvre un et manque l'autre.
3. **Un seul vérificateur pour tout le dossier.** Celui qui relit les relevés de
   la presse n'est pas celui qui atteste qu'une formation a eu lieu.

## Décision

**L'action porte son critère d'efficacité et sa décision de vérification. Le
dossier garde les siens.** Les deux coexistent.

Cinq colonnes sur `capa_actions` (migration `V177`) :
`effectiveness_criterion`, `verification_required`, `verification_assignee_id`,
`verification_assignee_name`, `verification_instructions`. Toutes nullables,
sans valeur par défaut : `NULL` veut dire « la question n'a pas été tranchée
pour cette action », ce qui est l'état exact des lignes existantes et n'est pas
« non ».

**Ce qui fait foi, et où.**

| | Le dossier | L'action |
| --- | --- | --- |
| Pose la question | à l'ouverture | à chaque écriture d'action |
| Gouverne la **clôture** du dossier | **oui** (ADR 0073) | non |
| Porte le **verdict** d'efficacité | **oui** (ADR 0108) | non |
| Dit ce qu'on attend de **ce geste-là** | non | **oui** |

La clôture et le verdict restent inchangés : ils s'appuient sur le dossier. Rien
dans ce lot ne crée de nouveau verrou de clôture.

**Les règles, identiques à celles du dossier.**

- Un **endiguement** se voit refuser critère et vérification : il arrête l'effet,
  il ne prétend pas supprimer la cause (ISO 9001 §10.2), donc il n'y a rien à
  mesurer sur lui. Refusé plutôt qu'accepté-puis-ignoré : un champ accepté donne
  à croire qu'une mesure a été promise.
- Désigner un vérificateur ou écrire des consignes suppose la vérification
  **exigée**.
- Une vérification exigée doit être **confiée à quelqu'un** : sans nom, elle
  n'aurait lieu que si quelqu'un y pense.
- Un « **non** » efface le vérificateur et les consignes, au lieu d'être refusé —
  c'est exactement ce que fait la même décision sur le dossier, et deux règles
  divergentes sur la même question se liraient comme un bogue.
- Le critère d'une action se **verrouille** dès qu'il est posé (ADR 0108). Le
  même texte renvoyé à l'identique passe : un PATCH qui n'y touche pas n'a pas à
  être refusé.

**La première action recopie ce que l'ouverture vient de poser.** La question n'a
été posée qu'une fois, en écrivant cette action-là : elle serait sinon la seule
du dossier à ne rien dire de son effet, alors qu'elle est précisément celle
devant laquelle on a répondu. Rien sous un premier endiguement.

**À l'écran.** La fenêtre d'une action demande le critère et la question à
chaque ajout d'une action corrective ou préventive, et affiche **au-dessus**, en
lecture, ce que le dossier attend déjà : on écrit mieux la seconde en voyant la
première. À l'édition, c'est ce que **cette** action a répondu qui se rappelle,
et rien ne se redemande. L'envoi se fait en **une seule écriture**, avec
l'action — avant, c'étaient deux appels enchaînés sur le dossier, et une panne
entre les deux laissait un critère posé sans l'action qu'il mesure.

## Justification

L'alternative examinée était de **déduire** la décision du dossier de celles de
ses actions : plus de décision propre, le dossier exigeant une vérification dès
qu'une de ses actions l'exige. C'est plus simple à lire, mais cela demandait de
reprendre le verdict d'efficacité (ADR 0108), l'écran d'évaluation et le verrou
de clôture (ADR 0073) dans le même lot, et de répondre à une question qui n'est
pas posée : que vérifie-t-on au juste quand on clôt un dossier dont deux actions
sur cinq ont été vérifiées ? Faire coexister les deux livre ce qui était demandé
— la question posée sur chaque action — sans déplacer ce sur quoi s'appuie la
clôture.

Le prix de ce choix est nommé plus bas : la même question reçoit deux réponses
sur un même dossier. C'est tenable parce que les deux ne répondent pas à la même
chose — l'une engage le dossier, l'autre décrit un geste — et parce que l'écran
les montre côte à côte au lieu de les confondre.

## Conséquences

- ✅ La question de la vérification est posée à **chaque** action corrective ou
  préventive, quelle que soit la discipline qui l'écrit (D5, D7) et quel que
  soit le rang de l'action dans son dossier.
- ✅ Deux actions d'un même dossier peuvent avoir leur propre mesure et leur
  propre vérificateur.
- ✅ Une seule écriture : plus de fenêtre où une panne laisse le critère posé
  sans son action.
- ✅ Un endiguement ne se fait rien demander, et ne peut rien porter.
- ⚠ **La même question reçoit deux réponses sur un même dossier** — la sienne et
  celle de chacune de ses actions. C'est voulu (voir *Justification*), mais c'est
  à garder en tête : la clôture ne lit que celle du dossier. Un écran qui
  afficherait les deux comme une seule décision serait un défaut.
- ✅ **Verrou de clôture sur les vérifications d'action** — ajouté par
  l'**ADR 0132**, qui solde la dette que ce lot avait laissée : une action qui a
  promis une vérification n'est close que vérifiée, et le dossier ne se clôt pas
  avant elle.
- ⚠ Les actions antérieures à la migration portent `NULL` partout : ni critère,
  ni décision. Rien n'est repris — recopier celle du dossier affirmerait une
  décision prise action par action, que personne n'a prise.

## Tests d'invariant

- `CapaServiceTest` : une action porte son critère et sa vérification, le
  dossier garde les siens intacts ; un endiguement refuse l'un comme l'autre ;
  une vérification exigée sans vérificateur est refusée ; un vérificateur sans
  réponse à la question est refusé ; un « non » efface le vérificateur ; le
  critère d'une action ne se réécrit pas, mais le même texte passe ; l'ouverture
  recopie l'efficacité sur sa première action, et rien sur un premier
  endiguement.
- `capa-action-dialog.component.spec.ts` : la demande est posée même quand le
  dossier a déjà tranché, et ce qu'il attend est rappelé à côté ; l'envoi part
  avec l'action en une seule écriture, jamais sur le dossier ; rien n'est
  demandé sous un endiguement, à l'édition, ni en lecture seule ; à l'édition,
  c'est ce que cette action a répondu qui se rappelle.

## Références

- CLAUDE.md §4 (CAPA), §18.2 #11
- ADR 0073 (vérification d'efficacité), ADR 0108 (critère posé à l'avance)
- ADR 0088 (une CAPA naît avec son action), ADR 0115 (plusieurs actions par
  dossier), ADR 0117 (le statut du dossier se déduit de ses actions)
- ADR 0130 (une action tient sa nature de la discipline qui l'écrit) — révisé
  sur son point 5
- ISO 9001 §10.2 ; méthode 8D (D3, D5, D7)
