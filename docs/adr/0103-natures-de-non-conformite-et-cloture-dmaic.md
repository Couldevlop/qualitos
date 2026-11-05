# ADR 0103 — Les natures d'écart sont une donnée du client ; clôturer un DMAIC exige la fiche complète et les actions faites

- **Statut** : Accepté
- **Date** : 2026-11-04
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`nonconformity.subcategory`, `NcService`,
  `NcController`, migration V154 ; `DmaicPhaseGate`, `DmaicService.advancePhase`,
  `ResolutionService.completionBlockers`, `ClaimReport`,
  `PdfBoxClaimReportRenderer` ; `CapaDto.OpenCaseRequest.actionType`), web
  (`/admin/nc`, saisie et registre NC, fiche DMAIC, fenêtre CAPA, registre NC
  fournisseurs)
- **Complète** : ADR 0088 (LA fenêtre de création CAPA), ADR 0101 (fiche de
  résolution DMAIC), ADR 0096 (paramétrage SPC — même grammaire de réglage)

## Contexte

Trois manques distincts, un même défaut de fond : **la plateforme classait et
clôturait sans exiger ce qui donne du sens au classement et à la clôture.**

**1. Les non-conformités n'avaient que sept catégories.** Produit, processus,
documentation, fournisseur, sécurité, environnement, autre. C'est assez pour
ranger, pas pour analyser : « 412 NC de processus » ne dit pas s'il s'agit de
dérives de paramètre, de procédures non respectées ou d'étalonnages périmés — et
c'est cette distinction-là qui décide de l'action. Le niveau fin manquait, donc
le Pareto ne descendait jamais sous la catégorie.

**2. Un projet DMAIC se clôturait sur un plan d'actions inachevé.** La phase
« Contrôler » franchie, le statut passait à `COMPLETED` sans que rien ne vérifie
l'état du plan d'actions. Un projet déclaré résolu avec des actions à 60 %
affirme une résolution que rien ne soutient — et c'est précisément ce qu'un
auditeur vient vérifier. Pire : le bouton « Avancer » était **éteint** en phase
« Contrôler », si bien que la clôture était inatteignable depuis l'écran.

**3. L'endiguement n'était pas proposé à la création d'une CAPA.** La fenêtre
offrait « corrective » ou « préventive », au motif qu'un dossier de pur
endiguement ne se clôturerait jamais (ISO 9001 §10.2). Le motif est juste, la
conclusion trop large : elle confondait la nature du **dossier** et celle de
l'**action**. Résultat, la mesure prise le jour même — trier un lot, arrêter une
ligne — se saisissait comme « corrective », et un dossier où l'on avait trié le
lot suspect et rien d'autre se lisait comme un dossier où l'on avait corrigé le
réglage de la presse.

## Décision

### 1. Les natures d'écart sont une donnée du client, pas une énumération du code

Une **sous-catégorie** (`code`, `category`, `label`) qualifie la nature fine de
l'écart sous sa catégorie. Elle est portée par la NC dans
`non_conformities.subcategory_code`, et **facultative** : un écart se saisit
d'abord — souvent sur le terrain, dans l'urgence — et se classe finement
ensuite ; l'exiger à la création ferait saisir n'importe quoi.

