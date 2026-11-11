# Module Gage R&R — Répétabilité et reproductibilité des moyens de mesure

[← Retour à l'index](../README.md) · Route : **`/spc/gage-rr`** · Menu : *SPC › Gage R&R*

## À quoi sert ce module

Une étude **Gage R&R** mesure la part de variation qui vient **du système de mesure** et non du
procédé. C'est le préalable à toute étude de capabilité : un Cpk calculé avec un instrument dont
la dispersion n'est pas démontrée mesure l'instrument autant que la pièce.

Deux composantes sont séparées :

- **EV — répétabilité** (*Equipment Variation*) : le même opérateur, la même pièce, le même
  instrument, plusieurs essais. C'est la dispersion du moyen lui-même.
- **AV — reproductibilité** (*Appraiser Variation*) : plusieurs opérateurs sur les mêmes pièces.
  C'est la dispersion due aux personnes et à la méthode.

Le module calcule les deux par la **méthode moyenne et étendue** et par **ANOVA**, et rend la
décision AIAG.

## Parcours pas à pas

### 1. Ouvrir le registre

`/spc/gage-rr` liste les études R&R réalisées, avec pour chacune son plan, son %GRR, son ndc et
sa décision. Cliquer sur une ligne rouvre l'étude.

### 2. Définir le plan d'étude

Un plan R&R se décrit par trois nombres :

| Paramètre | Valeurs acceptées |
|---|---|
| **k** — nombre d'opérateurs | 2 ou 3 |
| **n** — nombre de pièces | 2 à 10 |
| **r** — nombre d'essais par pièce | 2 ou 3 |

Ces bornes sont celles de la table de constantes AIAG (K1, K2, K3, D4). Un plan qui en sort —
4 opérateurs, 15 pièces — est **refusé** par le serveur, et non silencieusement accepté : il
demanderait d'autres constantes.

Vous choisissez aussi le **multiplicateur σ** : 5,15 (99 % de la dispersion, valeur historique
des constantes) ou 6. Le %GRR rapporté à la variation totale ne dépend pas de ce choix ; le
%GRR rapporté à la tolérance, si.

### 3. Désigner les personnes

Le **responsable de l'étude** et **chaque opérateur** se choisissent dans l'annuaire de votre
organisation. Les opérateurs sont exactement k, tous distincts. Aucun nom n'est tapé en texte
libre, aucun identifiant technique n'est affiché.

### 4. Saisir la grille

La grille reprend la disposition d'un relevé R&R classique : pour chaque opérateur, ses essais
par pièce, puis la moyenne et l'étendue de chaque pièce ; la dernière ligne donne les moyennes
par essai, X̄ et R̄ de chaque opérateur, et la moyenne générale. Un bloc copié depuis un tableur
se colle d'un coup.

Une **étendue au-dessus de UCL<sub>R</sub>** est marquée en rouge : c'est un relevé à reprendre
avant d'interpréter quoi que ce soit.

La grille doit être **complète**, et ne pas être faite de mesures toutes identiques — sans
variation totale, l'étude n'a pas de sens et elle est refusée. Si vous réduisez le plan, les
mesures saisies au plan le plus large ne sont pas perdues.

Le bouton **« Charger l'exemple »** remplit la fiche avec un jeu de référence (arbre AX-210,
10,00 ± 0,10 mm, 3 opérateurs × 10 pièces × 3 essais, 5,15 σ), sans les personnes, et ne remplace
des mesures déjà saisies qu'après confirmation. Rien n'est enregistré avant que vous
n'enregistriez l'étude.

### 5. Lire les résultats

Les résultats déroulent les **quatre étapes** du calcul, chacune avec sa formule et son
application numérique :

1. X̄ et R̄ de chaque opérateur, X̄diff, R̄̄, Rp (étendue des moyennes de pièces), UCL<sub>R</sub>.
2. Les constantes retenues : K1 (pour r), K2 (pour k), K3 (pour n), D4.
3. **EV**, **AV**, **GRR** = √(EV² + AV²), **PV** (variation des pièces), **TV** (variation
   totale) — chacune exprimée en % de la variation totale, en % de contribution (rapport des
   variances) et en % de la tolérance.
4. **ndc** (*number of distinct categories*, nombre de catégories distinctes) et la décision.

Puis le **tableau d'ANOVA** à deux facteurs croisés avec répétitions : sommes des carrés
(pièces, opérateurs, interaction, répétabilité, total), carrés moyens, et tests F. Quand
l'interaction n'est pas significative (**p > 0,25**, seuil AIAG), elle est **regroupée** avec la
répétabilité et un second tableau est calculé sur l'erreur regroupée — l'écran affiche les deux.
Les composantes de variance en découlent, avec les mêmes pourcentages, le même ndc et la même
décision que la première méthode. La **comparaison des deux méthodes** est présentée côte à
côte : un écart important entre elles est en soi une information.

### La décision AIAG

| %GRR | Décision |
|---|---|
| < 10 % | système de mesure **acceptable** |
| 10 à 30 % | acceptable **sous conditions** (selon l'importance de l'application, le coût, l'historique) |
| > 30 % | **non acceptable** — le système de mesure doit être amélioré |

À quoi s'ajoutent deux lectures :

- **ndc ≥ 5** : la discrimination est suffisante, le système sait distinguer les pièces entre
  elles. En dessous, il classe mal.
- **Le poste dominant** : EV ou AV. Si EV domine, c'est l'instrument (usure, résolution,
  montage) ; si AV domine, c'est la méthode et la formation des opérateurs.

### Les quatre graphiques

- **Composantes de la variation**, avec les seuils de 10 % et 30 % tracés.
- **Carte R par opérateur** avec UCL<sub>R</sub> : des points hors limites signalent un
  opérateur qui ne mesure pas de façon répétable.
- **Carte X̄ par opérateur** (X̿ ± A2 R̄̄) : ici, **un bon système a la plupart des points hors
  limites** — cela signifie que la variation entre pièces domine largement celle du moyen.
- **Mesures par pièce**, pour voir d'un coup d'œil les pièces mal mesurées.

## Qui peut faire quoi

- **Lire** le registre et les fiches : tout membre de l'organisation.
- **Créer, réviser, supprimer** une étude : le droit `spc.manage`.
- Le module **DMAIC** doit être actif, comme pour les études de capabilité.

Création, révision et suppression sont journalisées avec le plan, le multiplicateur, les %GRR,
le ndc et la décision — **ni mesure, ni nom de personne**.

## Bonnes pratiques

- **Choisissez les pièces pour couvrir l'étendue de production**, pas dix pièces bonnes : PV
  sous-estimée gonfle artificiellement le %GRR.
- **Mesurez en aveugle et dans un ordre mélangé** : un opérateur qui reconnaît la pièce reproduit
  sa mesure précédente, et l'étude conclut à une répétabilité qui n'existe pas.
- **Traitez l'étude avant la capabilité** : faites le R&R, puis l'[étude de capabilité](spc.md).
- **Un %GRR > 30 % est une non-conformité du moyen** : ouvrez une [CAPA](capa.md) et, si
  l'instrument est concerné, vérifiez son plan de calibration.
