# ADR 0122 — Un refus se dit avant le clic, et une cause se trace

- **Statut** : Accepté
- **Date** : 2026-11-15
- **Owners** : @Couldevlop
- **Portée** : `apps/api-quality-engine` (`quality/nonconformity` : `NcBlockerCode`
  (nouveau), `NcDto.Blocker` (nouveau), `NcService` ; `quality/nonconformity/eightd` :
  `EightDSnapshot.Figure`/`Node`/`FigureKind` (nouveaux), `EightDSnapshotAssembler`,
  `PdfBoxEightDRenderAdapter`) et `apps/web` (`features/nc` : fiche d'écart).
  Aucun changement de schéma.
- **Prolonge** : **ADR 0121** (les gardes de la résolution et de la clôture d'un
  écart) — qui a posé les refus ; celui-ci les rend lisibles. Reprend le parti de
  `ClosureBlockerCode` (clôture d'un dossier CAPA) pour le registre des écarts.
- **S'inscrit dans** : CLAUDE.md §4.3 (NC), §15.1 (sobriété, accessibilité),
  §19 (documentation utile à l'écran)

## Contexte

Deux défauts d'expression, sur deux écrans qui se lisent en revue.

**1. Le refus arrivait après le clic.** L'écart ne se résout pas tant que son
dossier CAPA porte des actions à mener, ni ne se clôt avant le verdict
d'efficacité (ADR 0121, ADR 0108). Ces règles sont justes. Mais l'utilisateur
les découvrait sous la forme d'un `409` portant une phrase anglaise construite
par le service — « Non-conformity cannot be resolved while 2 CAPA case(s) still
have actions to carry out: … » —, au moment où il croyait en avoir fini. Le
bouton, lui, ne disait rien : ni qu'il échouerait, ni ce qu'il fallait faire pour
l'allumer.

**2. Le 8D énumérait ce qu'il aurait dû tracer.** Le D4 recopiait l'Ishikawa et
les 5 pourquoi ligne à ligne : « MATERIEL : Joint usé », « Why 3 : … ». Un
rapport 8D se lit en revue, souvent debout, et cette énumération ne montrait ni
les branches ni la descente — c'est-à-dire exactement ce que ces deux outils
servent à voir. À l'écran, les figures interactives existaient depuis longtemps ;
le document **généré**, celui qu'on envoie au client, n'en avait aucune.

## Décision

### Les motifs sont énumérés, pas rédigés par le serveur

Un nouvel enum `NcBlockerCode` — `CAPA_ACTIONS_TO_CARRY_OUT` (empêche de
résoudre), `CAPA_CASES_UNSETTLED` (empêche de clore) — voyage dans la fiche avec
son **décompte**. L'écran rédige la phrase, en six langues, et elle nomme **le
geste qui débloque** : « Terminez ses actions pour pouvoir résoudre cet écart ».
Le bouton que le serveur refuserait est éteint, et l'infobulle redit le motif.

Trois choix dans cette décision :

- **Un code, pas une phrase.** Un code se traduit, se teste et se compte ; une
  phrase anglaise construite en Java ne fait aucun des trois.
- **Seule l'étape courante est instruite.** Un écart `ACTION_DEFINED` porte le
  motif de la résolution ; `RESOLVED`, celui de la clôture ; ailleurs, aucun.
  Reprocher à l'utilisateur une étape qu'on ne lui propose pas encore serait
  incompréhensible, et chaque motif coûte des requêtes.
- **Les lignes de liste n'en portent pas.** Les calculer coûterait deux requêtes
  par ligne pour une information qu'on n'y affiche pas.

Le serveur reste l'autorité : il refuse de toute façon. Éteindre le bouton évite
seulement de proposer un geste qu'on sait repoussé.

### Les figures entrent dans l'instantané scellé, comme DONNÉES

`EightDSnapshot.Section` reçoit une liste de `Figure`, décrite par ses `Node`
(groupe, libellé, rang) et sa forme (`FISHBONE`, `WHY_CHAIN`). Le D4 porte
désormais ces figures **à la place** de son énumération : le tracé remplace le
texte, il ne s'y ajoute pas — répéter l'un sous l'autre doublerait la page sans
rien apprendre. Seule la cause racine portée par la fiche de l'écart reste en
texte : aucune figure ne la dit.

Le rendu PDF trace l'arête (épine horizontale, branches alternées par catégorie,
énoncé dans la tête) et l'échelle des pourquoi (cadres reliés, rang nommé, cause
racine distinguée par un filet indigo).

**Ce sont des données, pas une image.** C'est ce qui préserve l'invariant central
du module : le rendu reste une **fonction pure** de ce qui est stocké, donc
l'empreinte scellée reste vérifiable. C'est la même raison qui fait stocker du
texte plutôt que des identifiants.

Et le champ est **tolérant à l'absence** : un rapport émis avant les figures le
relit sans elles, et rend exactement les mêmes octets qu'au jour de son sceau.

## Alternatives écartées

- **Afficher le message du serveur tel quel.** C'est ce qui se passait : une
  phrase anglaise, technique, non traduite, et qui dit l'état sans dire le geste.
- **Calculer les motifs sur chaque ligne de liste.** Deux requêtes par ligne pour
  une information invisible.
- **Laisser le bouton actif et compter sur le refus.** Un geste proposé puis
  repoussé est une promesse non tenue ; et l'encart serait lu après l'échec.
- **Rendre les figures en image (PNG/SVG) côté serveur.** Une image dans
  l'instantané ferait grossir le JSON de chaque rapport, et surtout ferait
  dépendre l'empreinte d'un moteur de rendu graphique : la moindre mise à jour de
  bibliothèque casserait la vérification de tous les rapports passés.
- **Lire les figures en direct au moment du rendu.** Le rapport ne dirait plus,
  dans dix ans, ce qu'il disait le jour de la clôture — l'Ishikawa aura été
  complété depuis.
- **Garder le texte sous la figure.** Deux fois la même information, sur un
  document que personne ne lit en entier.

## Conséquences

- ✅ Le bouton éteint s'explique, et la phrase dit quoi faire.
- ✅ Le 8D envoyé au client montre l'arête et la descente, pas une liste.
- ✅ Les rapports déjà scellés restent vérifiables au bit près.
- ⚠ Les figures du PDF coupent les étiquettes longues (26 caractères par cause) :
  une étiquette de branche doit se lire d'un coup d'œil, et le texte complet
  reste sur la fiche de l'écart.
- ⚠ Les sous-causes ne sont pas hiérarchisées dans l'arête : la source les livre
  à plat, et inventer une hiérarchie absente dessinerait un diagramme que
  personne n'a construit.
- ⚠ L'écran du rapport continue de tracer ses figures depuis les données
  **vives** (service Ishikawa / 5 pourquoi), pas depuis l'instantané. Un rapport
  émis dont l'Ishikawa a été supprimé depuis montrera donc ses figures dans le
  PDF scellé, et pas à l'écran. Dette assumée, consignée ici.

## Tests d'invariant

- `NcServiceTest` : la fiche porte le motif de SON étape, avec son décompte ;
  rien avant l'étape, rien après la clôture ; la liste n'interroge pas les
  dossiers.
- `nc-detail.blocages.spec.ts` : la phrase nomme le geste, le singulier est
  respecté, le bouton s'éteint, et un code inconnu reste compréhensible.
- `EightDSnapshotAssemblerTest` : le D4 porte deux figures et une seule ligne de
  texte ; une descente sans conclusion n'a pas de nœud racine ; un Ishikawa vide
  donne une figure sans nœud mais une discipline servie.
- `PdfBoxEightDRenderAdapterTest` : les branches et les rangs sortent dans le
  texte du PDF ; une figure vide le dit ; **deux rendus donnent les mêmes
  octets** ; et surtout, **un instantané sans figure rend les mêmes octets
  qu'avant la décision**.

## Références

- CLAUDE.md §4.3, §15.1
- ADR 0108 (verdict d'efficacité), ADR 0121 (gardes de l'écart), ADR 0062
  (une empreinte ne se calcule que sur des valeurs stockables)