Le catalogue n'est **pas** une énumération Java. Les natures d'écart d'une usine
ne sont pas celles d'un CHU, d'une banque ou d'une exploitation agricole, et les
coder en dur inscrirait un secteur dans le cœur de la plateforme (§18.2 #9). La
plateforme livre donc un **catalogue de départ transverse** (`NcSubcategoryDefaults`,
68 natures couvrant l'industrie, la santé, le laboratoire, la banque,
l'informatique, l'agroalimentaire, la logistique, le BTP, l'énergie, le secteur
public et l'éducation) que le client remplace entièrement par le sien. Aucune
ligne en base = le catalogue de la plateforme s'applique, et la saisie fonctionne
dès le premier jour — la même mécanique que le questionnaire DMAIC (ADR 0101) et
les seuils de Cpk (ADR 0096).

Le paramétrage est réservé à l'**administrateur du client** (`authz.manage`,
écran `/admin/nc`) ; la **lecture** est ouverte à tout authentifié, puisque
chaque formulaire de saisie en a besoin.

Deux règles de cohérence, posées au **domaine** et non dans l'écran — l'API est
publique (§13.1) et la file hors-ligne du mobile rejoue par le même chemin :

- un code inconnu du catalogue est refusé (400) ;
- un code rattaché à une autre catégorie que celle de la NC est refusé (400),
  y compris lorsqu'on **change la catégorie** d'une NC déjà classée.

Un code retiré du catalogue par le client reste porté par les NC passées, et
s'affiche tel quel : effacer un classement pour cause de changement de
nomenclature réécrirait l'histoire.

### 2. Clôturer un DMAIC n'est pas avancer d'une phase de plus

`DmaicPhaseGate` gagne `blockingCompletion(projectId)`, distinct de
`missing(projectId, phase)`. La transition `CONTROL → COMPLETED` exige :

- **toute** la fiche de résolution renseignée — pas seulement la phase en cours ;
- **au moins une action** — un problème ne se clôt pas sans action ;
- **chaque action à 100 %**, plan d'amélioration et actions de confinement
  comprises. Endiguer n'exempte pas : une mesure conservatoire laissée en route
  est une protection qu'on croit en place et qui ne l'est pas.

La règle lit la fiche **enregistrée**, jamais un brouillon : ce qui n'est pas
enregistré n'existe pas pour l'auditeur. Le refus nomme ce qui bloque
(« Action à 60 % : « Changer l'outil » »), il ne se contente pas d'un compte.

Côté écran, le bouton devient « Clôturer le projet » en phase « Contrôler », et
**reste cliquable** quand la phase est incomplète : le clic signale alors en
rouge les champs à renseigner et amène l'utilisateur sur le premier. Un bouton
grisé ne dit pas lesquelles des trente questions manquent.

### 3. L'endiguement qualifie l'action, le dossier reste clôturable

`OpenCaseRequest` gagne un `actionType` optionnel. La fenêtre de création CAPA
propose trois natures, **endiguement en premier** — c'est ce qu'on fait le jour
même, avant de connaître la cause (ISO 9001 §10.2 a, étape D3 de la 8D). Choisir
l'endiguement ouvre un dossier **correctif** dont l'**action** est typée
`CONTAINMENT`. Le dossier reste donc clôturable, et sa clôture demandera une
action qui supprime la cause — le verrou `CONTAINMENT_ONLY` existant s'en
charge. La fenêtre le dit à l'écran plutôt que de le laisser deviner.

La fiche DMAIC **recopie** sur sa ligne la nature et la criticité du dossier
ouvert, au lieu de les relire. Le rapport client doit rester lisible des années
plus tard, y compris si le dossier a été requalifié depuis, et sans que la fiche
dépende du module CAPA.

### 4. Le rapport client sort les actions en tableau

Les actions de confinement (phase « Analyser ») et le plan d'actions (phase
« Améliorer ») sont rendus en **tableau**, aux colonnes du registre CAPA :
intitulé, nature, criticité, statut, responsable, échéance. En paragraphes, le
lecteur devait reconstruire mentalement la colonne qu'il cherchait, ligne après
ligne ; le tableau la lui donne, et c'est la même lecture que dans
l'application — donc rien à réapprendre.

## Conséquences

**Acquis.**

- Le Pareto des NC descend sous la catégorie, et le fait avec la nomenclature du
  client — pas celle d'un secteur choisi par l'éditeur.
- Un projet DMAIC clos est un projet dont la fiche est complète et les actions
  faites. L'affirmation devient défendable devant un auditeur.
- La clôture d'un projet est atteignable depuis l'écran, ce qu'elle n'était pas.
- La distinction « arrêter l'effet » / « supprimer la cause » est saisissable à
  l'ouverture du dossier, là où elle se décide.

**Coûts et limites.**

- Les projets DMAIC en cours dont le plan d'actions n'est pas à 100 % ne peuvent
  plus être clos sans solder leurs actions. C'est l'effet voulu, mais il se
  constatera sur l'existant.
- Les NC déjà saisies n'ont pas de nature : la colonne affiche « — ». Aucun
  rattrapage automatique n'est tenté — deviner la nature d'un écart à partir de
  son titre fabriquerait une donnée que personne n'a validée.
- Le catalogue est une liste plate par catégorie, sans hiérarchie à plusieurs
  niveaux. Deux niveaux (catégorie → nature) suffisent à l'analyse ; un
  troisième se paierait en saisie sans servir le Pareto.

**Rejeté.**

- *Une énumération `NcSubcategory` en Java.* Simple, et contraire à §18.2 #9 :
  elle inscrirait un secteur dans le cœur de la plateforme, et toute nature
  manquante deviendrait une livraison.
- *Rendre la nature obligatoire à la création.* Un écart se constate sur le
  terrain ; exiger le classement fin à cet instant ferait choisir au hasard, et
  une donnée choisie au hasard est pire qu'une donnée absente.
- *Autoriser un dossier CAPA de type `CONTAINMENT`.* Il naîtrait bloqué : rien
  dans un dossier de pur endiguement ne permet de le clore. La nature appartient
  à l'action.
- *Relire la nature et la criticité du dossier CAPA à chaque rendu du rapport.*
  Coupler le rapport client au module CAPA, et faire varier un rapport déjà
  remis au client si le dossier est requalifié plus tard.
