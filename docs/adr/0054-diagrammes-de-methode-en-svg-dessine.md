# ADR 0054 — Les diagrammes de méthode (Ishikawa, 5 Pourquoi) sont dessinés en SVG, pas confiés à JointJS

- **Statut** : Accepté
- **Date** : 2026-08-10
- **Owners** : @Couldevlop
- **Remet en cause** : CLAUDE.md §3.5 et §7.2 (« Diagrammes interactifs → JointJS / GoJS »)

## Contexte

Les modules Ishikawa et 5 Pourquoi affichaient leur contenu sous forme de **listes** :
une carte par catégorie de causes avec ses puces pour l'Ishikawa, une liste ordonnée de
pourquoi pour les 5 Pourquoi. Tout y était — libellés complets, hiérarchie, commandes —
et pourtant l'essentiel manquait.

Ces deux méthodes ne sont pas des façons de ranger de l'information : ce sont des
**formes**. L'arête de poisson affirme que toutes les familles de causes convergent vers
un seul effet ; une liste de six cartes juxtaposées ne l'affirme pas. Les 5 Pourquoi
affirment une **descente**, du symptôme constaté vers la racine ; une liste à puces empile
des lignes équivalentes et perd exactement ce que la méthode dit. Un qualiticien reconnaît
ces figures au premier coup d'œil — c'est même leur unique raison d'exister par rapport à
un tableau.

CLAUDE.md tranchait le sujet : **JointJS** (ou GoJS) pour les « diagrammes interactifs ».
La bibliothèque n'a jamais été introduite ; le besoin, lui, s'est précisé en le regardant
de près, et il n'est pas celui qu'un éditeur de graphes sert. § 22-7 impose alors un ADR.

## Décision

1. **Les figures de méthode sont dessinées en SVG en ligne**, la géométrie étant calculée
   en TypeScript. Pas de JointJS, pas de GoJS, pas d'ECharts.
2. **Géométrie séparée du rendu** — un fichier `*.layout.ts` par figure, fonction pure
   (entrées → coordonnées), testable sans DOM ni navigateur. Le composant ne fait que
   projeter le résultat dans le gabarit.
3. **Rien n'est dessiné en dur** — la cascade s'adapte de 3 à 7 pourquoi, l'arête aux
   modes 6M / 7M / 8M. Aucune constante « cinq » ni « six » dans le tracé.
4. **La figure illustre, la liste énonce.** Chaque diagramme est accompagné, dans la même
   page, de la liste ou des cartes qu'il double : elles portent le texte intégral (jamais
   tronqué), la hiérarchie complète et **toutes** les commandes. Le SVG porte `role="img"`,
   un `<title>` et un `<desc>` ; son contenu interne reste opaque aux lecteurs d'écran.
5. **Sous-causes non tracées sur l'arête** : le dessin porte le premier niveau et signale
   par un compteur (« +3 ») qu'une cause en cache d'autres.
6. **RTL en une passe** — la géométrie est calculée dans un repère « sens de lecture
   naturel », puis miroitée (abscisses + ancrage du texte). Aucune branche `if (rtl)`
   dispersée dans le tracé.
7. **Couleurs par jetons** — rampe `--qos-chain-1..4` (symptôme → racine), déclinée clair
   et sombre.
8. **ECharts reste la règle pour les graphes de données** (séries, axes, KPIs) et
   **bpmn-js pour BPMN**. La présente décision ne porte que sur les figures de méthode.

## Justification

**Pourquoi pas JointJS.** JointJS est un éditeur de graphes : nœuds déplaçables, liens
routés, sérialisation, historique. Ce qu'il apporte — la manipulation libre — est
précisément ce qu'on ne veut pas ici. Un Ishikawa n'est pas un graphe libre : ses branches
sont les catégories du mode retenu (6M/7M/8M), leur position se déduit du modèle, et
laisser l'utilisateur les déplacer produirait des diagrammes qui ne se ressemblent plus
d'un dossier à l'autre — dans un référentiel qualité, la comparabilité vaut mieux que la
liberté de mise en page. On paierait en outre ~150 ko de bundle sur des modules chargés
paresseusement, plus une dépendance à suivre en CVE, pour obtenir un tracé qu'il faudrait
de toute façon contraindre. La structure d'édition existe déjà, ailleurs et mieux : dans
les cartes, où l'on ajoute, renomme et supprime les causes.

**Pourquoi pas ECharts.** Il n'y a ni série, ni axe, ni échelle. Détourner un graphique en
diagramme conventionnel se paie en contorsions à chaque évolution.

