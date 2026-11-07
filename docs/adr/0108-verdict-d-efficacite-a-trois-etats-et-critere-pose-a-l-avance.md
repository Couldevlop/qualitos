# ADR 0108 — Verdict d'efficacité à trois états, et critère posé à l'avance

- **Statut** : Accepté
- **Date** : 2026-11-06
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`capa` : `CapaCase`, `CapaService`,
  `CapaTransition`, `CapaDto` ; `capa.effectiveness` : domaine
  `EffectivenessVerdict`, `EffectivenessReview`, application
  `EffectivenessReviewService`, infrastructure et web), migration
  `V156`, écran `/capa/efficacite`
- **Complète** : ADR 0096 — non ; **complète la mesure d'efficacité par
  récidive** déjà en place dans `capa.effectiveness` (taux mesuré avant/après
  clôture), à laquelle elle ajoute le versant déclaré.

## Contexte

L'ISO 9001 §10.2.1 d) demande d'« examiner l'efficacité de toute action
corrective menée ». La plateforme le faisait de deux façons, et il manquait la
jonction entre les deux.

**Ce qui existait.** Le dossier CAPA porte une vérification d'efficacité
(`PATCH /api/v1/capa/cases/{id}/effectiveness`), déjà sérieusement verrouillée :
statut `RESOLVED`, toutes les actions `DONE`, refus d'un dossier qui n'a fait
qu'endiguer, refus tant qu'une non-conformité rattachée reste ouverte. Elle
pilote la machine à états — `CLOSED` si l'efficacité est démontrée, retour en
`IN_PROGRESS` sinon — et chaque passage s'inscrit au journal chaîné du client.
En parallèle, un second écran mesure l'efficacité **après coup**, en comptant
les récidives sur deux fenêtres de même durée de part et d'autre de la clôture.

**Le défaut.** Cette vérification est un **booléen**. Trois conséquences, toutes
observées sur le terrain :

1. **Le milieu n'existe pas.** Une action qui fait passer le rebut de 4,2 % à
   2,5 % pour une cible de 2 % n'est ni efficace ni inefficace. Le booléen
   oblige à mentir dans un sens ou dans l'autre. Répondre « non » rouvre un
   dossier qui avançait et efface le progrès du registre ; répondre « oui »
   clôt au-dessus d'un objectif non atteint — et c'est la seconde réponse que
   l'on choisit, parce qu'elle fait avancer le tableau de bord.
2. **Le critère n'est nulle part.** Rien ne portait la phrase « zéro erreur de
   référence sur 90 jours ». Le critère se décidait donc au moment de juger,
   par la personne qui avait mené l'action, en regardant le résultat obtenu.
   Un critère écrit après la mesure n'est pas un critère, c'est un commentaire.
3. **Le jugement ne laissait aucune trace exploitable.** On savait *qu'une*
   vérification avait eu lieu et *quand*, jamais sur quoi elle s'appuyait. Un
   auditeur qui demande « sur quelle base avez-vous conclu à l'efficacité ? »
   n'avait rien à lire.

## Décision

### 1. Le verdict passe à trois états

`EffectivenessVerdict` : `EFFECTIVE`, `PARTIAL`, `INEFFECTIVE`. Chaque état
**porte sa conséquence** sur le dossier, dans l'énumération elle-même plutôt que
dans une suite de `if` du service :

| Verdict | Dossier | Transition journalisée | Ce que ça veut dire |
| --- | --- | --- | --- |
| `EFFECTIVE` | `CLOSED` | `CLOSED` | le critère est atteint, le dossier se clôt |
| `PARTIAL` | `IN_PROGRESS` | `EFFECTIVENESS_PARTIAL` | effet réel mais critère non atteint : une action complémentaire est attendue |
| `INEFFECTIVE` | `IN_PROGRESS` | `EFFECTIVENESS_REJECTED` | aucun effet : la cause a été mal identifiée, l'analyse est à reprendre |

`PARTIAL` et `INEFFECTIVE` ramènent tous deux le dossier en traitement, et ce
n'est pas une raison de les confondre. « Partiellement efficace » dit : la cause
était la bonne, l'action ne va pas assez loin — on en ajoute une. « Inefficace »
dit : la cause n'était pas celle-là — on reprend l'analyse (5 Pourquoi,
Ishikawa). Ce sont deux travaux différents, confiés à deux moments différents du
raisonnement, et l'écran le dit en toutes lettres à chaque verdict.

Les trois garde-fous de la clôture (actions faites, pas d'endiguement seul, pas
de non-conformité rattachée encore ouverte) ne s'appliquent **qu'à**
`EFFECTIVE`, comme aujourd'hui : ils protègent la clôture, et rien d'autre.

Ce point mérite d'être dit, parce que la maquette montre un verrou plus serré et
que les deux coexistent : **l'écran** n'ouvre pas le formulaire tant que les
actions ne sont pas menées à terme — juger l'effet d'une action qu'on n'a pas
faite n'a aucun sens, et la personne qui arrive sur ce dossier a mieux à faire
que de rendre un verdict prématuré. **L'API**, elle, laisse passer un verdict
`PARTIAL` ou `INEFFECTIVE` sur un dossier aux actions inachevées. Deux raisons :
consigner un échec est un constat de fait, et l'interdire empêcherait d'écrire
que l'action n'a rien produit ; et c'est le comportement qu'elle a déjà, utilisé
par les intégrations qui remontent un refus d'efficacité depuis un ERP. On ne
casse pas un contrat public (§13.1) pour durcir une règle que l'écran tient
déjà.

