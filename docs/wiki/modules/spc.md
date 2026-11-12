# Module SPC — Études de capabilité, cartes de contrôle & règles de Nelson

[← Retour à l'index](../README.md) · Routes : **`/spc/cartes`**, **`/spc`** · Menu : *SPC › Cartes de contrôle*

## À quoi sert ce module

Le **SPC** (*Statistical Process Control*, maîtrise statistique des procédés) répond à deux
questions différentes, et QualitOS les sépare :

| Question | Écran | Route |
|---|---|---|
| Mon procédé est-il **capable** de tenir ses tolérances ? | Registre des études de capabilité | `/spc/cartes` |
| Mon système de mesure est-il **fiable** ? | [Études Gage R&R](gage-rr.md) | `/spc/gage-rr` |
| Cette série de mesures montre-t-elle un **signal** ? | Détection d'anomalies SPC | `/spc` |

La section **SPC** du menu donne accès aux deux premiers. L'écran `/spc` n'a pas d'entrée de
menu : il s'ouvre depuis l'analyse d'un KPI, parce qu'une analyse de série part toujours de
mesures déjà collectées.

## Parcours pas à pas — une étude de capabilité

### 1. Ouvrir le registre

`/spc/cartes` liste toutes les études réalisées, avec cinq indicateurs en tête : études
réalisées, procédés maîtrisés, plans d'actions en cours, procédés à surveiller, et **études non
maîtrisées sans plan d'actions** — c'est celui-là qu'il faut ramener à zéro. La liste se filtre
par statut (*Maîtrisé*, *À surveiller*, *Non maîtrisé*) et par présence d'un plan d'actions,
se trie par colonne et s'exporte en CSV.

### 2. Créer l'étude

« Créer une étude SPC » ouvre une fiche vide. Vous renseignez l'identification (référence de
la pièce, caractéristique, machine, date), les **limites de tolérance** (USL, LSL), la **taille
d'échantillon (n)**, le **nombre de sous-groupes**, et le **meneur de l'étude** — choisi dans
l'annuaire de votre organisation, jamais tapé en texte libre.

La taille d'échantillon décide de la carte, et l'écran le dit sous le champ :

| Mesures par prélèvement | Carte retenue | Estimation de σ |
|---|---|---|
| 2 à 10 | **X̄ – R** (moyenne / étendue) | σ = R̄ / d2 |
| 11 à 25 | **X̄ – S** (moyenne / écart-type) | σ = s̄ / c4 |
| au-delà de 25 | **aucune** | refus explicite, rien n'est calculé |

Le refus au-delà de 25 est volontaire : c'est la dernière ligne des tables de constantes
publiées, et un prélèvement plus large relève du contrôle à 100 %, pas de l'échantillonnage.
L'écran l'annonce au lieu d'afficher un indice faux.

Le nombre de sous-groupes se choisit dans une liste (un par un jusqu'à vingt, puis par
paliers jusqu'à cent). **Réduire** ce nombre alors que des mesures sont saisies demande
confirmation : on ne jette pas des relevés d'un clic.

### 3. Saisir la grille

Chaque colonne est un sous-groupe, chaque ligne une mesure de l'échantillon. Un bloc copié
depuis un tableur se colle d'un coup. Une cellule vide laisse son sous-groupe **incomplet**, et
seuls les sous-groupes complets entrent dans le calcul.

Deux **jeux d'exemple** sont chargeables si vous avez le droit de saisie : une grille maîtrisée
(poids 500 ± 25 g, n = 5) et un diamètre usiné décentré (Ø 12 ± 0,1 mm, n = 4). Ils remplissent
la fiche sans rien enregistrer, et ne remplacent des mesures déjà saisies qu'après confirmation.

### 4. Lire le résultat

Les formules se lisent **à droite de la grille** : la table des constantes avec la ligne de
votre taille d'échantillon mise en évidence, puis Σ X̄, X̿, R̄ (ou s̄), σ, 3σ, 6σ, Cp, CPU, CPL,
Cpk et les limites des deux cartes. Le bouton « Masquer les formules » replie ce panneau.

- La **carte des moyennes (X̄)** trace UCL, la ligne centrale, LCL, et aussi la USL et la LSL.
- La **carte de dispersion (R ou S)** ne trace pas les tolérances : une tolérance ne s'applique
  pas à une étendue.
- La **courbe de distribution et de tolérance** se trace dès qu'un sous-groupe est complet.
- Deux pastilles annoncent « Procédé maîtrisé » / « Procédé non maîtrisé », neutres tant
  qu'aucun Cpk ne se calcule.

Le verdict du registre dépend du Cpk et des **seuils de votre organisation** : maîtrise à 1,67
et surveillance à 1,33 par défaut, réglables par l'administrateur du client dans *Administration
› Paramètres SPC*. Une étude **recopie les seuils en vigueur à sa création et les garde** : son
verdict ne change pas quand le réglage change plus tard.

### 5. Les règles de Nelson sur votre carte

La carte X̄ de l'étude est **analysée automatiquement** pendant que la grille se remplit ; il n'y
a pas de bouton à trouver. L'analyse part de la ligne centrale et du σ **de la carte** — donc
elle juge la série contre les limites que vous avez sous les yeux.

Ce qui est détecté est posé **sur le graphe** : halo et abrégé (« N2 », « N5 ») sur chaque point
visé, infobulle nommant ses règles, et tracé des frontières ±1σ / ±2σ sans lesquelles un point
marqué « 2 sur 3 au-delà de 2σ » ne serait pas lisible. Sous la carte, une ligne par règle
déclenchée : son titre, sa sévérité, et les **numéros de sous-groupes** concernés.

La carte de dispersion est **exclue** de cette analyse, et l'écran le dit : les règles de série
supposent une distribution symétrique autour de la ligne centrale, ce que celle d'une étendue
n'est pas.

Si le service d'analyse est indisponible, l'écran le dit et propose de relancer — les limites,
Cp, Cpk et le verdict restent exacts. Et aucune règle déclenchée ne change le statut du procédé
d'elle-même : **l'analyse signale, vous décidez**.

### 6. Le plan d'actions

Chaque ligne du registre porte un indicateur « Plan d'actions (O/N)», modifiable directement
dans la liste. Une étude non maîtrisée sans plan d'actions est le défaut que le registre met en
avant.

## Parcours pas à pas — analyser une série (`/spc`)

1. **Coller une série de mesures** (séparées par virgule, espace ou retour à la ligne). La ligne
   centrale et σ sont facultatifs : laissés vides, ils sont estimés à partir de la série
   (carte des valeurs individuelles, σ = MR̄/d₂).
2. Ou bien **partir d'un KPI du catalogue** : choisissez le KPI et la fenêtre (nombre de
   mesures), et cochez au besoin « Ouvrir une CAPA si le procédé est hors-contrôle ». La CAPA
   ouverte est **corrective**, de source `SPC_ALERT`, et un lien direct vers le dossier
   s'affiche.

   > **Lire la carte** ne demande aucun droit particulier : savoir si un procédé est sous
   > contrôle n'est le secret de personne. Lui faire **ouvrir un dossier CAPA**, en revanche,
   > demande `capa.create` — le même droit que partout ailleurs. Sans cela, la carte serait
   > une porte de service vers les dossiers que la [fiche CAPA](capa.md) refuse d'ouvrir.
   >
   > Et le module **DMAIC** doit être actif : l'ouverture automatique sur signal SPC est ce
   > qu'il vend. Lui inactif, la case disparaît, mais **la carte reste lisible** — une
   > décision commerciale ne doit jamais se transformer en perte de conformité.
