# ADR 0143 — L'objectif propose d'agir, il ne rédige pas la CAPA

- **Statut** : accepté
- **Date** : 2026-11-27
- **Portée** : revue « Objectifs vs KPI réels », fenêtre « Nouvel objectif
  qualité », ouverture d'un dossier CAPA depuis un écart d'objectif
- **Amende** : l'ADR 0133 (§ « la CAPA se propose » et le modèle de données de
  `quality_objectives`)
- **S'appuie sur** : l'ADR 0073 et l'ADR 0108 (critère et verdict d'efficacité),
  l'ADR 0088 (fenêtre d'ouverture de CAPA partagée), l'ADR 0052 (libellés figés)

## Contexte

L'ADR 0133 a livré la chaîne complète : la règle compte les périodes en écart,
rédige une proposition de CAPA — titre, description, critère d'efficacité — et
l'écran la présente dans un panneau où le pilote l'accepte, l'accepte en
ajoutant une action, ou la rejette avec un motif.

La recette du 27/11/2026 a tranché contre trois morceaux de ce dispositif.

**1. Le texte prérédigé ne décrivait pas l'action.** La proposition disait
l'écart constaté (« Objectif qualité critique — Taux de NC client, 900 ppm pour
une cible de 500 »), ce qui est un CONSTAT, pas un plan. Accepté tel quel — et
il l'était, puisqu'un bouton suffisait — le dossier naissait avec un intitulé
qui ne nommait aucun geste. La recette demande que la colonne s'appelle
« Action », que le bouton ouvre la fenêtre CAPA ordinaire, et qu'il n'y ait
« pas de CAPA préremplie proposée ».

**2. L'objectif portait un pilote de CAPA et un délai d'analyse de cause
racine.** Deux champs réglés à la définition de l'objectif, c'est-à-dire des
mois avant l'écart qu'ils serviront à traiter. La recette les a encadrés en
rouge : ils décident trop tôt de ce qui se décide au moment d'agir.

**3. La cible et les seuils se saisissaient deux fois** — une fois au catalogue
des KPI, une fois sur l'objectif — sans que rien ne relie les deux. Deux
chiffres pour un même engagement finissent par diverger, et personne ne sait
lequel fait foi.

## Décision

### 1. Aucune CAPA préremplie : l'écran ouvre la fenêtre, l'utilisateur écrit

Le panneau « CAPA préremplie » disparaît. Sur une ligne en alerte ou en
critique, et seulement si aucun dossier ouvert ne la tient déjà, la colonne
**Action** porte un bouton **« Ajouter une action »** qui ouvre la fenêtre CAPA
partagée (ADR 0088), vide : l'utilisateur y écrit ce qu'il va faire, choisit le
responsable dans l'annuaire et pose l'échéance.

Le dossier naît par `POST /quality-objectives/{id}/capa`, et non plus par
`POST /proposals/{id}/accept`, qui est retiré. Trois choses restent portées par
le serveur et ne se saisissent pas :

- la **source** `QUALITY_OBJECTIVE` et sa référence, qui rattachent le dossier à
  l'objectif, au niveau atteint et à la période ;
- la **criticité**, qui suit le niveau atteint — elle est verrouillée dans la
  fenêtre : la laisser saisir permettrait de traiter un écart critique en
  « faible » ;
- la **vérification d'efficacité**, exigée, et son **critère**, rédigé par la
  règle et affiché en lecture seule. C'est la garantie de l'ADR 0133 qu'on ne
  voulait surtout pas perdre : un écart d'objectif ne se referme pas sur une
  déclaration, il se referme quand le KPI est revenu sous la cible pendant le
  nombre de périodes exigé. Laisser saisir ce critère reviendrait à clore sur une
  cible plus facile que l'engagement pris.

