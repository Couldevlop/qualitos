# ADR 0088 — Une CAPA, une action, une seule fenêtre de création

- **Statut** : Accepté
- **Date** : 2026-10-14
- **Owners** : @Couldevlop
- **Portée** : module `capa` (api-quality-engine et web), escalade NC, constats
  d'audit, registre des risques (ADR 0076/0077), panneau de case de la matrice SMI
- **S'inscrit dans** : CLAUDE.md §3.6 (référentiel transverse), §4.2 (CAPA),
  §18.2.2 (tenant et acteur issus du jeton), ADR 0052 (porteur et preuve
  d'action), ADR 0073 (vérification d'efficacité), ADR 0077 (dossier de risque =
  action unique), ADR 0081 (chacun voit ce qui le concerne)

## Contexte

Trois fenêtres différentes ouvraient une CAPA : la création depuis la liste
(sans responsable, pilote = qui clique), « Ajouter une action » (depuis la fiche
CAPA et depuis une NC, qui créait d'abord un dossier vide), et la fenêtre
d'action du registre des risques (responsable en texte libre). Les constats
d'audit n'avaient aucun chemin : on saisissait l'identifiant d'une CAPA à la
main. La fiche CAPA, elle, montrait un tableau d'actions à remplir, avec des
suggestions IA d'actions multiples, sauf pour les dossiers de risque (ADR 0077).

Le responsable produit demande la même fenêtre partout, plus d'« Ajouter une
action », et un pilote qui soit vraiment le responsable de la CAPA de risque —
affiché par son nom.

## Décisions

### 1. Une CAPA porte une action

Le modèle de l'ADR 0077 est généralisé à toutes les sources : l'intitulé, la
nature (corrective ou préventive), le responsable et l'échéance sont à la fois
ceux du dossier et de son unique action. Le serveur refuse une deuxième action
(409) quelle que soit la source. Un dossier sans action — ancien, ou ouvert par
un automate (carte de contrôle, IoT, anomalie) — peut recevoir la sienne une
fois ; la désigner en fait aussi le pilote du dossier.

L'endiguement seul n'est plus proposé à l'ouverture : un dossier dont l'unique
action ne fait qu'endiguer ne pourrait jamais être clos (motif
`CONTAINMENT_ONLY`, ISO 9001 §10.2). Les anciens dossiers dans ce cas le disent
et renvoient vers une CAPA corrective.

### 2. Une ouverture, une écriture

`CapaService.openCase` crée le dossier et son action dans la même écriture
(cascade), journal de cycle de vie compris. Chaque écran d'origine garde sa
route, qui fixe la source côté serveur et pose le lien :

| Origine | Route | Source / référence |
|---|---|---|
| Liste CAPA | `POST /capa/cases/open` | choisie (jamais `RISK`) |
| Non-conformité | `POST /nc/{id}/escalate-capa` | `NON_CONFORMITY` / `NC-…`, `capa_case_id` posé |
| Constat d'audit non conforme | `POST /audits/plans/{p}/findings/{f}/capa` | `AUDIT` / `AUD-… / constat n`, `capa_id` posé |
| Risque (fiche, case SMI) | `POST /risk-register/risks/{id}/capa` | `RISK` / `R-…`, criticité de la cotation brute |

Le corps est le même partout (`OpenCaseRequest`) : jamais de tenant ni
d'auteur, lus dans le jeton.

### 3. Un pilote désigné, et nommé

Le pilote est le responsable choisi dans l'annuaire du client, identifié par
son compte de connexion (`sub`) — c'est ce que la portée « ce qui me concerne »
compare (ADR 0081). Son libellé est recopié dans `capa_cases.owner_name`
(migration V139), comme celui du vérificateur (ADR 0073). Pour un risque, le
propriétaire saisi en clair est proposé s'il se rapproche strictement d'un
compte ; celui qui clique reste l'auteur dans l'historique et la piste d'audit.
L'écran n'affiche jamais un identifiant : nom recopié, sinon nom connu de
l'annuaire, sinon « — ».

L'annuaire (`/api/v1/users`) n'est lisible que des administrateurs. Sans ce
droit, la fenêtre propose l'utilisateur courant seul et le dit : elle reste
utilisable, sans jamais montrer d'identifiant. Ouvrir une lecture de l'annuaire
à tous les membres est une décision produit distincte, en attente.

### 4. Une seule fenêtre, en deux modes

`CapaCreateDialogComponent` (module exportable `CapaCreateDialogModule`) est
ouvert par toutes les sections ; l'écran d'origine fournit la source verrouillée
et sa route d'ouverture. Le mode « définir l'action » est le même formulaire,
réduit, pour un dossier sans action. Les fenêtres « Ajouter une action » et le
mode CAPA de la fenêtre d'action du registre disparaissent.

### 5. Plus de suggestions d'actions multiples

Le bouton « Suggérer (IA) » proposait cinq à huit actions à ajouter : il n'a plus
d'objet quand le dossier porte une action décidée à l'ouverture. La route
`suggest-actions` est retirée avec lui.

## Conséquences

- La fiche CAPA montre l'action (nature, responsable, échéance, statut, preuve)
  et la fait avancer. Les anciens dossiers à plusieurs actions les montrent
  toutes, en lecture et avancement, sans ajout ; la clôture et ses motifs de
  blocage (ADR 0052) ne changent pas.
- Les KPI qui comptent les actions restent justes : une action par dossier
  nouveau, les anciennes actions restent comptées.
- Les dossiers ouverts par un automate naissent toujours sans action : la fiche
  propose « Définir l'action ».

## Tests d'invariant

- `CapaServiceTest` : ouverture dossier + action en une écriture, refus de
  l'endiguement, de l'échéance passée, d'une seconde action pour toute source,
  pilote désigné par la définition de l'action.
- `CapaControllerTest`, `NcServiceTest`, `AuditFindingCapa*Test`,
  `RiskRegisterServiceTest`, `RegisterInfrastructureTest` : la source vient de
  la route, jamais du corps ; le pilote est le responsable choisi.
- Specs web : fenêtre unique (création libre, source verrouillée, mode
  « définir l'action », annuaire illisible), fiche réduite à l'action, pilote
  nommé, points d'entrée NC / audit / risque.

## Références

CLAUDE.md §3.6, §4.2, §18.2 ; ADR 0052, 0073, 0076, 0077, 0081.
