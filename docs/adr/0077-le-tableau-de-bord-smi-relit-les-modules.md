# ADR 0077 — Le tableau de bord SMI relit les modules, il ne stocke rien

- **Statut** : Accepté
- **Date** : 2026-10-08
- **Owners** : @Couldevlop
- **Portée** : Module `smi` (api-quality-engine), écrans `/smi`, `/smi/risques`,
  `/smi/exigences` (web) ; retouches du registre des risques (ADR 0076) et de la
  fiche CAPA
- **S'inscrit dans** : CLAUDE.md §7.1 (dashboards), §8.9 (systèmes intégrés,
  co-couverture), §3.6 (référentiel transverse), §18.2 (tenant issu du jeton),
  ISO 9001 / 14001 / 45001 Annexe SL

## Contexte

La maquette `docs/Tableau de bord SMI.pptx` décrit trois écrans :

1. un tableau de bord « Système de Management Intégré » filtrable par norme :
   conformité globale, actions en retard, risques majeurs ouverts, prochain
   audit ; conformité par référentiel ; matrice des risques 5 × 5 ; « À traiter
   cette semaine » ;
2. « Risques d'une case » : chaque case de la matrice ouvre la liste de ses
   risques (filtres brut/résiduel, type, processus, site) et un panneau de
   détail avec « Créer une action CAPA » et « Ouvrir la fiche risque » ;
3. « Matrice des exigences · une preuve, plusieurs référentiels » : chapitres
   communs × normes, Couvert / Partiel / Écart, colonne « Module QualitOS »,
   ouverte par un titre bien visible en bas du tableau de bord.

Dans le même lot, trois retouches du registre : cause et effet d'un risque
deviennent obligatoires ; « Créer une action CAPA » demande la nature
(corrective ou préventive), un responsable et une échéance obligatoire ; sur
la fiche d'une CAPA issue d'un risque, le bloc « Actions / Ajouter une action /
Suggérer (IA) » disparaît.

## Décisions

### 1. Un agrégat en lecture, sans table

Le SMI n'a aucune donnée propre. `SmiDashboardService` lit, par des ports, le
Standards Hub (adoptions et alignement), les actions CAPA, le registre des
risques, le planning d'audit, les étalonnages et les changements. Les
adaptateurs passent par les services des modules quand ils existent
(`StandardsService`, `RiskRegisterService`, `AuditService`) pour hériter de leur
cloisonnement ; trois requêtes manquaient et sont ajoutées aux dépôts —
compte des actions CAPA en retard (total et critiques), actions dues d'ici une
date, changements en attente de validation prévus d'ici une date — toutes
bornées au client du jeton. Une seule requête HTTP par écran.

### 2. Le filtre de norme sans table de correspondance

Le code d'une norme du Standards Hub (`iso-45001`) se lit tel quel dans les
exigences du registre (`ISO_45001_6_1`) et, ramené à ses lettres et chiffres,
dans le référentiel libre d'un audit planifié (« ISO 45001:2018 »). Une norme
ajoutée au catalogue est donc filtrable sans code. Les actions CAPA et les
étalonnages, qui ne portent aucune norme, restent comptés en entier : les
filtrer les ferait disparaître sans raison. Un code non adopté ne filtre rien
(`selected` nul) plutôt que d'afficher un tableau vide.

### 3. Définitions des indicateurs (§18.2 n° 8)

- **Conformité** d'une norme : `overallScore` de l'alignement (exigences
  prouvées / exigences, Standards Hub) ; **globale** = moyenne des normes
  affichées, ou la norme filtrée.
- **Actions en retard** : actions CAPA non terminées, échéance dépassée, dans un
  dossier ni clos ni rejeté ; **critiques** = dossier de criticité CRITICAL.
- **Risques majeurs** : risques non clos (acceptés compris) de score brut ≥ 10.
- **Matrice** : risques non clos, brute ou résiduelle visée ; un risque sans
  résiduelle n'apparaît pas dans la grille résiduelle, et l'écran le dit.
- **Cette semaine** : échéances d'ici sept jours, retards compris, huit au plus.
- **Case de la matrice des exigences** : Couvert si toutes les exigences du
  chapitre ont une preuve, Partiel si certaines, Écart si aucune, Sans objet si
  la norme n'a pas d'exigence sous ce chapitre.

### 4. La colonne « Module QualitOS » est une constante des chapitres

Les sept chapitres de l'Annexe SL (4 à 10) et les modules qui en fabriquent les
preuves (`HlsChapter`) ne dépendent ni de la norme ni du secteur : ce n'est pas
une logique sectorielle (§18.2 n° 9). Le serveur rend des codes, l'écran les
nomme dans sa langue.

### 5. Une CAPA issue d'un risque EST son action

`POST /risk-register/risks/{id}/capa` exige désormais `kind`
(CORRECTIVE | PREVENTIVE), `assignee` et `dueDate` (pas dans le passé). Le
dossier naît avec sa nature et SON action — même nature, même échéance,
confiée au responsable — dans la transaction du contrôleur. Sur la fiche CAPA
d'un tel dossier, le bloc d'actions (ajout, suggestion IA, édition en ligne)
laisse place à la seule action et à son bouton d'avancement : la clôture
exige toujours une action terminée, la règle `NO_ACTION` reste intacte. Un
dossier de risque antérieur, sans action, garde le bloc complet — sinon il ne
pourrait jamais être clôturé.

### 6. Cause et effet obligatoires

Domaine (`Texts.required`, 422 sur le champ) et formulaire. La base reste
`NULL`-able : les risques déjà saisis sans cause ni effet restent lisibles, et
leur prochaine révision demandera de les compléter.

### 7. « Créer une action CAPA » depuis la matrice des exigences

L'écran SMI ne charge pas le module CAPA : il ouvre `/capa?nouveau=1&titre=…&ref=…`,
que la liste CAPA lit une fois, efface de l'adresse, et transforme en fenêtre
de création préremplie. Rien n'est créé sans validation humaine.

## Conséquences

- Nouvelle entrée de navigation « Tableau de bord SMI » (groupe Pilotage, sans
  module : il ne fait que relire).
- Lecture ouverte à tout authentifié, comme le tableau de bord exécutif ; les
  écritures restent celles des modules d'origine.
- Le nom de l'auditeur du prochain audit n'est pas affiché : le planning ne
  porte qu'un identifiant et le moteur n'a pas d'annuaire lisible.
- Les exigences portées par le registre (`RegisterRequirement`) ne sont pas
  encore des preuves du Standards Hub : les y relier reste à faire.

## Vérification

- `SmiDashboardServiceTest`, `SmiDomainTest`, `SmiAdaptersTest`,
  `SmiDashboardControllerTest`, `SmiQueriesIntegrationTest` (requêtes jouées sur
  le mapping JPA réel, cloisonnement par client compris).
- `RiskTest`, `RiskRegisterServiceTest`, `RegisterInfrastructureTest`,
  `RiskRegisterControllerTest` pour cause/effet et l'action CAPA.
- Specs web : `smi-dashboard`, `risk-cell`, `requirements-matrix`,
  `risk-capa-opener`, `action-dialog`, `item-form`, `item-detail`,
  `capa-detail.actions-table`, `capa-list`, `capa-create-dialog`, `main-shell`.
