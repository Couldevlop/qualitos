# ADR 0132 — Une action n'est close que vérifiée

- **Statut** : accepté
- **Date** : 2026-11-21
- **Portée** : clôture d'un dossier CAPA, historique des évaluations
  d'efficacité, tableau des actions, rapport 8D
- **Complète** : l'ADR 0131 (chaque action porte sa propre évaluation
  d'efficacité), dont il solde la dette

## Contexte

L'ADR 0131 a donné à chaque action son critère d'efficacité et sa propre
exigence de vérification. Il a aussi laissé une dette, nommée dans ses
conséquences : **aucun verrou de clôture sur ces vérifications**. Un dossier se
clôturait donc ainsi :

1. chaque action est marquée **faite** (`ACTIONS_NOT_DONE` satisfait) ;
2. le dossier reçoit **un** verdict d'efficacité (ADR 0073, 0108) ;
3. il se clôt.

Les vérifications promises **action par action** n'entraient nulle part. Une
action pouvait annoncer « l'effet de ce geste sera vérifié par X », être marquée
faite, et le dossier se clore sans que personne n'ait regardé. Le registre
portait alors un dossier clos pour une cause déclarée traitée **sur la foi d'une
promesse que personne n'était allé contrôler** — et c'est précisément ce
contrôle qu'un auditeur vient chercher (ISO 9001 §10.2).

Une action « faite » dit que **le geste a eu lieu**. Elle ne dit pas qu'il a
**produit son effet**. Les deux se confondaient.

## Décision

**Toutes les actions d'un dossier doivent être closes avant lui, et une action
qui a promis une vérification n'est close que vérifiée.**

**1. Le jugement d'une action vit dans l'historique des évaluations.** La table
`capa_effectiveness_reviews` gagne une colonne `capa_action_id` (migration
`V178`) : nulle, la ligne juge le **dossier** — ce qu'elle a toujours fait ;
renseignée, elle juge **l'action** citée.

Dans la table existante, et non dans des colonnes sur l'action : un jugement est
une ligne d'historique **immuable** (ADR 0108). La suite des verdicts —
inefficace, puis partiel, puis efficace — est exactement ce qu'un auditeur lit :
elle montre un système qui corrige. Un champ écrasé sur l'action ne montrerait
que la fin, et donnerait de l'action une image de réussite immédiate qui est
fausse. Deux tables, elles, auraient divergé à la première colonne ajoutée d'un
seul côté.

**2. Un nouvel obstacle à la clôture : `ACTION_VERIFICATIONS_PENDING`.** Le
décompte est le nombre d'actions qui ont promis une vérification et ne l'ont pas
obtenue. Trois cas comptent :

| État de l'action | Compte comme en attente ? |
| --- | --- |
| N'a rien promis (`verificationRequired` nul ou faux) | non — son statut `DONE` suffit |
| A promis, jamais jugée | **oui** |
| A promis, dernier verdict `PARTIAL` | **oui** |
| A promis, dernier verdict `INEFFECTIVE` | **oui** |
| A promis, dernier verdict `EFFECTIVE` | non |

`PARTIAL` et `INEFFECTIVE` retiennent la clôture : l'action **a** été regardée,
et ce qu'on a vu ne permet pas de clore. C'est exactement ce que le verdict du
dossier dit déjà de lui-même (ADR 0108), à l'échelle de l'action.

**3. L'obstacle est annoncé ET tenu.** La fiche du dossier le liste avant le clic
(`closureBlockers`), et `verifyEffectiveness` le refuse au moment de clore. Une
liste d'obstacles qui n'engage pas le serveur ne vaut rien ; un serveur qui
refuse ce que l'écran n'annonçait pas ne vaut pas mieux.

**4. Le geste de jugement vit sur la ligne de l'action.**
`PATCH /capa/cases/{id}/actions/{actionId}/effectiveness`, même permission
(`CAPA_VERIFY`) que le jugement du dossier : c'est le même geste, à l'échelle où
la promesse a été faite. Un droit distinct laisserait vérifier le dossier sans
pouvoir vérifier ses actions, alors que le premier dépend désormais des secondes.

Côté écran, le bouton « Vérifier l'efficacité » apparaît dans le tableau des
actions, sur la ligne qu'on vient de terminer — et non sur un écran séparé qu'il
faudrait penser à visiter, pendant que le dossier attend. Une fois le verdict
`EFFECTIVE` rendu, la ligne porte « Efficacité vérifiée », avec qui a jugé et
quand en infobulle.

**5. Quatre refus au jugement d'une action.**

- Une action qui **n'a rien promis** ne se juge pas : il n'y a pas de critère
  auquel confronter un relevé, et juger sans critère annoncé revient à choisir le
  critère en regardant le résultat (ADR 0108).
- Une action **pas encore faite** ne se juge pas : on mesurerait l'effet d'un
  geste qui n'a pas eu lieu.
