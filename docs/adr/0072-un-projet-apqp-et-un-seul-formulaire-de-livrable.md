# ADR 0072 — L'APQP se mène par PROJET, et tout livrable porte le même formulaire

- **Statut** : Accepté
- **Date** : 2026-09-14
- **Owners** : @Couldevlop
- **Portée** : Module APQP (projets, cycle en V, livrables, dossier PPAP, référentiel
  d'amorçage), migration **V131**
- **Révise** : [ADR 0068](./0068-un-livrable-apqp-se-coche-et-se-prouve.md) sections 1 et 2
  (le GENRE fermé, et le `data jsonb` qu'il commande) · [ADR 0066](./0066-le-cycle-apqp-appartient-au-tenant.md)
  (le cycle appartenait au tenant ; il appartient désormais à un projet DU tenant)
- **S'inscrit dans** : [ADR 0065](./0065-autorisation-decidee-avant-la-lecture-du-corps.md)
  (autorisation décidée avant la lecture du corps) · [ADR 0070](./0070-le-referentiel-apqp-suit-la-langue.md)
  (le référentiel suit la langue, ce que le client écrit lui appartient)

## Contexte

Le commanditaire a fourni le classeur qu'il tient réellement — `docs/APQP_Tracker.xlsx`,
feuille « APQP Tracker », 48 lignes. Le confronter à ce que la plateforme proposait a
fait apparaître deux écarts de fond, et l'un explique l'autre.

### Un cycle par client, alors qu'on mène plusieurs programmes

L'ADR 0066 posait que le cycle APQP est une donnée du tenant, amorcée à la première
lecture. Un tenant, un cycle. Or une organisation mène **plusieurs programmes de
front** — une introduction de produit pour un client, un transfert d'outillage pour un
autre — et **chacun remet son propre dossier PPAP**. Le cycle unique les mélangeait :
impossible de dire de quel programme venait un livrable coché, impossible de remettre
le dossier de l'un sans y trouver les pièces de l'autre.

C'est exactement la structure que les projets PFMEA ont déjà, et pour la même raison.

### Un genre fermé qui décidait du formulaire — et qui bloquait la case

L'ADR 0068 avait tranché, section 1 : un livrable porte un **GENRE** parmi quatre
(`ATTACHMENT`, `MODULE_LINK`, `DATA_ENTRY`, `CHECKLIST`), et ce genre décide de ce que
son formulaire demande. Le raisonnement était bon en soi — un formulaire par livrable
aurait figé une cinquantaine d'écrans — mais il a produit trois défauts à l'usage.

1. **La moitié des livrables ne se cochait pas.** L'écran portait
   `[disabled]="!editable || livrable.kind === 'MODULE_LINK'"` : les livrables « renvoi
   module » — Plan de surveillance, AMDEC processus — avaient leur case **désactivée en
   dur**. Un pilote de programme qui voulait dire « c'est fait » devait ouvrir un popup
   et désigner un enregistrement, y compris quand la preuve était ailleurs.
2. **Quatre formulaires répondaient à une seule question.** « Où en est-on ? » se pose
   de la même façon sur les quarante-huit lignes. Le genre imposait de savoir, AVANT
   d'ouvrir, quel écran on allait voir.
3. **Le classeur du client, lui, a UNE ligne par livrable et LES MÊMES colonnes pour
   toutes** : artefact attendu, PPAP requis, responsable, échéance, statut, avancement,
   notes. C'est ce que l'on tient dans la vraie vie, et ce n'est pas quatre formes.

### Un dossier PPAP figé par le référentiel

L'astérisque du référentiel (`ppap`) était une marque de PLATEFORME. Le classeur en fait
une **colonne que l'utilisateur pilote** — colonne E, « PPAP Req'd », douze `Y`. Le
client sait mieux que le référentiel ce que son client à lui exige.

## Décision

### 1. Le projet est l'unité de travail

Un **projet APQP** (`apqp_projects`) porte un nom, un **type**, un client destinataire,
une référence de programme et une description. Les phases lui appartiennent
(`apqp_phases.project_id`), et le tenant reste le propriétaire de tout.

Quatre types, fermés : `NPI` (produit nouveau), `TOW` (transfert d'activité ou
d'outillage), `NEW_CUSTOMER` (produit connu, client nouveau), `OTHER`. Fermé parce que
c'est sur le type qu'on filtre la liste, et qu'un texte libre aurait produit autant
d'orthographes que de saisies.

`/apqp` affiche désormais la **liste des projets**, sur le modèle de la liste des projets
PFMEA. Le V et le dossier PPAP vivent DANS un projet : `/apqp/:projetId` et
`/apqp/:projetId/ppap`. L'entrée de menu « Le cycle » devient « Projets », et l'entrée
« Dossier PPAP » disparaît — le dossier appartient à un programme, il n'a plus d'adresse
globale.

Le cycle est amorcé **à la création du projet** et non plus à la première lecture : le
projet n'existe que parce que quelqu'un vient de le demander, il n'y a donc plus rien à
économiser en différant, et « un projet sans cycle » serait un état de plus à traiter
dans chaque écran.

### 2. Un seul formulaire, et ce sont les colonnes du classeur

Le genre disparaît, avec ses quatre corps de formulaire. Tout livrable porte, et porte
seul :

| Colonne du classeur | Champ |
| --- | --- |
| D — Output / Artifact | `expected_artifact` |
| E — PPAP Req'd | `ppap` |
| F — Owner | `owner` |
| G — Due Date | `due_date` |
| H — Status | `status` (`NOT_STARTED`, `IN_PROGRESS`, `BLOCKED`, `DONE`) |
| I — % Complete | `percent_complete` |
| J — Comments / Notes | `comment` |

Plus, **toujours**, la possibilité de joindre une pièce — ce qui était réservé au genre
`ATTACHMENT` vaut pour tous, parce qu'un livrable se prouve quel qu'il soit.

Le libellé dit CE QU'ON DOIT PRODUIRE, l'artefact dit SOUS QUELLE FORME. C'est la seule
chose que le genre prétendait dire, et l'artefact la dit en clair, dans les six langues.

### 3. Le renvoi devient un champ, pas un genre

Renvoyer vers une AMDEC, un cycle PDCA ou une CAPA reste possible sur **n'importe quel**
livrable, et n'est imposé à aucun. Ce qu'on continue de refuser :

- un renvoi **à moitié posé** (genre sans identifiant, ou l'inverse) — l'écran
  afficherait un lien qui ne mène nulle part ;
- un renvoi **mort** — `ApqpLinkResolver` vérifie que l'enregistrement existe DANS le
  tenant, car un lien mort affirme qu'une preuve existe. Cette garantie de l'ADR 0068
  survit intacte, ainsi que le bouton « Ouvrir la fiche ».

### 4. La case pilote ; le statut et l'avancement suivent

Une seule vérité, et c'est la case.

- `done = true` ⇒ statut `DONE`, avancement 100, **quoi qu'on ait envoyé d'autre**.
- `done = false` ⇒ le statut demandé s'il n'est pas `DONE`, `IN_PROGRESS` sinon ;
  l'avancement demandé s'il est inférieur à 100, **zéro** sinon.
- Un champ **absent** vaut « applique la règle » — c'est le cas quand on coche depuis la
  liste, qui n'a pas de formulaire.

Sans cette réconciliation côté serveur, l'écran aurait pu afficher « terminé à 40 % », ou
« non démarré » sous une case cochée. Ces deux états n'ont pas de lecture.

`doneAt` vient du serveur et `doneBy` du jeton, jamais du corps : c'est inchangé depuis
l'ADR 0068, et décocher les efface toujours.

### 5. Le dossier PPAP est piloté par l'utilisateur, et se dit « requis »

`ppap` est **amorcé** sur les douze `Y` du classeur, puis piloté livrable par livrable
depuis le formulaire unique. Le dossier reste une **VUE** du cycle (ADR 0068 §6) et non
une seconde liste. Son vocabulaire change : on écrit « livrables **requis** », et non
plus « acquis » ou « obtenus » — c'est ce que le client exige, pas ce que nous avons
obtenu.

### 6. Le référentiel est réconcilié avec le classeur

Les 48 lignes du classeur pour 47 clés distinctes : « Control plan » figure en phase 3
(plan de pré-lancement) ET en phase 4 (plan de production). Même livrable, deux états —
d'où **deux clés d'artefact pour une seule clé de livrable**, et un artefact porté
explicitement par le modèle plutôt que déduit de la clé.

Les 48 artefacts sont traduits dans les six langues servies (`fr` source, `en`, `es`,
`ar`, `ja`, `zh`), les textes anglais du classeur faisant foi. La règle de l'ADR 0070
s'applique à l'artefact comme au libellé : traduit tant qu'il est mot pour mot celui du
référentiel, littéral dès qu'un utilisateur l'a réécrit.

Un artefact **jamais renseigné** retombe sur celui du référentiel. C'est ce qui permet
aux cycles écrits avant la V131 d'afficher l'artefact sans qu'on ait eu à recopier
quarante-huit textes dans une migration SQL, où ils auraient formé une seconde
définition du référentiel — celle qui diverge au premier ajustement.

### 7. La reprise ne perd rien

Migration **V131** :

1. Les clients qui avaient un cycle reçoivent **un projet « Projet par défaut »** de type
   `OTHER` (et eux seuls : un client qui n'avait jamais ouvert l'écran n'en reçoit aucun,
   un projet vide posé d'office aurait pollué toutes les listes).
2. Toutes leurs phases y sont versées ; `project_id` devient `NOT NULL` **après**
   remplissage.
3. L'unicité du rang passe de `(tenant, position)` à `(projet, position)` : deux
   programmes ont chacun leur phase 1.
4. Les livrables déjà cochés prennent `DONE` / 100 %, faute de quoi tout un cycle
   s'afficherait « non démarré » sous des cases cochées.
5. Le contenu saisi dans les anciens formulaires (`data`) est **replié en texte dans les
   notes** avant que la colonne ne parte — et seulement lorsqu'il porte une saisie réelle
   (une case cochée, une valeur), car recopier l'amorçage intact aurait rempli les notes
   de tout le monde avec ce que le référentiel disait déjà.

Cases cochées, marques PPAP, commentaires, pièces jointes : tout survit, et un banc sur
PostgreSQL réel le prouve (`ApqpProjetsRepriseOnPostgresTest`, tag `migration`).

## Alternatives écartées

**Garder le genre et rendre seulement la case active.** Corrigeait le défaut le plus
visible sans toucher au reste — mais laissait quatre formulaires pour une question, et
surtout aucune place pour les colonnes F à I du classeur, qui n'appartiennent à aucun
genre en particulier.

**Un cinquième genre « ligne de classeur ».** Aurait fait cohabiter deux modèles de
livrable dans le même cycle, et obligé chaque écran à demander lequel il regarde. Le
classeur ne décrit pas un cas de plus : il décrit LE cas.

**Garder la colonne `data` « au cas où ».** Une colonne que plus rien n'écrit ni ne lit
n'est pas une précaution, c'est une dette qu'on redécouvre trois lots plus tard sans
savoir ce qu'elle contenait. Le contenu réel est replié dans les notes, où l'utilisateur
le voit.

**Un projet unique par client, renommable.** Revenait au cycle unique sous un autre nom,
et ne réglait ni les dossiers PPAP séparés ni la question « de quel programme vient ce
livrable ? ».

**Déduire `ppap` du référentiel sans laisser l'utilisateur le changer.** Les exigences
PPAP varient d'un client à l'autre et d'un programme à l'autre. Le référentiel est un
bon point de départ, pas un arbitre.

## Conséquences

- Migration **V131** : table `apqp_projects`, `apqp_phases.project_id` (`NOT NULL`,
  cascade), cinq colonnes de plus sur `apqp_deliverables`, `kind` et `data` retirées,
  unicité du rang déplacée sur le projet.
- Les routes du cycle sont préfixées par `/api/v1/apqp/projects/{projectId}` — y compris
  les preuves. La phase est relue **dans son projet et dans son client** : trois filtres
  qui, ensemble, ferment la porte à un identifiant emprunté à un autre programme.
- `ApqpDeliverableKind` et `ApqpDeliverableDataValidator` disparaissent, avec leurs bancs.
- Le référentiel gagne 48 entrées d'artefact × 6 langues ; les 21 clés de sous-points et
  d'intitulés de mesures, devenues sans objet, sont retirées.
- La liste des projets porte ses compteurs, calculés en **une** requête groupée : quatre
  chiffres par ligne, pas quatre appels.
- Supprimer un projet emporte son cycle, ses livrables et leurs preuves. C'est voulu,
  c'est confirmé à l'écran, et la cascade est tenue par la base.

## Tests d'invariant

- `ApqpProjetsRepriseOnPostgresTest` (tag `migration`, PostgreSQL réel) : chaque client
  qui avait un cycle reçoit un projet et un seul ; aucune phase n'est orpheline ; les
  cases, marques, commentaires et pièces survivent ; le contenu saisi est replié dans les
  notes et l'amorçage intact ne l'est pas ; deux projets ont chacun leur phase 1 ; la
  suppression d'un projet emporte tout son cycle.
- `ApqpServiceTest` : la case pilote dans les trois sens (cocher, décocher sans
  formulaire, décocher en choisissant un statut), « terminé » sans la case retombe sur
  « en cours », le projet d'un autre client est introuvable, une phase d'un autre projet
  aussi, « Control plan » prend l'artefact de SA phase.
- `ApqpControllerTest` / `ApqpProjectControllerTest` : lire est ouvert, écrire exige le
  pilotage qualité, et le refus précède la lecture du corps (ADR 0065).

## Références

`CLAUDE.md` §3.6 (référentiel transverse), §5.2 (pack Automobile — APQP/PPAP),
§15.1 (six langues), §18.2 règles 2, 7 et 9, §22.2.
