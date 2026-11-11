# Module Coûts de non-qualité — Le modèle PAF

[← Retour à l'index](../README.md) · Route : **`/cout-qualite`** · Menu : *Pilotage › Coûts de non-qualité*

## À quoi sert ce module

La qualité a un coût, et la non-qualité en a un autre. Ce module les met côte à côte selon
le **modèle PAF** (prévention, appréciation, défaillances), en quatre familles :

| Famille | Nature | Exemples livrés avec le produit |
|---|---|---|
| **Prévention** | dépenses volontaires, en amont | formation qualité, planification et système qualité, audits internes, revue de conception / AMDEC |
| **Appréciation** (détection) | dépenses volontaires de vérification | contrôle réception matières, inspections en cours de production, essais et étalonnage, audit produit / process |
| **Défaillances internes** | pertes subies, avant livraison | rebuts, retouches et réparations, re-contrôles après retouche |
| **Défaillances externes** | pertes subies, après livraison | retours produits, et les autres postes de la famille |

L'intérêt n'est pas le total : c'est le **rapport** entre ce qu'on investit (prévention +
appréciation) et ce qu'on perd (défaillances internes + externes). Un euro de prévention qui
évite dix euros de rebut est la seule démonstration qui convainc une direction.

La lecture est ouverte à toute personne connectée. La saisie, la correction, la suppression,
l'ajout d'un libellé et le choix de la devise sont réservés au pilotage qualité (manager
qualité, directeur qualité, administrateur de l'organisation).

## Parcours pas à pas

### Lire un mois

1. **Ouvrir** `/cout-qualite` (menu *Pilotage › Coûts de non-qualité*), mode **Mois**.
2. Lire le **coût total de non-qualité**, puis les deux blocs : *coûts de conformité
   (investissement)* et *coûts de non-conformité (pertes)*, avec la phrase qui en donne le
   rapport.
3. Dérouler chaque famille : ses libellés, le montant de chacun, et le nombre de lignes qui
   le composent. Une ligne de contrôle de pièces affiche son **lot** et son nombre de pièces.

### Saisir une ligne

1. **Ajouter une ligne** (mode Mois).
2. **Libellé de la ligne** : choisi dans la liste, ou tapé pour créer le vôtre. Un libellé
   tapé rejoint votre liste déroulante pour les mois suivants.
3. **Montant** (positif ou nul), dans la devise de l'organisation.
4. **Date d'imputation** — obligatoire. C'est elle qui **range la ligne dans son mois**, et
   l'écran le dit sous le champ.
5. **Responsable** — obligatoire, choisi dans l'annuaire de votre organisation.
6. **Commentaire** — facultatif.
7. Si le libellé est un **contrôle qualité de pièces**, quatre champs supplémentaires
   deviennent obligatoires : référence pièce ou produit, nombre de pièces, lot, date de
   réception ou de fabrication.

Deux rebuts dans le même mois font **deux lignes** sous le même libellé : une ligne est une
imputation, pas un poste mensuel que l'on cumulerait à la main. C'est ce qui permet de
remonter à un lot précis.

### Lire une année

1. Basculer en mode **Année**.
2. Chaque libellé est cumulé sur douze mois, et un histogramme donne la répartition
   mensuelle.
3. **La vue année est en lecture seule** : on n'y saisit rien. Une vue de douze mois ne
   saurait pas quelle date d'imputation proposer, alors que c'est la date qui range la ligne.

### Choisir la devise

La devise est **unique pour l'organisation** et se règle dans l'en-tête de l'écran. Les
montants ne sont jamais convertis : on n'additionne pas des euros et des dollars, et une
devise par ligne produirait des totaux faux.

## Comment lire le résultat

- **Les libellés livrés apparaissent à zéro.** Un poste de prévention sans imputation
  s'affiche à 0 et ouvre la saisie sur son libellé : l'absence de coût de prévention est une
  information, pas une ligne à cacher. Les libellés que vous avez tapés, eux, n'apparaissent
  que s'ils ont servi dans la période.
- **Le rapport conformité / non-conformité n'est pas affiché quand il n'y a aucune perte.**
  Afficher « 0 » dirait le contraire de la réalité.
- **Corriger la date d'imputation déplace la ligne de mois**, et c'est voulu : la date
  comptable fait foi.
- **Changer le libellé d'une ligne pour un libellé qui n'est pas un contrôle de pièces
  effacera ses champs pièces.** Sans cela, une ligne passée de « Rebuts » à « Formation »
  garderait un numéro de lot qui ne voudrait plus rien dire.
- **Il n'y a pas de bouton « réinitialiser la période ».** Dans un système qualité
  auditable, on supprime une ligne nommément, on ne vide pas un mois.

## Bonnes pratiques

- **Imputez au fil de l'eau**, pas en fin d'année : une imputation retrouvée six mois plus
  tard n'a plus ni responsable ni lot.
- **Nommez un responsable réel** sur chaque ligne : c'est lui qui pourra expliquer le
  montant devant la revue de direction.
- **Reliez les pertes à leur cause** : un poste de rebut qui gonfle doit se retrouver dans
  les [non-conformités](non-conformites.md) et dans les [CAPA](capa.md). Un coût sans
  dossier associé ne se réduira pas.
- **N'ajoutez un libellé que s'il manque vraiment.** Deux libellés voisins (« Rebuts » et
  « Rebut atelier ») coupent le cumul annuel en deux lignes qu'on croit différentes — la
  plateforme rattrape le doublon exact, pas la variante.
- **Comparez les familles, pas les totaux** : un total qui baisse parce qu'on a arrêté de
  contrôler n'est pas un progrès.
