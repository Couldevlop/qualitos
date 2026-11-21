# ADR 0130 — Une action tient sa nature de la discipline qui l'écrit

- **Statut** : accepté
- **Date** : 2026-11-21
- **Portée** : traitement 8D de la fiche d'écart (D3, D5, D7), rapport 8D,
  fenêtre d'ouverture d'une CAPA, fenêtre d'une action, registre des sources

## Contexte

Le traitement 8D d'un écart nomme trois gestes différents, chacun dans sa
discipline : **D3** endigue (on arrête l'effet), **D5** corrige (on supprime la
cause de l'écart survenu), **D7** prévient (on supprime la cause de celui qui
n'est pas encore survenu). La nature de l'action — `CONTAINMENT`, `CORRECTIVE`,
`PREVENTIVE` — est ce qui relie l'action à sa discipline : chaque tableau ne
montre que les actions de sa nature.

Cinq défauts relevés en recette, tous sur ce même fil.

1. **La première action d'un écart naissait sous une fausse nature.** Le D3 et
   le D7 n'avaient pas de bouton : leur texte renvoyait au D5 pour « ouvrir le
   dossier ». On y descendait, on cliquait « Nouvelle action », et la fenêtre
   d'ouverture reposait la question de la nature en trois choix avec
   « Corrective » **pré-cochée**. L'action de confinement qu'on venait d'écrire
   naissait donc corrective : le registre CAPA l'affichait sous ce faux type, et
   le D3 restait vide puisqu'il ne montre que l'endiguement.

2. **« Détection interne » rendait le dossier orphelin.** C'était la source
   proposée **par défaut** dans la fenêtre « Nouvelle CAPA », et elle n'adosse
   le dossier à aucun registre : sa référence se tape à la main. Or la fiche
   d'un écart retrouve ses dossiers par le couple *source + référence*
   (`CapaService.dossiersDe`, ADR 0123). Un dossier ouvert depuis le registre
   CAPA sous cette source, en citant « Int-NC-2026-0046 », était donc invisible
   au D3, au D5, au D7 **et** dans le rapport 8D de cet écart — alors que ses
   actions retenaient sa clôture.

3. **La prévention se lisait deux fois dans le rapport.** Le D5 du rapport
   montrait `CORRECTIVE` **et** `PREVENTIVE`, à l'écran comme dans le PDF, alors
   que le D7 montre déjà `PREVENTIVE`. Chaque action préventive paraissait donc
   sous deux disciplines du même document, comme si l'écart avait été traité deux
   fois. La fiche de l'écart, elle, faisait déjà la séparation : l'écran et le
   rapport ne décrivaient pas le même dossier.

4. **L'intitulé du dossier coiffait les trois tableaux d'actions.** Dès qu'un
   écart portait deux dossiers, le titre du dossier s'écrivait au-dessus de
   chacun des tableaux D3, D5 et D7. Comme le titre d'un dossier est celui de sa
   première action (ADR 0088), on lisait « Action containment 1 » en guise de
   titre de section, juste au-dessus d'un tableau qui reprend cette même action
   sur sa première ligne.

5. **L'évaluation d'efficacité ne se demandait qu'à la corrective, et jamais
   deux fois.** Le critère (ADR 0108) et l'exigence de vérification (ADR 0073)
   appartiennent au **dossier** et ne se posent qu'une fois. Deux conséquences :
   le D7 ne demandait **aucun** critère (la demande était restreinte à
   `CORRECTIVE`), et une action écrite après l'ouverture n'en voyait plus
   **rien** — ni demande, ni rappel. On écrivait donc une action corrective sans
   savoir si son effet serait vérifié, et la plateforme paraissait ne le demander
   nulle part. À l'inverse, l'endiguement se faisait demander un critère
   d'efficacité : on faisait promettre un résultat à un geste qui n'en poursuit
   aucun.

## Décision

**1. La nature vient de la discipline, et ne se repose pas.** Le D3, le D5 et le
D7 ont chacun leur bouton, qui **nomme** ce qu'il ajoute (« Ajouter une action de
confinement / corrective / préventive »). La fenêtre qu'il ouvre ne propose plus
que cette nature-là, cochée :

