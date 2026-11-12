# ADR 0115 — Une CAPA porte plusieurs actions, montrées en tableau

- **Statut** : Accepté
- **Date** : 2026-11-12
- **Owners** : @Couldevlop
- **Portée** : `apps/api-quality-engine` (`quality/capa` : `CapaService.addAction`),
  `apps/web` (`features/capa` : `CapaActionPanelComponent`,
  `CapaActionDialogComponent` + `CapaActionDialogModule`,
  `CapaActionsDialogComponent` + `CapaActionsDialogModule` ;
  `features/nc` : fiche d'écart)
- **Révise** : **ADR 0088 §1 (« une CAPA porte une action »)** sur ce seul point.
  Tout le reste de l'ADR 0088 tient : une seule fenêtre de création de dossier,
  source verrouillée, pilote nommé, endiguement seul non clôturable.
- **Révise** : **ADR 0114** sur sa phrase « pas de liste d'actions, pas
  d'ajout » : la carte de l'écart devient un tableau, et l'ajout existe.
- **S'inscrit dans** : CLAUDE.md §4.2 (CAPA), §3.6 (référentiel transverse),
  §18.2 #2 (tenant et acteur issus du jeton), ADR 0052 (porteur et preuve
  d'action), ADR 0073 (vérification d'efficacité), ADR 0081 (chacun voit ce qui
  le concerne), ADR 0091 (une personne se choisit dans l'annuaire)

## Contexte

L'ADR 0088 a fermé la liste d'actions : un dossier CAPA portait **une** action,
et le serveur refusait la seconde par un 409. La règle avait une bonne raison —
les tableaux d'actions vides, les suggestions IA qui remplissaient des lignes
que personne ne portait — mais elle ne résiste pas à un écart réel.

Un écart se traite en plusieurs gestes, et les normes les nomment séparément :
le 8D distingue l'**endiguement immédiat** (D3), l'**action corrective
permanente** (D5) et la **prévention de la récidive** (D6) ; ISO 9001 §10.2
demande de corriger *et* d'éliminer la cause. Avec une seule action autorisée,
il restait deux contournements, tous deux mauvais :

1. **Ouvrir un second dossier CAPA sur le même écart** — mais un écart n'a qu'un
   dossier (c'est `nc.capaCaseId`), donc le second se retrouvait orphelin, hors
   du 8D et hors du blocage de clôture (ADR 0114).
2. **Tout écrire dans un intitulé unique** (« isoler le lot, reprendre le plan de
   contrôle et former l'équipe de nuit ») — l'avancement n'était alors plus
   mesurable geste par geste : l'action restait « en cours » jusqu'au dernier,
   et la preuve d'efficacité (ADR 0073) ne portait sur rien de précis.

Côté écran, deux autres défauts venaient du même endroit. La fiche d'écart avait
**deux boutons qui changeaient de nom selon l'avancement** — « Créer une CAPA »
puis « Voir la CAPA », « Définir action » puis rien — pour des boutons qui ne
changeaient pas de place : il fallait relire l'écran pour savoir ce qu'on
allait obtenir. Et **la création de CAPA recopiait le titre et la description de
l'écart**, ce qui donnait un dossier qui redisait le constat au lieu de nommer
ce qu'on allait faire — le champ pré-rempli se validant tel quel.

## Décisions

### 1. Le serveur accepte plusieurs actions par dossier

`CapaService.addAction` n'oppose plus de 409 à la seconde action, quelle que soit
la source du dossier (écart interne ou externe, constat d'audit, risque, carte de
contrôle, anomalie, IoT). Les seuls refus qui restent sont ceux qui tiennent à
l'état : un dossier `CLOSED` ou `REJECTED` n'accepte aucune action.

**Le pilote du dossier reste celui de la PREMIÈRE action.** C'est à lui qu'on
demande des nouvelles, et il doit pouvoir voir le dossier (ADR 0081) ; laisser la
dernière action déplacer le pilotage aurait confié le dossier à qui exécute un
geste tardif.

Les blocages de clôture existants (`closureBlockers`) géraient déjà plusieurs
actions et ne changent pas : un dossier dont **aucune** action ne va au-delà de
l'endiguement n'est pas clôturable (`CONTAINMENT_ONLY`), et une action ouverte
bloque la clôture.

### 2. Les actions se lisent en tableau, une ligne s'ouvre pour se corriger

`CapaActionPanelComponent` devient un tableau : une ligne par action, avec les
colonnes nature, intitulé, responsable, échéance, statut, et le geste qui avance
l'action d'un cran. Les colonnes sont **toujours** remplies, même quand le
responsable et l'échéance sont ceux du dossier : une cellule laissée vide selon
la valeur du dossier rendrait la colonne illisible d'une ligne à l'autre, alors
que c'est précisément ce qu'on vient comparer (cela révise le choix inverse fait
dans l'ADR 0114).

Une ligne s'ouvre d'un clic (souris, `Entrée` ou `Espace`) sur
`CapaActionDialogComponent` : c'est là que l'action se corrige — un intitulé à
reformuler, un porteur parti, une échéance à décaler. Avant, il fallait rouvrir
un dossier pour changer une phrase. Un dossier terminé ouvre la même fenêtre **en
lecture seule** plutôt que de ne pas s'ouvrir : relire ce qui a été fait est
légitime après la clôture, et une ligne qui ne réagit pas au clic passe pour une
panne.

**Une seule fenêtre pour l'ajout et pour la correction.** Deux fenêtres jumelles
divergeraient au premier champ ajouté d'un seul côté. L'ajout se distingue de la
correction par l'absence d'action fournie à la fenêtre ; il propose le pilote et
l'échéance du dossier, qui sont le cas le plus fréquent. Le responsable se
choisit dans l'annuaire (ADR 0091), jamais par son identifiant.

### 3. Deux libellés fixes sur la fiche d'écart

- **« Ajouter une CAPA »** (en-tête) : toujours visible, un seul mot. Sans
  dossier, il ouvre la fenêtre de création — source verrouillée sur cet écart ;
  avec, il y emmène.
- **« Ajouter des actions »** : sans dossier, il ouvre la création (le serveur
  fait avancer l'écart du même geste) ; avec, il ouvre
  `CapaActionsDialogComponent`, la fenêtre qui liste les actions du dossier sans
  quitter la fiche. Quitter la fiche pour ajouter une action faisait perdre de
  vue le constat, les photos et la cause.

Il n'y a plus ni « Définir action », ni « Créer une CAPA », ni « Voir la CAPA ».
Quand une action a bougé dans la fenêtre, la fiche se relit : le statut de
l'écart dépend de celui de l'action côté serveur.

### 4. La création de CAPA depuis un écart ne recopie plus les phrases

Le pré-remplissage ne garde que les **données** déduites de l'écart — le type
(`CORRECTIVE`) et la criticité, tirée de la gravité — et la source, verrouillée.
Le **titre et la description partent vides** : ce sont des phrases à écrire, et
les recopier donnait un dossier qui redisait le problème au lieu de nommer la
réponse.

### 5. La fenêtre des actions est un module à part

`CapaActionDialogModule` et `CapaActionsDialogModule` sont des modules distincts
de `CapaModule` : celui-ci porte ses routes via `RouterModule.forChild`, et
l'importer depuis la fiche d'écart grefferait les écrans CAPA à une adresse qui
n'est pas la leur. `CapaActionsDialogComponent` n'embarque aucun tableau à lui :
il héberge `qos-capa-action-panel`, qui tient déjà les droits, l'avancement et la
fenêtre d'une action.

## Alternatives écartées

- **Garder une action et ouvrir un dossier par geste.** Un écart n'a qu'un
  dossier : les suivants seraient orphelins du 8D et du blocage de clôture.
- **Un tableau éditable en ligne.** Le responsable se choisit dans l'annuaire et
  la nature est une question fermée : l'édition en ligne aurait redemandé ces
  deux contrôles dans une cellule, et la validation serveur du titre vide (déjà
  présente dans `updateAction`) ne suffit pas à rendre l'ergonomie lisible.
- **Rétablir les suggestions IA d'actions.** C'est ce que l'ADR 0088 a retiré, et
  pour la bonne raison : des lignes que personne ne porte. Rien ici ne les
  rouvre — chaque action est ajoutée par une personne, dans une fenêtre.

## Conséquences

- ✅ Les trois gestes du 8D (D3, D5, D6) s'écrivent et s'avancent séparément, dans
  un seul dossier rattaché à l'écart.
- ✅ L'avancement est mesurable action par action ; la clôture reste bloquée tant
  qu'une action est ouverte.
- ✅ Deux libellés fixes au lieu de quatre mots changeants ; l'écart ne se quitte
  plus pour ajouter une action.
- ⚠ Les dossiers de risque (ADR 0077) peuvent désormais recevoir plusieurs
  actions. C'est volontaire : le modèle était identique, et la restriction n'y
  avait pas d'autre justification que l'ADR 0088.
- ⚠ Le rapport 8D ne liste encore qu'une action en D5/D6 : le tableau y reste à
  porter (dette identifiée, suivie hors de cet ADR).

## Tests d'invariant

- `CapaServiceTest` :
  `addAction_dossierAyantDejaUneAction_enAccepteUneSeconde_quelleQueSoitSaSource`,
  `addAction_secondeAction_neDeplacePasLePiloteDuDossier`,
  `addAction_leResponsableDeLaPremiereActionDevientLePiloteDuDossier`, et les
  refus sur dossier clos ou rejeté.
- `capa-action-panel.component.spec.ts` : ajout avec pilote et échéance proposés,
  ouverture d'une ligne pour correction, lecture seule sur dossier clos,
  non-confusion entre l'avancement d'un cran et l'ouverture de la ligne.
- `nc-detail.component.spec.ts` : libellé d'en-tête inchangé avec ou sans
  dossier, « Ajouter des actions » qui ouvre la création sans dossier et la liste
  avec, relecture de la fiche quand une action a bougé, et pré-remplissage sans
  titre ni description.

## Références

- CLAUDE.md §4.2 (CAPA), §3.6 (référentiel transverse), §18.2 #2
- ADR 0052, ADR 0073, ADR 0077, ADR 0081, ADR 0088, ADR 0091, ADR 0114
- ISO 9001:2015 §10.2 ; démarche 8D (D3, D5, D6)
