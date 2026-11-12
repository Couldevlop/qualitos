# ADR 0114 — La fiche d'écart rassemble l'analyse de cause et l'action corrective

- **Statut** : Accepté
- **Date** : 2026-11-12
- **Owners** : @Couldevlop
- **Portée** : `apps/api-quality-engine` (`quality/fivewhys` : `FiveWhysService`,
  `FiveWhysStepRepository`), `apps/web` (`features/ishikawa` :
  `ishikawa-branches.ts`, `IshikawaSharedModule` ; `features/five-whys` :
  `FiveWhysSharedModule` ; `features/capa` : `capa.labels.ts`,
  `CapaActionPanelComponent`, `CapaActionPanelModule` ; `features/nc` : fiche
  d'écart)
- **Complète** : ADR 0088 (« une CAPA EST son action »), ADR 0113 (l'équipe sur
  la fiche d'écart) et le rapport 8D. **Aucun des trois n'est révisé** — en
  particulier, rien ici ne rouvre la liste d'actions que l'ADR 0088 a fermée.

## Contexte

La fiche d'un écart portait les faits (quoi, où, quand, qui), et des **liens**
vers tout le reste : « Ouvrir le dossier » pour la CAPA, un onglet séparé pour
l'Ishikawa, un autre pour les 5 Pourquoi. Trois questions revenaient en revue
d'écart, et aucune ne se lisait sur place :

1. **Quelle cause a-t-on retenue ?** Il fallait ouvrir le diagramme dans un autre
   écran, revenir, et de mémoire comparer avec les faits de la fiche.
2. **Où en est l'action ?** « Voir la CAPA » emmenait ailleurs pour un seul
   renseignement — un statut — et on revenait en arrière pour la suite.
3. Conséquence : **on ne les posait plus.** L'écran qui sert à décider ne portait
   pas les éléments de la décision, et le rapport 8D — qui, lui, les rassemble en
   D4 et D5 — était le seul endroit où l'on voyait l'écart en entier. Un document
   produit en fin de dossier ne pilote rien pendant le dossier.

Un troisième défaut aggravait le premier : `FiveWhysService.findByNc` renvoyait
les chaînes **sans leurs étapes** (`List.of()`). La fiche aurait affiché des
cascades vides, ce qui se lit « aucune analyse » alors que l'analyse existe.

## Décision

**La fiche d'écart montre la cause et l'action, en lecture et en un geste.** Deux
cartes s'ajoutent, après les faits :

- **Analyse de cause** : chaque chaîne 5 Pourquoi et chaque diagramme Ishikawa
  rattachés à l'écart, dessinés — la cascade et l'arête, pas une liste de
  libellés —, avec un lien vers l'écran dédié pour les modifier.
- **Action corrective en cours** : l'action du dossier CAPA, son statut, et le
  geste qui la fait avancer d'un cran, sans quitter l'écart.

**Les figures sont les mêmes qu'ailleurs, jamais des jumelles.** Le dessin de
l'arête et celui de la cascade sortent des modules de leur méthode
(`IshikawaSharedModule`, `FiveWhysSharedModule` : les composants de figure, **sans
les routes**). Le rangement des causes par famille — l'ordre des M — devient une
fonction pure (`ishikawa-branches.ts`) partagée par l'écran du diagramme et la
fiche. Une seconde implémentation divergerait sur cet ordre, et **l'ordre est la
méthode**.

**Le serveur charge les étapes des chaînes en une requête.** `findByNc` groupe les
étapes par analyse (`findByAnalysisIdInAndTenantIdOrderByAnalysisIdAscPositionAsc`)
au lieu d'une requête par chaîne : la fiche en affiche plusieurs.

**Le panneau d'action est un module à part, pas le module CAPA.** `CapaModule`
porte ses routes (`RouterModule.forChild`) ; l'importer depuis la fiche d'écart
grefferait les écrans CAPA à une adresse qui n'est pas la leur. Le panneau ne
reçoit qu'un **identifiant** et charge le dossier lui-même : l'écran hôte n'a pas
à connaître la forme d'une CAPA.

**Le lien « Voir la CAPA » quitte l'en-tête pour la carte de l'action.** Il y était
le seul chemin vers le statut ; il est maintenant le chemin vers ce que la carte
ne montre pas — la preuve de l'action, l'historique, l'efficacité.

**Une panne se dit.** Si le dossier CAPA ne se lit pas, le panneau l'annonce et se
relance. Un panneau vide se lirait « aucune action » : l'information opposée.

### Ce que la décision ne fait pas

- **Elle n'ajoute pas de liste d'actions.** L'ADR 0088 tient : une CAPA EST son
  action. Aucun bouton n'en ajoute une seconde. Un dossier qui n'en a **pas** —
  ancien, ou ouvert par un automate — propose de définir la sienne **une fois**,
  dans la **même** fenêtre que la création, et le dit : sans action, il ne pourra
  pas être clôturé. Les dossiers antérieurs qui portent plusieurs actions les
  montrent toutes, sans possibilité d'en rajouter.
- **Elle ne déplace pas la preuve de l'action.** Déposer, retirer ou prévisualiser
  une pièce reste sur la fiche CAPA : un panneau qui ferait les deux ne serait
  lisible dans aucun des deux écrans.
- **Elle ne modifie pas les analyses depuis la fiche.** Les cartes sont en
  lecture ; on édite un diagramme dans son écran, qui a son outillage. Une
  édition à deux endroits se contredirait.
- **Elle ne touche à aucun droit.** Le geste d'avancement reste sous
  `capa.action.update`, la définition sous `capa.action.manage`, et le serveur
  tient la même garde : un dossier clos ou rejeté ne bouge plus.

## Alternatives écartées

- **Garder les liens et s'en tenir au rapport 8D.** C'est l'état d'avant : le
  document arrive après les décisions qu'il aurait dû éclairer.
- **Recopier les figures dans la fiche.** Deux implémentations du même dessin
  divergent, et l'ordre des familles de causes aurait fini par différer d'un écran
  à l'autre.
- **Importer `CapaModule` dans `NcModule`.** Les routes CAPA se seraient greffées
  sous l'adresse des écarts.
- **Passer le dossier CAPA déjà chargé au panneau.** La fiche d'écart aurait dû
  le charger, donc connaître la forme d'une CAPA, et le recharger après chaque
  avancement : deux écrans responsables du même objet.
- **Rendre les actions éditables en liste depuis l'écart.** Ce serait superséder
  l'ADR 0088 par un effet de bord d'écran, sans en reprendre le raisonnement.

## Conséquences

- ✅ La revue d'écart se tient sur un seul écran : faits, équipe, cause, action.
- ✅ Les chaînes 5 Pourquoi s'affichent avec leurs étapes (le défaut est corrigé),
  en **une** requête quel que soit leur nombre.
- ✅ Faire avancer l'action depuis l'écart relit la fiche : son statut dépend de
  celui de l'action côté serveur.
- ✅ Un dossier sans action le dit à l'endroit où on le lit, et se complète sur
  place.
- ⚠ La fiche d'écart s'allonge. Les cartes sont **conditionnelles** (pas de CAPA,
  pas de carte d'action ; pas d'analyse, un message explicite) et le panneau
  d'action charge son dossier **après** la fiche : l'écart s'affiche sans
  l'attendre.
- ⚠ Deux modules partagés de plus (`IshikawaSharedModule`, `FiveWhysSharedModule`)
  et un module de panneau. C'est le prix de la figure unique.

## Tests d'invariant

- `FiveWhysServiceTest` : les étapes reviennent avec leurs chaînes, dans l'ordre
  des positions, sur plusieurs analyses à la fois, et filtrées par client.
- `capa-action-panel.component.spec.ts` : le panneau ne reçoit qu'un identifiant
  et charge le dossier ; statut seul transmis à l'avancement ; aucun geste sur une
  action faite, un dossier clos ou sans le droit ; définition offerte **une seule
  fois** et seulement sans action ; panne annoncée puis relançable ; plusieurs
  actions d'un ancien dossier toutes affichées, sans bouton d'ajout.
- `nc-detail.cause-analysis.spec.ts` : cascades et arêtes dessinées pour chaque
  analyse rattachée, message explicite quand il n'y en a pas, lien vers l'écran
  dédié.
- `nc-detail.component.spec.ts` : le dossier est confié au panneau par son seul
  identifiant, et la fiche se relit quand le panneau annonce un changement ;
  l'identifiant brut ne s'affiche jamais.
- `ishikawa-branches` : l'ordre des familles et l'arbre parent → enfants sont
  ceux de l'écran du diagramme — une seule source.

## Références

- CLAUDE.md §3.5 (Ishikawa), §3.6 (référentiel transverse partagé), §4.2 (CAPA),
  §4.3 (NC).
- ADR 0088 — une CAPA est son action. ADR 0113 — l'équipe sur la fiche d'écart.
- `docs/wiki/modules/non-conformites.md`, `docs/wiki/modules/rapport-8d.md`.