- sans dossier, le bouton ouvre la fenêtre de création avec `lockType: true` et
  `prefill.type` = la nature de la discipline ;
- avec dossier, le tableau passe déjà `actionType` à la fenêtre d'une action
  (ADR 0115) ; celle-ci réduit désormais ses choix à cette seule nature.

À l'**édition** d'une action, les trois natures restent : corriger une nature mal
choisie est précisément ce qu'on vient faire.

**2. « Détection interne » n'est plus proposée à la création.** Elle reste dans
l'énumération — les dossiers déjà ouverts la portent, et leur libellé ne change
pas — mais sort de la liste de la fenêtre (`manual: false`). Citer un écart
passe désormais par une source adossée à un registre, où la référence se
**choisit** : un dossier ne peut plus naître en citant un écart sans lui être
rattaché.

Le défaut de la fenêtre devient « Autre », et non l'écart : un défaut adossé à un
registre rendrait la référence **obligatoire à chaque ouverture**, y compris pour
un dossier qui ne traite ni écart, ni constat, ni risque. C'est ce que
« détection interne » permettait, et c'est légitime ; ce qui ne l'était pas,
c'est de l'offrir à qui venait citer un écart.

**3. Le D5 ne montre que `CORRECTIVE`.** À l'écran et dans l'instantané du
document (`EightDSnapshotAssembler.NATURES_D5`). Le D3 garde `CONTAINMENT`, le
D7 `PREVENTIVE` : une action, une discipline.

**4. Les tableaux d'actions ne sont plus coiffés de l'intitulé du dossier.** Le
D5 garde le lien « Voir la CAPA », qui mène au dossier ; le document émis, lui,
nomme chaque tableau, parce qu'il n'a pas de lien à offrir.

**5. L'évaluation d'efficacité suit ce que l'action PROMET.**

- Une action qui prétend supprimer la cause — `CORRECTIVE` **ou** `PREVENTIVE` —
  se voit demander le critère tant que le dossier n'en a pas, et la question de
  la vérification tant qu'elle n'a pas été tranchée.
- Quand l'un ou l'autre est déjà réglé, il est **rappelé en lecture** au-dessus
  du formulaire, au lieu d'être passé sous silence.
- Un `CONTAINMENT` ne se voit rien demander et rien rappeler : ni critère, ni
  vérification. La question se reposera au premier correctif, et le serveur
  refuse de toute façon de clore un dossier qui n'endigue que
  (`CONTAINMENT_ONLY`).

## Justification

La nature n'est pas une préférence de saisie : c'est la clé qui range l'action
sous sa discipline et qui, dans le document remis au client, sépare ce qui a été
fait *tout de suite* de ce qui supprime la cause (ISO 9001 §10.2, 8D). La poser
en question ouverte sous un titre qui porte déjà la réponse, avec une valeur
pré-cochée, garantissait l'erreur à la première main qui dérape — et l'erreur
n'est pas rattrapable à la lecture : elle fait disparaître l'action de la
discipline où on la cherche.

L'alternative examinée pour le point 5 était un **critère par action** :
chaque action corrective ou préventive porterait sa propre mesure et sa propre
exigence de vérification. Écartée pour ce lot : elle demande une migration, une
reprise du verdict d'efficacité du dossier (ADR 0108) et de l'écran
d'évaluation, et elle rouvre la question de savoir ce que vérifie une clôture
de dossier. La demande exprimée — « que le D5 et le D7 demandent l'évaluation
d'efficacité » — est satisfaite sans ce changement de modèle, puisque la
question est désormais posée dans les deux disciplines quand elle est ouverte,
et montrée quand elle est fermée.

