# ADR 0107 — Toute taille d'échantillon, tout nombre de sous-groupes : deux cartes et une borne dite

- **Statut** : Accepté
- **Date** : 2026-11-06
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`spc.capability` : `SpcConstants`,
  `StdevConstants`, `ChartFactors`, `CapabilityCalculation`, `CapabilityStudy`,
  `SpcStudyDto`, `SpcStudyWebDto`), écran `/spc/cartes/:id`
  (`features/spc/capability`)
- **Complète** : ADR 0096 (études de capabilité SPC, cartes X̄ – R, Cp / Cpk),
  ADR 0018 (règles de Nelson)

## Contexte

La fiche d'étude de capabilité (ADR 0096) ne proposait qu'un seul réglage de
taille, « Taille du sous-groupe (n) », dans une liste de 2 à 10 — la borne de
la table de d2, A2, D3 et D4 reprise du classeur de référence. Trois gênes
remontaient du terrain :

1. **Un seul libellé pour deux notions.** « Taille du sous-groupe » désigne en
   fait le nombre de mesures d'un prélèvement — ce que l'on appelle couramment
   la *taille d'échantillon*. Le nombre de *sous-groupes*, lui, est le nombre de
   prélèvements qui composent l'étude. L'écran les confondait : la liste réglait
   les lignes de la grille, et les colonnes se réglaient par deux boutons
   « + » / « − » posés au-dessus de la grille, sans libellé.
2. **Une étude de plus de dix mesures par prélèvement était refusée.** Le
   serveur répondait 400 (`@Max(10)`) ; l'écran ne proposait rien au-delà.
3. **Le nombre de sous-groupes plafonnait à quarante**, et ne se réglait qu'un
   clic à la fois.

La demande était d'accepter « n'importe quel nombre de SG et de sample ». Prise
au pied de la lettre côté calcul, elle produit un faux : les constantes de la
carte X̄ – R ne sont tabulées que jusqu'à dix, et lever `@Max(10)` sans rien
changer d'autre aurait fait diviser R̄ par un d2 inexistant. Un Cpk sorti d'un
`null` ou d'un zéro est plus dangereux qu'un refus, parce qu'il a l'air d'un
résultat.

## Décisions

### 1. Deux cartes, choisies par la taille d'échantillon

| n (mesures par prélèvement) | Carte | σ̂ | Limites X̄ | Carte de dispersion |
| --------------------------- | ----- | -- | ---------- | ------------------- |
| 2 à 10 | **X̄ – R** | R̄ / d2 | X̿ ± A2 × R̄ | D3 × R̄ … D4 × R̄ |
| 11 à 25 | **X̄ – S** | s̄ / c4 | X̿ ± A3 × s̄ | B3 × s̄ … B4 × s̄ |
| hors 2 à 25 | **aucune** | — | — | refus explicite |

La frontière à dix n'est pas un arrondi : l'étendue n'utilise que la plus petite
et la plus grande des `n` mesures et jette l'information du milieu, si bien que
son efficacité comme estimateur de σ s'effondre quand le sous-groupe grossit.
C'est pourquoi les tables publiées de d2, A2, D3 et D4 s'arrêtent là où leur
usage cesse d'être recommandé. Au-delà, l'écart-type d'échantillon s (diviseur
`n − 1`) utilise toutes les mesures et reste efficace.

Les valeurs de d2, A2, D3 et D4 ne changent pas d'un chiffre : une étude déjà
enregistrée se recalcule exactement comme avant, et le chemin doré du classeur
(`Cp` 1,840, `Cpk` 1,769) reste le banc de référence.

### 2. Au-delà de vingt-cinq, on refuse, et on dit pourquoi

Vingt-cinq est la dernière ligne des tables publiées (Montgomery,
*Introduction to Statistical Quality Control*, annexe VI ; ASTM STP-15D). La
formule de c4 se prolongerait sans broncher, mais une carte de contrôle dont
personne ne peut recouper les facteurs dans une table n'est pas auditable — et
un prélèvement de plus de vingt-cinq pièces relève du contrôle à 100 %, pas de
l'échantillonnage.

Le refus est dit des deux côtés :

- **serveur** : `ChartFactors.forSubgroupSize` lève une
  `SpcValidationException` sur le champ `subgroupSize` (422), et
  `@Max(25)` arrête la requête avant le domaine (400) ;
- **écran** : la liste déroulante ne propose pas au-delà, et si une fiche
  arrivait malgré tout avec une taille hors domaine, l'écran affiche
  « Taille d'échantillon hors domaine … Rien n'est calculé, pour ne pas
  afficher un indice faux », ne calcule rien et refuse d'enregistrer.

### 3. c4, A3, B3 et B4 sont calculés, pas recopiés

Contrairement à d2, A2, D3, D4 — repris tels quels du classeur avec ses
arrondis, pour que l'écran rende le même Cpk que le tableur qu'il remplace —
les facteurs de la carte S ont une définition fermée exacte :

```
c4(n) = √(2 / (n − 1)) × Γ(n / 2) / Γ((n − 1) / 2)
A3(n) = 3 / (c4 √n)
B3(n) = max(0, 1 − 3 √(1 − c4²) / c4)
B4(n) = 1 + 3 √(1 − c4²) / c4
```

Les tables publiées n'en sont que l'impression arrondie. Les calculer supprime
le risque de transcription sur vingt-quatre lignes de quatre colonnes. Γ ne
demande aucune bibliothèque : `n` est entier, donc `n / 2` est entier ou
demi-entier, et Γ se déroule par récurrence depuis Γ(1) = 1 et Γ(1/2) = √π —
exactement la même fonction en Java et en TypeScript, donc le même Cpk de part
et d'autre.