**La règle continue de proposer.** Le moteur d'évaluation, les propositions et
leur historique sont inchangés : ce qui change est leur emploi. Une proposition
n'est plus un texte qu'on accepte, c'est le **signal** qui dit qu'il y a un
écart à traiter ; le bouton apparaît sur le statut de la ligne, donc dès la
première période en alerte, avant que la règle n'ait atteint ses périodes
consécutives. Quand un dossier s'ouvre et qu'une proposition était en attente
sur cet objectif, elle est **soldée** par ce dossier (acceptée, avec son
identifiant et son décideur) : sans cela l'écran redemanderait indéfiniment de
décider d'un écart déjà pris en charge.

Le refus motivé est conservé — c'est la trace qui distingue « on a jugé qu'il
n'y avait rien à faire » de « personne n'a regardé ». Il n'est plus dans un
panneau mais sur la ligne, en **« Aucune action — justifier »**, et seulement
quand une proposition est effectivement en attente.

### 2. L'objectif ne porte ni pilote ni échéance

`capaOwnerId`, `capaOwnerName` et `rootCauseDueDays` quittent le modèle de
l'objectif, l'API et la fenêtre de définition. Le responsable et l'échéance se
saisissent dans la fenêtre CAPA, au moment où l'on décide d'agir, par les champs
que cette fenêtre a déjà et que tous les autres dossiers utilisent.

En base, les colonnes sont **relâchées, pas supprimées** (`V183`) : pendant un
déploiement progressif l'ancienne version du service est encore en ligne et les
écrit. Leur suppression appartient à une migration ultérieure.

### 3. Les seuils se reprennent du KPI, et le report est proposé

Choisir le KPI descend sa cible et ses deux seuils dans les champs de
l'objectif, qui restent **modifiables** — un objectif peut viser mieux que la
santé courante de l'indicateur. Seules les valeurs que le KPI porte sont
reprises : un indicateur sans cible n'efface pas une saisie en cours. En
modification, le KPI est verrouillé et les seuils ne sont pas repris : ils
réécriraient un engagement déjà pris.

Si les seuils enregistrés s'écartent de ceux du KPI, la fenêtre **propose** de
les y reporter, et ne le fait qu'après accord. Elle ne synchronise pas d'office,
parce que l'ADR 0133 §1 autorise plusieurs objectifs sur un même KPI : écrire
sans demander écraserait silencieusement la cible d'un autre objectif. Le report
n'est proposé qu'à qui peut écrire au catalogue (`kpi.manage`), et il part
**après** l'enregistrement de l'objectif : l'inverse réécrirait le catalogue pour
un objectif qui n'a pas abouti. S'il échoue, l'objectif reste enregistré — c'est
lui que l'utilisateur venait créer.

### 4. La période se choisit en mm/aaaa

Le filtre de date de la revue devient une liste déroulante des 24 derniers mois,
avec « Période courante » par défaut. La revue se lit par période ; une date au
hasard dans le mois n'apporte rien et laisse croire à une précision qui n'existe
pas.

## Conséquences

- **Ce qu'on gagne** : le dossier dit ce qu'on va faire, et il le dit parce que
  quelqu'un l'a écrit. Le responsable et l'échéance sont ceux du moment où l'on
  agit. Une seule cible fait foi, et son alignement est un geste conscient.
- **Ce qu'on perd** : l'ouverture en un clic. Agir demande maintenant de remplir
  une fenêtre — c'est le prix du point précédent, et la recette l'a payé
  sciemment.
- **Ce qui reste garanti** : la vérification d'efficacité exigée, son critère
  verrouillé, la criticité imposée, la source et la période inscrites dans le
  dossier, le refus motivé tracé. Aucune de ces garanties n'est passée du serveur
  à l'écran.
- **Dette signalée, non traitée** : le paquet `objective` est plat (§18.3) ; ce
  lot ne l'a pas replié, pour ne pas mêler une refonte de structure à un
  correctif de recette. La ligne « Liens générés » de la maquette, qui vivait
  dans le panneau supprimé, n'a pas été relogée ailleurs.
