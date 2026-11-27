# ADR 0143 — L'efficacité se juge une fois, depuis la discipline qui la porte

- **Statut** : Accepté
- **Date** : 2026-11-27
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`capa` : `CapaService.verifyActionEffectiveness`,
  `CapaController`, `CapaScope`, `CapaCaseRepository` ; `capa.effectiveness` :
  domaine `CaseSettlement` ; `nonconformity.eightd` :
  `EightDSnapshotAssembler`), écrans `/capa/efficacite` et la fiche d'un écart
  (ses huit disciplines)
- **Amende** : ADR 0131 (le verdict du dossier n'est plus seul à clore),
  ADR 0132 §4 (la garde du droit passe de l'intercepteur au service)
- **S'appuie sur** : ADR 0135 (une CAPA porte une action et une seule),
  ADR 0137 (la liste d'obstacles reste opposée), ADR 0139 (le rapport 8D montre
  ce que la plateforme sait)

## Contexte

Trois constats de recette, sur le même geste.

**1. On évaluait deux fois.** Une action corrective se juge depuis le D5 de son
écart, une préventive depuis le D7 : la ligne porte « Vérifier l'efficacité », on
confronte le relevé au critère, on rend le verdict. Le jugement s'inscrivait bien
— ADR 0132 — mais le dossier, lui, ne bougeait pas : il restait `RESOLVED`. L'écran
d'efficacité l'affichait donc « en attente d'évaluation » et redemandait une
**seconde** évaluation, du même geste, contre le même critère, pour le faire
passer en « clos ». La seconde ne pouvait rien apprendre de plus que la première.

C'est l'ADR 0131 qui avait tranché ainsi : le dossier porte son verdict, l'action
décrit un geste. Le raisonnement tenait quand un dossier portait plusieurs
actions — « que vérifie-t-on au juste quand on clôt un dossier dont deux actions
sur cinq ont été vérifiées ? ». **L'ADR 0135 a répondu à cette question en la
supprimant** : une CAPA porte une action, et une seule. Juger l'action, c'est
juger le dossier.

**2. La personne désignée ne pouvait pas juger.** Une vérification exigée doit
être confiée à quelqu'un (ADR 0131), choisi dans l'annuaire. Deux verrous la lui
refusaient ensuite :

- le droit `capa.verify` n'est livré qu'au pilotage et à l'auditeur. Désigné
  vérificateur, un utilisateur recevait 403 — l'intercepteur refuse avant même de
  savoir quelle action est visée, donc avant de pouvoir constater qu'il en est le
  vérificateur ;
- la portée du registre (ADR 0081) ne connaissait que le vérificateur **du
  dossier**. Depuis l'ADR 0131 la vérification se confie **action par action** :
  la personne désignée là ne voyait pas le dossier, ni sur la liste, ni sur son
  plan de travail d'évaluations, et sa fiche répondait 404.

**3. Le rapport 8D ne disait pas si cela avait servi.** Le D5 dit ce qui a été
décidé, le D6 ce qui a été mené et prouvé, le D7 ce qui empêche le retour. Aucune
des trois ne portait le verdict : il avait été retiré du D6 à la demande de la
recette, et le document se fermait sur une équipe remerciée pour un résultat
qu'il ne nommait pas.

## Décision

### 1. Le verdict d'une action clôt son dossier

Un verdict `EFFECTIVE` porté sur une action **clôt le dossier qui la porte**, dès
lors que le dossier est `RESOLVED` et que **plus rien ne s'oppose à la clôture**.

La condition est tenue par `CaseSettlement.settlesCase`, fonction pure du domaine
d'efficacité : verdict qui clôt, dossier résolu, liste d'obstacles vide. Les
obstacles sont ceux que `closureBlockers` énumère déjà, c'est-à-dire exactement
ceux que le verdict du dossier doit franchir (ADR 0137) : action inachevée, écart
rattaché encore ouvert, autre vérification promise qui n'a pas conclu, objectif
qualité pas revenu sous sa cible (ADR 0133). **Juger une action n'est donc pas une
porte dérobée** ; un obstacle qui demeure laisse le dossier `RESOLVED`, et l'écran
le nomme avant le clic comme il le faisait déjà.

Le dossier ainsi clos porte `effectiveness_verified` à vrai et son horodatage,
comme le ferait le verdict du dossier, et journalise `CLOSED` : la vérification a
bien eu lieu — à l'échelle de l'action. **Aucune ligne d'évaluation du dossier
n'est écrite** : le verdict existe, il est celui de l'action, et en recopier un
second donnerait à lire deux jugements là où une seule personne a jugé une seule
fois.

`PARTIAL` et `INEFFECTIVE` ne changent rien au dossier par ce chemin : ils
retiennent déjà sa clôture (ADR 0132) et le travail restant se décide sur
l'action.

### 2. La personne désignée juge, et nul autre qu'elle

La garde du jugement d'une action passe de l'intercepteur au **service**
(amendement de l'ADR 0132 §4). Elle accorde le geste à deux personnes, et à deux
seulement :

- celle qui porte `capa.verify` — comme avant, sans rien changer au catalogue ;
- **celle qui est désignée sur l'action** — à défaut, celle du dossier.

Le catalogue des droits n'est **pas** élargi : `tous()` aurait ouvert le jugement
à chacun, alors que c'est la désignation, et non le rôle, qui ouvre ce chemin.
L'appelant est lu dans le `sub` du jeton, jamais dans le corps (CLAUDE.md
§18.2 #2). Un appelant qui n'est ni l'un ni l'autre reçoit 403.

**La même règle aux deux échelles.** Le verdict du **dossier** — ce que l'écran
d'efficacité envoie — passe par la même garde : son vérificateur, et ceux que ses
actions désignent. Les secondes comptent parce que la question se tranche action
par action depuis l'ADR 0131 : un dossier dont seule l'action nomme un
vérificateur aurait sinon offert à cette personne l'écran, la ligne à juger, le
formulaire — puis un refus à l'envoi. La **clôture directe**, elle, garde son
`@RequiresPermission` : un dossier qui n'a promis aucune vérification ne désigne
personne, et il n'y a donc personne à qui ce chemin devrait s'ouvrir.

La portée du registre CAPA (ADR 0081) compte désormais le vérificateur d'une
**action** parmi ceux qu'un dossier concerne : `CapaScope.concerns` et les trois
requêtes qui portent le même prédicat en base — le registre paginé, le plan de
travail des évaluations, la vue d'une fiche. Les quatre ensemble, sans quoi un
écran montrerait ce qu'un autre refuse.

### 3. Le D8 porte l'efficacité des actions

La discipline qui clôt le traitement porte un tableau — « Efficacité des
actions » : l'action, sa nature, le verdict, le jour du jugement. Une ligne par
action **jugée**, et aucune pour celles qui ne l'ont pas été : ce qui reste à
juger se lit sur l'écran d'efficacité, dont c'est le métier, et une ligne « non
jugée » ferait de la discipline un relevé de manques.

**Le verdict du dossier y entre aussi, sur un dossier d'une seule action.** Un
jugement porté depuis l'écran d'efficacité est une évaluation **du dossier**, et
le rapport ne lisait que celles des actions : le verdict n'atteignait donc aucune
discipline, et le document taisait un jugement pourtant rendu. Sur un dossier
d'une action — le cas général depuis l'ADR 0135 —, c'est le **dernier** jugement
du dossier qui vaut, action comprise : prendre celui de l'action aurait affiché un
« partiellement » dépassé sous un « efficace » rendu depuis. Le repli s'arrête là :
sur un dossier ancien qui porte plusieurs actions, le verdict du dossier se
répéterait sur chaque ligne en prétendant juger chacune.

Des **données**, comme les autres tableaux du rapport : le rendu reste une
fonction pure de ce qu'on stocke, donc l'empreinte d'un rapport scellé reste
vérifiable (ADR 0071). Un rapport émis avant cette décision se relit tel qu'il a
été scellé, sans ce tableau.

Le drapeau `sourced` du D8 reste décidé par **le texte saisi seul** : il
gouverne la mention « rapport partiel » et la coche du sommaire, et un jugement
d'efficacité ne remplace pas le mot que l'équipe n'a pas écrit. La reconnaissance
standard reste donc servie aux mêmes conditions qu'avant.

L'écran de l'écart rend ce tableau sous son D8, **tel que le rapport le donne** —
colonnes comprises — au lieu de le recomposer : deux assemblages du même fait
finissent par diverger, et c'est un document signé qui en porterait la
contradiction.

## Justification

L'alternative au point 1 était de laisser l'écran d'efficacité **masquer** la
ligne dès qu'une action avait conclu, sans rien clore. Elle supprimait la
deuxième demande d'évaluation sans supprimer le travail qu'elle représentait : le
dossier restait ouvert au registre, et sa clôture devenait un geste que plus aucun
écran ne proposait.

L'alternative au point 2 était d'élargir `capa.verify` à `tous()` et de faire
vérifier l'acteur par l'agrégat — c'est le parti pris de `derogation.approve`. Il
convient là où **tout** dossier désigne un signataire. Ici, la plupart des
jugements sont portés par le pilotage sur des dossiers qui ne désignent personne :
élargir le droit aurait ouvert ces dossiers-là à chacun pour servir le cas où
quelqu'un est nommé. La garde dans le service ne donne le geste qu'à la personne
nommée, sur l'action où elle l'est.

## Ce que cela ne décide pas

- **Le critère reste lu sur le dossier** par le plan de travail des évaluations.
  Un dossier dont le critère ne vit que sur l'action — repris après coup, hors du
  chemin d'ouverture qui le recopie — s'y affiche encore « sans critère ».
- **Les refus opposés au verdict du dossier ne changent pas** : ceux de
  l'ADR 0108 et de l'ADR 0137, obstacle par obstacle. En particulier, un dossier
  dont l'ACTION a promis une vérification refuse le verdict du dossier tant que
  cette action n'a pas conclu (ADR 0132) : sur un dossier d'une seule action, le
  formulaire de l'écran d'efficacité n'a donc rien à y faire — le jugement se
  porte sur la ligne de l'action, et le dossier suit. Faire compter le verdict du
  dossier POUR son action unique est une suite possible, non tranchée ici.
- **Les dossiers déjà jugés avant ce lot** restent en attente d'évaluation : la
  clôture se décide à l'instant du verdict, et rien ne repasse derrière.

## Conséquences

- Un dossier d'une seule action se clôt désormais **depuis la fiche de l'écart**,
  sans passer par l'écran d'efficacité. Cet écran garde son rôle : juger les
  dossiers dont le verdict se porte à leur échelle, et montrer ce qui a été jugé.
- Les bancs qui constataient « le dossier reste `RESOLVED` après le verdict d'une
  action » changent de verdict attendu, à dessein.
- Le tableau du D8 paraît sur tout rapport **non scellé** dès qu'une action a été
  jugée, et sur les rapports émis à partir de maintenant.