**Pourquoi une géométrie pure et séparée.** C'est ce qui rend la figure testable : les
tests de `*.layout.ts` vérifient qu'une branche vide reste dessinée, que 8 catégories
n'empiètent pas sur la tête, que le miroir RTL est bien une involution — sans navigateur
et sans capture d'écran. Le composant devient trivial, donc sûr.

**Pourquoi tronquer dans le dessin.** SVG ne replie pas le texte : un `<text>` déborde de
sa boîte sans rien dire, et la hauteur d'un encart — qui commande toute la géométrie
suivante — ne peut donc pas se déduire du contenu. `<foreignObject>` rendrait du vrai HTML
et donc un vrai repli, mais sa hauteur n'est connue qu'**après** rendu (impossible d'en
tirer une géométrie calculée en amont) et il se comporte mal à l'export image. On découpe
donc en amont sur une estimation de largeur de caractère : déterministe, testable, hauteur
connue avant le premier trait. Le découpage est approximatif — un « W » est plus large
qu'un « i » — et c'est précisément pourquoi le texte intégral reste dans la liste voisine.

**Pourquoi `role="img"` plutôt qu'un SVG « accessible ».** Une arête de poisson lue trait
par trait n'a aucun sens. Un empilement de `<text>` sans ordre sémantique serait pire que
le silence. L'équivalent textuel existe déjà et il est meilleur : c'est la liste, qui porte
le texte entier, la hiérarchie dépliée et les commandes.

**Pourquoi une rampe de couleurs dédiée.** La progression du modèle papier (rouge →
jaune → vert) porte du sens : elle dit la descente vers la racine. Mais le jaune franc
tombe à ~1,8:1 sur blanc, sous le 3:1 qu'exige WCAG 1.4.11 pour un objet graphique
porteur de sens. La progression est conservée, chaque teinte descendue au niveau qui passe
le contraste — et remontée en thème sombre, où le rapport s'inverse.

## Conséquences

- ✅ Les deux méthodes s'affichent enfin sous la forme qui les définit, sans un octet de
  dépendance supplémentaire.
- ✅ Le tracé est testé comme du code ordinaire (fonctions pures), pas comme du rendu.
- ✅ Le nombre de branches et de pourquoi reste ouvert : 6M/7M/8M et 3 à 7 pourquoi sans
  toucher au tracé.
- ⚠ **Figures en lecture seule** : on ne déplace rien, on n'édite pas dans le dessin.
  L'édition reste dans les cartes et la liste. Assumé (cf. Justification).
- ⚠ **Troncature approximative** des libellés dans le dessin (police proportionnelle).
  Compensée par la liste, qui n'en tronque aucun.
- ⚠ **Sous-causes réduites à un compteur** sur l'arête. La hiérarchie complète reste dans
  les cartes.
- ⚠ Toute nouvelle figure de méthode devra suivre la même trame (`*.layout.ts` pur +
  composant mince) plutôt que d'introduire une bibliothèque — sans quoi la décision se
  dissout.

## Tests d'invariant

- `svg-text.spec.ts` — repli et troncature : mot plus long qu'une ligne, ellipse sur la
  dernière ligne, texte vide, bornes absurdes.
- `ishikawa-fishbone.layout.spec.ts` — 6/7/8 branches, branche vide toujours dessinée,
  compteur de sous-causes, tête basculée à gauche en RTL avec boîte et ordonnées
  inchangées, tracé entièrement contenu dans la boîte annoncée — miroité compris.
- `five-whys-cascade.layout.spec.ts` — chaînes de 3 à 7 pourquoi, étapes reçues en
  désordre remises en rang, cause racine fermant la descente, chaîne vide, rampe répartie
  sur la longueur réelle, encarts sans chevauchement quelle que soit la longueur du texte.
- `ishikawa-detail` / `five-whys-detail` (specs d'écran) — le composant de figure est
  **déclaré**, jamais bouchonné : une régression de tracé doit casser un test d'écran.
  Les specs vérifient que la liste subsiste à côté du dessin.
- Le gate de couverture front (karma, bloquant) couvre ces fichiers comme les autres.

## Références

- CLAUDE.md §3.5 (Ishikawa), §7.2 (bibliothèques graphiques), §15.1 (accessibilité
  WCAG 2.2 AA), §22-7 (toute remise en cause d'un choix tranché exige un ADR)
- WCAG 2.2 — 1.4.11 (contraste des objets graphiques), 1.1.1 (équivalent textuel)
- `docs/web-design-system.md` § « Diagrammes de méthode » — la règle d'usage au quotidien
- [ADR 0002](./0002-angular-ngmodules-no-standalone.md) — composants déclarés en NgModule
