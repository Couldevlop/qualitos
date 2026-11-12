# ADR 0117 — Le dossier CAPA reflète ses actions, il ne se pilote plus

- **Statut** : Accepté
- **Date** : 2026-11-12
- **Owners** : @Couldevlop
- **Portée** : `apps/api-quality-engine` (`quality/capa` : `CapaService`,
  `CapaController`, `CapaTransition`), `apps/web` (`features/capa` :
  `CapaDetailComponent`, `CapaActionPanelComponent`, `CapaService`)
- **Révise** : **ADR 0088** sur le cycle de vie du dossier — les transitions
  `OPEN → IN_PROGRESS` et `IN_PROGRESS → RESOLVED` ne sont plus des gestes
  humains. Le reste de l'ADR 0088 tient.
- **Prolonge** : **ADR 0115** (un dossier porte plusieurs actions). C'est cette
  pluralité qui rend le statut du dossier calculable.
- **S'inscrit dans** : CLAUDE.md §4.2 (CAPA), §11.5 (traçabilité), ADR 0052
  (preuve d'action), ADR 0073 et ADR 0108 (vérification d'efficacité)

## Contexte

Le dossier CAPA avait son propre cycle de vie, avancé à la main par deux
boutons : **« Démarrer »** (`PATCH /cases/{id}/start`) et **« Résoudre »**
(`PATCH /cases/{id}/resolve`). Ses actions avaient le leur : *À faire → En
cours → Faite*.

Deux machines à états, donc — sauf qu'elles n'étaient pas indépendantes. Le
serveur refusait déjà la résolution tant qu'une action restait inachevée :

```java
if (!allDone) {
    throw new CapaStateException("All actions must be DONE before resolution");
}
```

et la clôture publiait le même verrou sous le code `ACTIONS_NOT_DONE`. Le bouton
« Résoudre » ne décidait donc rien : il demandait à un humain de recopier à la
main une conclusion que le serveur tirait déjà tout seul. « Démarrer » n'en
décidait pas davantage — commencer une action est l'acte, l'annoncer n'est
qu'un doublon.

Deux conséquences, l'une pour le pilote et l'autre pour le lecteur :

1. Le pilote qui terminait sa dernière action voyait son dossier rester
   « en cours ». Il fallait savoir qu'un second geste, ailleurs dans l'écran,
   attendait d'être posé — et les dossiers finis-mais-non-résolus s'accumulaient
   dans les registres.
2. La fiche écrivait le mot **« Statut »** deux fois, en tête pour le dossier et
   dans le bloc des actions, avec les mêmes valeurs affichées (*En cours*) pour
   deux réalités différentes. Personne ne distingue à la lecture deux colonnes
   homonymes dont l'une est la conséquence de l'autre.

## Décisions

### 1. Le statut du dossier est déduit de ses actions

`CapaService.refleterAvancement(CapaCase)` recalcule le statut après **toute**
écriture d'action — ajout, modification, suppression :

| Actions du dossier | Statut déduit |
| --- | --- |
| aucune, ou toutes encore *à faire* | `OPEN` |
| au moins une *commencée* | `IN_PROGRESS` |
| toutes *réalisées* | `RESOLVED` |

Un dossier **sans aucune action reste `OPEN`**, et non `RESOLVED` : « aucune
action inachevée » n'est pas « le travail est fait ». Ce sont précisément les
dossiers vides qui attendent qu'on écrive quoi faire.

`resolvedAt` suit le statut. Un dossier qui repart en traitement perd son
horodatage de résolution, sans quoi la fiche dirait à la fois « en cours » et
« résolu le 12 mars ».

### 2. `CLOSED` et `REJECTED` restent hors d'atteinte de la déduction

Ce sont les deux seules décisions humaines qui subsistent dans le cycle :

- **`CLOSED`** — le verdict de vérification d'efficacité (ADR 0108). Aucune
  action réalisée ne démontre à elle seule que le problème a cessé.
- **`REJECTED`** — l'abandon du dossier. Renoncer est une décision, pas une
  déduction.

Un dossier clos ne redevient donc pas « en cours » parce qu'on rouvre une de ses
actions. Le retour en traitement d'un dossier clos passe par le seul chemin qui
le consigne : une vérification non concluante (`EFFECTIVENESS_REJECTED` ou
`EFFECTIVENESS_PARTIAL`).

### 3. `PATCH /cases/{id}/start` et `/cases/{id}/resolve` sont supprimés

Les deux routes disparaissent du contrôleur, et `startCase` / `resolveCase` du
service. Les garder en les ignorant laisserait deux verbes documentés qui
mentent sur ce qu'ils font. Côté interface, les boutons « Démarrer » et
« Résoudre » disparaissent de la fiche.

Restent sur la fiche : **« Rejeter »** et **« Efficace — clôturer »**, les deux
décisions du point 2.