### 2. Le critère est posé à l'avance, sur le dossier

`capa_cases.effectiveness_criterion` : la phrase qui dit à quoi on reconnaîtra
l'efficacité. Elle se saisit à l'ouverture ou pendant le traitement, et
**l'évaluation la recopie** dans son enregistrement.

La recopie n'est pas de la redondance. Modifier le critère d'un dossier ne doit
pas réécrire le critère sur lequel une évaluation passée s'est prononcée — c'est
la même raison qui fait recopier `verification_assignee_name` depuis l'annuaire :
un dossier CAPA se relit des années plus tard, souvent en audit, et une trace
qui change après coup ne vaut rien.

### 3. Les évaluations forment un historique, pas un champ

`capa_effectiveness_reviews` : une ligne par évaluation, avec le critère recopié,
les trois relevés (avant / cible / constaté), le verdict, le commentaire, et qui
a jugé — nom recopié depuis l'annuaire.

**Pourquoi un historique et non trois colonnes de plus sur le dossier.** Un
dossier jugé inefficace repart en traitement, reçoit une action, et se fait
réévaluer. La suite des verdicts — *inefficace, puis partiel, puis efficace* —
est précisément ce qu'un auditeur lit : elle montre un système qui corrige. Un
champ écrasé ne montre que la fin, et donne du dossier une image de réussite
immédiate qui est fausse.

Les relevés sont du **texte court**, pas des nombres. « 4,2 % », « ≤ 2 g »,
« 0 retard sur 620 expéditions » : l'unité, la tolérance et l'assiette font
partie du relevé, et les séparer en nombre + unité obligerait à inventer un
référentiel d'unités que chaque secteur contredirait. Aucun indicateur n'est
calculé à partir de ces trois champs — ils se lisent, ils ne s'agrègent pas
(CLAUDE.md §18.2 #8 : aucun KPI affiché sans formule explicite ; ici, c'est le
*verdict* qui alimente les indicateurs, et lui est énuméré).

### 4. Le booléen existant devient une projection

`effectiveness_verified` reste, et devient la projection du verdict :
`EFFECTIVE → true`, `PARTIAL` et `INEFFECTIVE → false`. Il n'est plus la source
de vérité, et il n'est plus écrit à la main.

Il n'est pas supprimé pour une raison concrète : l'écran d'efficacité **mesurée**
l'affiche à côté du taux de récidive, et c'est l'écart entre les deux qui fait
tout l'intérêt de cet écran. Ce même écran gagne le verdict déclaré, plus
précis — le booléen reste pour les dossiers antérieurs à cet ADR, qui n'ont pas
de verdict et n'en auront jamais.

### 5. Compatibilité de l'API

`PATCH /api/v1/capa/cases/{id}/effectiveness` accepte désormais `verdict`, et
continue d'accepter `effective` — `true → EFFECTIVE`, `false → INEFFECTIVE`.
Exactement l'un des deux doit être fourni ; les deux ensemble, ou aucun, sont
refusés en 400.

L'ancien champ n'est pas retiré parce que l'API est publique et versionnée
(§13.1) : un intégrateur qui envoie `{"effective": true}` depuis un ERP ne doit
pas découvrir la rupture en production. Il est documenté comme déprécié dans
l'OpenAPI, et il ne permet pas d'exprimer `PARTIAL` — ce qui est la bonne
incitation à passer à `verdict`.

## Conséquences

**Ce qu'on gagne.** Le milieu existe : une action qui progresse sans atteindre
sa cible se dit telle quelle, sans rouvrir le dossier ni mentir au tableau de
bord. Le critère précède la mesure. Le jugement laisse une trace lisible en
audit. Et les deux versants de l'efficacité — ce qu'on a déclaré, ce que le
terrain a fait — se lisent sur le même écran.

**Ce que ça coûte.** Une table de plus, une colonne de plus sur `capa_cases`, un
état de plus dans la machine à états du dossier, et un champ d'API déprécié à
porter. Un critère est désormais **exigé** pour rendre un verdict : un dossier
ouvert qui n'en porte pas doit en recevoir un avant d'être jugé, et l'écran le
signale tant qu'il est encore temps de le poser. Les appels à l'ancienne forme
(`effective`) en sont dispensés — ils inscrivent une mention explicite à la
place — faute de quoi toute intégration existante serait tombée en 400 du jour
au lendemain.

Enfin, l'écran et l'API ne portent pas exactement le même verrou sur les actions
inachevées (cf. décision 1). C'est une asymétrie assumée, pas un oubli : elle est
le prix de la compatibilité, et elle va dans le sens le plus sûr — l'interface
que les gens utilisent est la plus stricte des deux.

**Ce qu'on ne fait pas.** Aucune création automatique d'action complémentaire sur
un verdict `PARTIAL`. L'écran la réclame, la nomme, et laisse la personne la
définir : une action engendrée par la machine n'a ni responsable ni échéance
choisis, et un dossier CAPA n'a pas besoin d'une ligne que personne n'a voulue.