**Le banc le prouve** : `StdevConstantsTest#tablePubliee` et le test
`spc-capability.calc.spec.ts` « calcule c4, A3, B3 et B4 conformément à la table
publiée » comparent les valeurs calculées aux valeurs imprimées pour
n = 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20 et 25, à la tolérance de
l'impression (10⁻³). Un chemin doré supplémentaire, calculé à la main des deux
côtés (`CapabilityCalculationTest#cheminDoreCarteS`), vérifie la carte X̄ – S
de bout en bout : deux sous-groupes de onze mesures construits pour que
s = 1 exactement, donc σ̂ = 1 / c4(11) = 1,0252729 et Cp = Cpk = 1,9507002.

### 4. Le nombre de sous-groupes monte à cent, et se règle dans une liste

Le plafond passe de quarante à cent. Il n'a rien de statistique — aucune formule
ne se dégrade quand les relevés s'accumulent, au contraire : la règle de l'art
(« vingt à vingt-cinq sous-groupes ») est un **plancher de confiance**, que
l'écran continue de rappeler sans l'imposer. Le plafond ne borne que la largeur
de la grille affichable et la taille du corps de requête (cent sous-groupes de
vingt-cinq mesures = 2 500 nombres).

Les deux boutons « + » / « − » laissent place à une **liste déroulante
libellée**, qui va un par un jusqu'à vingt — la zone où choisir exactement son
nombre a un sens — puis par paliers (25, 30, 40, 50, 60, 80, 100). Une largeur
intermédiaire reste atteignable : un bloc collé depuis un tableur apporte sa
largeur, et l'écran l'ajoute alors à la liste pour que la valeur s'affiche.

La garde du « − » est conservée, et élargie : **réduire sur des sous-groupes
saisis demande confirmation**, et refuser remet la liste sur l'ancienne valeur.
On ne jette pas des mesures d'un clic.

### 5. Le libellé dit ce qu'il règle

« Taille du sous-groupe (n) » devient « **Taille d'échantillon (n)** », cohérent
avec l'en-tête de ligne « Échantillon » déjà présent dans la grille, et un
second champ, « **Nombre de sous-groupes** », règle les colonnes. Un sous-texte
dit sous le premier quelle carte le choix entraîne (« Carte X̄ – R (σ = R̄ / d2),
jusqu'à 10 mesures »), parce que l'utilisateur choisit un nombre de mesures, pas
une carte, mais doit voir laquelle en découle.

Partout où l'écran nommait la carte R en dur — en-tête de ligne, titre du
graphique, table des constantes, libellés de formules, carte « Sigma » — le
symbole suit désormais la carte retenue (R / S, R̄ / s̄, d2 / c4, A2 / A3,
D3 / B3, D4 / B4), et la table affichée est celle de la carte en vigueur :
montrer d2 pendant qu'on divise par c4 ferait lire les mauvais facteurs.

### 6. Le contrat d'écriture ne bouge pas ; les facteurs rendus changent de nom

`subgroupSize`, `measurements`, `cp`, `cpk` : inchangés. Seules les bornes
s'élargissent (`subgroupSize` 2..25, lignes 2..25, colonnes 2..100).

En revanche, dans `CalculationView` — la charge d'affichage du panneau des
formules, lue par ce seul écran — `d2`, `a2`, `d3`, `d4` deviennent
`dispersionChart`, `sigmaDivisor`, `meanFactor`, `dispersionLower`,
`dispersionUpper`. Dès qu'une étude peut se conduire sur une carte S, ces
quatre nombres valent c4, A3, B3 et B4 une fois sur deux : les anciens noms
auraient menti, et un nom qui ment sur un chiffre de capabilité est un défaut,
pas un détail.

Les composantes `subgroupRanges`, `meanRange`, `rangeUcl` et `rangeLcl` gardent
leur nom et portent la dispersion de la carte retenue — l'étendue, ou
l'écart-type d'échantillon. Le nom est hérité ; `dispersionChart` dit de quoi il
s'agit.

### 7. La première colonne de la grille est figée pour de bon

`position: sticky; left: 0` était déjà posé, mais la colonne se lisait **à
travers** : son fond venait de `--qos-bg-subtle`, une teinte translucide
(`rgba(…, 0.025)`), et les cellules jaunes passaient dessous au défilement. La
teinte est désormais empilée sur une base opaque (`--qos-bg-elevated`), en clair
comme en sombre, et une ombre portée à droite montre que la colonne est figée.
`border-collapse: collapse` abandonnant la bordure d'une cellule figée, elle est
redessinée en ombre interne. L'en-tête de cette colonne passe devant les
en-têtes qui défilent (`z-index` 3 contre 2).

Avec cent sous-groupes possibles, savoir si l'on lit l'échantillon 3 ou la ligne
R est la seule chose qui rend le défilement horizontal utilisable : la colonne
figée n'est pas un confort, c'est ce qui rend la décision 4 exploitable.

## Conséquences

- Une étude de douze, seize ou vingt-cinq mesures par prélèvement se mène
  désormais, sur la bonne carte, avec les bons facteurs.
- Une campagne de cent sous-groupes se règle d'un clic.
- Aucune étude enregistrée ne change de valeur : les constantes de la carte R
  sont identiques au chiffre près.
- Le panneau des formules d'une étude existante garde le même contenu ; seuls
  les noms des champs rendus par l'API changent, et ils ne sont lus que par cet
  écran.
- Une taille hors domaine ne produit plus rien — ni 400 muet, ni chiffre faux :
  un message qui dit la borne et la raison.