- Un dossier **clos ou rejeté** ne reçoit plus de jugement : ce qui est clos l'a
  été sur l'état d'alors.
- **Sans critère** — ni sur l'action, ni sur le dossier —, le jugement est
  refusé. L'écran le dit avant le clic : il faut reprendre l'action pour y
  inscrire sa mesure.

Le critère retenu est celui de l'**action** ; à défaut, celui du **dossier**,
pour les actions antérieures aux colonnes de la `V177`.

## Justification

L'alternative était de porter le verdict d'une action dans des colonnes sur
`capa_actions` — plus court à écrire, et une requête de moins. Écartée : un
verdict qu'on peut écraser ne prouve rien, et c'est exactement ce qu'un auditeur
vient contrôler. Le cas normal est d'ailleurs la **suite** des verdicts : une
action jugée insuffisante, reprise, puis jugée efficace laisse trois lignes, et
ces trois lignes sont la démonstration que le système fonctionne.

Le choix de faire compter `PARTIAL` comme en attente mérite d'être explicité :
on pourrait soutenir qu'un effet partiel est un effet, et laisser clore. Mais
« partiellement efficace » veut dire qu'une action complémentaire est attendue
(c'est ce que l'écran annonce déjà au niveau du dossier) : clore là-dessus
reviendrait à décider que le reste ne sera jamais fait.

Enfin, le verrou ne s'applique qu'**à la clôture**, et non à la résolution : un
dossier doit pouvoir être résolu — toutes ses actions menées — pendant que les
vérifications courent. Une vérification d'efficacité se mesure sur des semaines ;
exiger qu'elle précède la résolution bloquerait le dossier à l'étape d'avant, et
personne ne saurait plus où il en est.

## Conséquences

- ✅ Un dossier ne se clôt plus au-dessus d'une promesse de vérification non
  tenue. L'obstacle est chiffré, annoncé avant le clic, et refusé par le serveur.
- ✅ Le jugement d'une action se rend d'un clic sur sa ligne, par la personne qui
  en a le droit, avec le critère posé à l'avance sous les yeux.
- ✅ L'historique d'une action est lisible : la suite de ses verdicts montre une
  reprise, là où un champ écrasé n'aurait montré que la fin.
- ⚠ **Un dossier dont une action a promis une vérification ne peut plus se clore
  sans elle.** C'est l'objet de cette décision, mais c'est un changement de
  comportement : des dossiers aujourd'hui clôturables ne le seront plus tant que
  leurs actions n'auront pas été jugées. Aucune donnée n'est reprise — les
  actions antérieures à la `V177` ont `verification_required` nul, donc n'ont
  rien promis et ne retiennent rien.
- ⚠ **La résolution n'est pas concernée.** Un dossier résolu peut rester en
  attente de vérifications pendant des semaines. C'est le déroulé réel d'une
  vérification d'efficacité, pas un oubli.
- ⚠ Une action supprimée emporte ses évaluations (`ON DELETE CASCADE`). Le refus
  de supprimer une action qui porte une preuve vit dans le service, où il peut
  s'expliquer ; la contrainte, elle, ne doit pas laisser de lignes orphelines.

## Tests d'invariant

- `CapaServiceTest` : le jugement d'une action s'écrit avec le critère de
  l'action, et retombe sur celui du dossier à défaut ; une action qui n'a rien
  promis, une action pas encore faite et l'absence de tout critère sont
  refusées ; la clôture est refusée tant qu'une action promise n'est pas jugée,
  et l'est encore sur un verdict `PARTIAL` ; elle passe dès que chaque action
  promise est jugée efficace ; la fiche annonce l'obstacle avec son décompte, et
  ignore les actions qui n'ont rien promis.
- `capa-action-verification-dialog.component.spec.ts` : le critère se lit et ne
  se saisit pas ; l'absence de tout critère est annoncée avant le clic et bloque
  l'envoi ; un verdict sans relevé ne passe pas ; les bornes vides ne partent
  pas ; un refus du serveur ne ferme pas la fenêtre.
- `capa-action-panel.component.spec.ts` : le bouton paraît sur une action faite
  qui a promis, pas sur les autres, et reste après un verdict insuffisant ;
  l'efficacité constatée s'affiche avec son auteur et sa date.
- Migrations `V177` et `V178` rejouées sur PostgreSQL réel par les bancs
  `*OnPostgresTest`.

## Références

- CLAUDE.md §4 (CAPA), §18.2 #5 (action critique auditable), #11
- ADR 0073 (vérification d'efficacité), ADR 0108 (critère posé à l'avance,
  historique immuable des verdicts)
- ADR 0115 (plusieurs actions par dossier), ADR 0117 (statut déduit des
  actions), ADR 0131 (chaque action porte son évaluation)
- ADR 0052 (le nom de l'auteur figé à la décision)
- ISO 9001 §10.2 ; méthode 8D (D5, D6, D7)