### 4. Une transition de journal distincte pour le retour en arrière

`CapaTransition.REOPENED` est ajoutée : *le dossier repart en traitement parce
que ses actions l'y ramènent*. Elle ne se confond pas avec
`EFFECTIVENESS_REJECTED` ni `EFFECTIVENESS_PARTIAL`, qui disent la même chose du
dossier pour une tout autre raison — là, une vérification a conclu que l'effet
n'était pas au rendez-vous ; ici personne n'a rien vérifié, le travail n'est
simplement plus fini. Les confondre ferait lire à l'auditeur un échec
d'efficacité là où il n'y a qu'une action de plus.

### 5. Chaque fait n'est écrit qu'à un seul endroit de la fiche

La déduction rend le partage lisible, et la fiche s'y range :

| Appartient au **dossier**, en tête | Appartient à l'**action**, en ligne de tableau |
| --- | --- |
| Statut (synthèse), Criticité, Source | Nature, Intitulé |
| Pilote, Échéance de clôture | Responsable, Échéance |
| Critère d'efficacité, Vérification | Avancement, Preuve |

Le **Type** du dossier disparaît de l'en-tête : depuis l'ADR 0115 un dossier
porte à la fois des correctives et des préventives, il ne peut plus avoir *un*
type. La nature vit sur chaque action.

Le pavé vertical qui dépliait chaque action sur six lignes est remplacé par le
tableau de `CapaActionPanelComponent`, auquel on ajoute la colonne **Preuve**
jusque-là propre à la fiche. Les deux niveaux de preuve restent distincts :
une pièce par **action** (elle atteste que ce geste a été fait, ADR 0052), et
jusqu'à dix pièces pour le **dossier** (elles démontrent que le problème a
cessé, ISO 9001 §10.2).

## Alternatives écartées

- **Garder les deux boutons et renommer le statut des actions en
  « Avancement ».** Cela traitait le symptôme — l'homonymie — en laissant la
  cause : un humain à qui l'on demande de confirmer ce que la machine sait déjà
  finit par oublier de le faire, et les registres gardent des dossiers finis
  marqués en cours.
- **Déduire aussi la clôture, quand toutes les actions sont prouvées.** C'est
  l'erreur exacte que l'ADR 0073 puis l'ADR 0108 ont écartée : des actions faites
  ne démontrent pas que le problème a cessé. La vérification d'efficacité est la
  seule chose qui le dit, et elle est humaine.
- **Laisser `/start` et `/resolve` en place sans effet.** Deux routes
  documentées qui ne changent plus rien : le premier intégrateur à les appeler
  conclurait que le serveur est en panne.

## Conséquences

- Le pilote a **un seul geste** à poser : faire avancer ses actions. Il ne peut
  plus oublier le second.
- Les registres cessent d'accumuler des dossiers achevés restés « en cours ».
- Les dossiers existants se remettent d'aplomb à la première écriture d'action ;
  aucune migration n'est nécessaire, le vocabulaire des statuts ne changeant pas.
- Un intégrateur qui appelait `/start` ou `/resolve` reçoit un 404 : la
  suppression est annoncée ici et dans le wiki technique.
- La fiche n'écrit plus aucun champ deux fois.

## Tests d'invariant

- `CapaServiceTest` : un dossier dont on commence la première action passe à
  `IN_PROGRESS` sans appel dédié ; dont on termine la dernière passe à
  `RESOLVED` avec son `resolvedAt` ; dont on rouvre une action revient à
  `IN_PROGRESS` avec `resolvedAt` à `null` et une entrée `REOPENED` au journal.
- `CapaServiceTest` : un dossier **sans action** reste `OPEN`.
- `CapaServiceTest` : un dossier `CLOSED` ou `REJECTED` ne bouge pas quand on
  touche à ses actions.
- `CapaControllerTest` : `/cases/{id}/start` et `/cases/{id}/resolve` ne sont
  plus routées.
- `capa-detail.component.spec.ts` : la fiche ne porte ni « Démarrer » ni
  « Résoudre », et n'affiche le Type du dossier nulle part.
- `capa-action-panel.component.spec.ts` : la colonne Preuve joint et retire une
  pièce sur la ligne visée, et ne touche pas aux preuves du dossier.

## Références

- CLAUDE.md §4.2 (CAPA), §11.5 (audit & traçabilité)
- ADR 0088 (une seule fenêtre de création, source verrouillée), révisé ici sur
  le cycle de vie
- ADR 0115 (une CAPA porte plusieurs actions, montrées en tableau)
- ADR 0052 (porteur et preuve d'action), ADR 0073 et ADR 0108 (vérification
  d'efficacité), ADR 0081 (chacun voit ce qui le concerne)
- ISO 9001:2015 §10.2 — Non-conformité et action corrective