3. **Lire le verdict** : état du procédé, ligne centrale, limites de contrôle (± 3 σ) avec la
   valeur de σ, puis la carte et la liste des **8 règles de Nelson** violées — point hors
   limites (> 3σ, sévérité haute), points du même côté, tendance, sciage (alternance),
   2 sur 3 au-delà de 2σ, 4 sur 5 au-delà de 1σ, points dans 1σ, points hors 1σ.

## La prévision de dérive

Le panneau **« Prévision de dérive »** répond à la question que la carte ne traite pas : non
pas « le procédé est-il hors contrôle ? », mais **« quand va-t-il en sortir ? »**.

1. Choisissez un **horizon** (en périodes).
2. Cochez au besoin « Ouvrir une CAPA préventive si une sortie est annoncée ».
3. Lancez « Prévoir la dérive ».

Le résultat est un tableau période par période : valeur projetée, intervalle, et **risque de
sortie** en pourcentage ; la première période dont le risque dépasse le seuil est mise en
évidence. La projection est tracée en pointillé *dans* la carte, dans le prolongement de l'axe,
et part du dernier point mesuré — elle emploie les **mêmes limites** que la carte, de sorte que
l'annonce porte toujours sur le trait dessiné.

L'annonce est accompagnée de ce qui la produit : niveau de confiance, tendance par période, R²,
σ résiduel, modèle employé et nombre de mesures utilisées. Une prévision sans ces éléments
serait un oracle.

La CAPA éventuellement ouverte est **préventive** (un écart annoncé n'est pas un écart survenu),
et sa criticité suit la **confiance** de la prévision, pas l'ampleur du risque. Elle n'est jamais
ouverte sans que vous l'ayez demandé.

## Qui peut faire quoi

- **Lire** le registre et les fiches : tout membre de l'organisation.
- **Créer, réviser, supprimer** une étude et changer son plan d'actions : le droit `spc.manage`,
  réglable par l'administrateur du client dans *Administration › Rôles et droits*.
- **Faire ouvrir une CAPA** par la carte de contrôle ou la prévision : le droit `capa.create`.
- Le module **DMAIC** doit être actif : la capabilité relève du pilotage Six Sigma. C'est aussi
  sous ce module que l'écran des rôles range `spc.manage` — il n'existe pas de module « SPC »
  séparé à souscrire.

Création, révision, plan d'actions, suppression et réglage des seuils sont journalisés avec la
taille du sous-groupe, Cp, Cpk, le statut et le plan d'actions — **ni mesure, ni nom de
personne**.

## Bonnes pratiques

- **Vingt à vingt-cinq sous-groupes** est un plancher de confiance, pas un plafond : l'écran le
  rappelle sans l'imposer.
- **Vérifiez d'abord le moyen de mesure** : un Cpk calculé sur un instrument dont la
  répétabilité n'est pas démontrée ne veut rien dire. Voir [Gage R&R](gage-rr.md).
- **Réagissez aux signaux, pas au bruit** : c'est l'intérêt même des règles de Nelson.
- **Reliez à l'action** : un signal récurrent doit alimenter une [CAPA](capa.md) ou un projet
  [DMAIC](dmaic.md). Au-delà des règles déterministes, la
  [détection d'anomalies](anomaly.md) repère des signaux plus diffus sur des données
  multivariées.
- **Ne rejouez pas l'histoire** : les seuils figés sur une étude sont une garantie d'audit.
  Changer le réglage du client vaut pour les études suivantes.