Le retrait de « Détection interne » se préfère à un rattachement devinée depuis
une référence tapée : deviner, c'est accepter de se tromper sur la cause d'un
dossier, et une référence mal recopiée se lirait alors comme un écart qui
n'existe pas.

## Conséquences

- ✅ La nature d'une action ne peut plus être fausse par défaut : elle est celle
  de la discipline d'où elle est écrite.
- ✅ Le registre CAPA affiche la bonne nature, qu'il lit sur les actions du
  dossier et non sur son type déclaré.
- ✅ Un dossier ouvert depuis le registre CAPA et rattaché à un écart tombe dans
  la discipline de sa nature, sur la fiche de cet écart comme dans son 8D.
- ✅ Une action préventive ne se lit plus qu'une fois dans le rapport.
- ⚠ **La source par défaut de la fenêtre « Nouvelle CAPA » devient « Autre ».**
  Elle ne désigne aucun objet : un dossier qui traite un écart demande donc de
  choisir sa source, et le choisir n'est plus une option mais le seul chemin
  pour que l'écart le voie.
- ⚠ **Les dossiers déjà ouverts sous « Détection interne » restent orphelins**
  de leur écart. Rien n'est repris : leur source est une donnée, et la réécrire
  en masse supposerait de deviner l'écart visé. Ils se rattachent en corrigeant
  leur source dossier par dossier.
- ⚠ Quand un écart porte **plusieurs** dossiers, les tableaux d'actions ne
  disent plus lequel porte quelle ligne. Le lien « Voir la CAPA » du D5 et le
  document émis le disent ; la fiche ne le dit plus.
- ⚠ Le critère d'efficacité reste porté par le **dossier** — **révisé par
  l'ADR 0131**, qui donne à chaque action le sien et pose la question à chaque
  écriture. Le dossier garde les siens, qui gouvernent sa clôture.

## Tests d'invariant

- `capa-create-dialog.component.spec.ts` : la nature imposée n'offre qu'un
  bouton radio, coché ; un endiguement n'envoie ni `verificationRequired` ni
  `effectivenessCriterion` et ouvre quand même un dossier `CORRECTIVE` ;
  « Détection interne » ne figure plus dans les sources proposées.
- `capa-create-dialog.source-ref.spec.ts` : la source par défaut est « Autre »,
  dont la référence reste libre et facultative ; « Détection interne » ne figure
  plus dans les sources proposées.
- `capa-action-dialog.component.spec.ts` : une seule nature à l'ajout, trois à
  l'édition ; critère et vérification demandés au préventif, aucun des deux à
  l'endiguement (la demande elle-même est reprise par l'ADR 0131, qui la pose à
  chaque action).
- `capa-action-panel.component.spec.ts` : le bouton d'ajout nomme la nature du
  tableau.
- `nc-detail.dossiers.spec.ts` : chaque discipline ouvre le dossier avec SA
  nature ; aucun intitulé de dossier au-dessus des tableaux D3, D5, D7.
- `nc-eightd.component.spec.ts` : D3 = `CONTAINMENT`, D5 = `CORRECTIVE`,
  D7 = `PREVENTIVE`.
- `EightDSnapshotAssemblerTest` : la préventive est au D7 et nulle part ailleurs.

## Références

- CLAUDE.md §3.6 (référentiel transverse), §4 (CAPA), §18.2 #11
- ADR 0088 (une CAPA naît avec son action), ADR 0115 (plusieurs actions par
  dossier), ADR 0117 (le statut du dossier se déduit de ses actions)
- ADR 0073 (vérification d'efficacité), ADR 0108 (critère posé à l'avance)
- ADR 0123 (un écart a plusieurs dossiers), ADR 0124 (le PDF se présente comme
  l'écran), ADR 0129 (le rapport parle français, la prévention est au D7)
- ISO 9001 §10.2 ; méthode 8D (D3, D5, D7)
