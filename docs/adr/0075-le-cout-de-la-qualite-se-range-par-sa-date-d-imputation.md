# ADR 0075 — Le coût de la qualité se range par sa date d'imputation

- **Statut** : Accepté
- **Date** : 2026-09-28
- **Owners** : @Couldevlop
- **Portée** : Module `costofquality` (api-quality-engine), écran `/cout-qualite` (web)
- **S'inscrit dans** : CLAUDE.md §6.2 (COQ = prévention + détection + défaillances
  internes + défaillances externes), §18.2 (tenant et acteur issus du jeton),
  `docs/ECART-nouveaux-modules-qms.md` (troisième des six modules maquettés)

## Contexte

La maquette `docs/quality.png` montre le coût de la qualité selon le modèle PAF :
quatre blocs (prévention, appréciation, anomalies internes, anomalies externes),
un total, et la part des coûts de conformité face aux pertes de non-conformité.
Elle stockait ses chiffres dans le navigateur. Le besoin exprimé ajoute quatre
choses :

1. la même vue **pour chaque mois ou pour l'année** ;
2. un **libellé de ligne choisi dans une liste**, avec la possibilité d'en taper
   un nouveau ;
3. une **fenêtre par ligne** : responsable et date d'imputation obligatoires,
   commentaire facultatif ;
4. pour les lignes de **contrôle qualité de pièces**, en plus : référence de la
   pièce ou du produit, nombre de pièces, lot, date de réception ou de
   fabrication — tous obligatoires.

## Décisions

### 1. Une ligne est une imputation, et sa date la range dans son mois

Une ligne porte un montant, un responsable et une date. Deux rebuts dans le
même mois font deux lignes sous le même libellé. L'autre modèle, un poste
mensuel qui cumule des saisies, aurait demandé une fenêtre qui liste d'autres
fenêtres, et aurait brouillé la traçabilité par lot.

Il n'y a **pas de champ « période »** : la date d'imputation suffit. Deux dates
pourraient se contredire, et c'est la date comptable qui fait foi. Corriger la
date déplace donc la ligne dans un autre mois, et c'est voulu.

### 2. Le catalogue livré n'appartient à personne ; un libellé tapé appartient au client

`coq_labels` porte les 16 libellés de la maquette **sans tenant**, sous un
`code` stable que l'écran traduit dans les six langues, avec des identifiants
fixes posés par la V134. Un libellé tapé en texte libre appartient au tenant,
n'a pas de code, et rejoint sa liste déroulante pour les mois suivants. La
contrainte `ck_coq_labels_code_iff_builtin` impose l'un OU l'autre.

Un libellé tapé sous un nom déjà présent dans la famille (livré ou saisi, sans
tenir compte de la casse ni des espaces) **rend l'existant** : deux « Rebuts »
couperaient le cumul annuel en deux lignes qu'on croirait différentes. L'index
unique partiel sur `lower(name)` sert de filet en cas de saisies simultanées.

### 3. « Contrôle de pièces » est une propriété du libellé, pas de la ligne

`part_control` vit sur le libellé. Sept libellés livrés l'ont, là où une pièce
ou un lot existe réellement : réception matières, inspections en cours de
production, audit produit/process, rebuts, retouches, re-contrôles, retours
produits. Pour un libellé tapé, une case de la fenêtre le déclare. La ligne en
recopie la valeur à l'écriture, avec sa famille, pour que les totaux et le
`CHECK` se calculent sans jointure.

La règle vit à trois endroits, et c'est volontaire :

- dans le **domaine** (`CoqEntry`), qui rend un 422 portant le champ fautif ;
- dans la **base** (`ck_coq_entries_part_fields`), pour toute écriture qui ne
  passerait pas par le domaine ;
- dans le **formulaire**, pour le confort de saisie.

Sur une ligne qui n'est pas un contrôle de pièces, les champs pièces sont
**effacés**. Sans cela, une ligne passée de « Rebuts » à « Formation »
garderait un numéro de lot qui ne voudrait plus rien dire.

### 4. La vue année est en lecture seule

L'année cumule chaque libellé sur douze mois et ajoute un histogramme mensuel.
On n'y saisit rien : une vue de douze mois ne saurait pas quelle date proposer,
alors que c'est la date qui range la ligne.

### 5. Les libellés livrés apparaissent à zéro

Comme sur la maquette, un libellé livré sans imputation s'affiche à 0 et ouvre
la saisie sur son libellé. L'absence de coût de prévention est une
information, pas une ligne à cacher. Un libellé tapé n'apparaît que s'il a
servi dans la période : sinon la liste ne ferait que s'allonger.

### 6. Une devise par client, sans conversion

`coq_settings` garde une devise ISO 4217 par tenant, EUR par défaut. Les
montants ne sont jamais convertis. Une devise par ligne aurait produit des
totaux faux : on n'additionne pas des euros et des dollars.

### 7. Lire est ouvert, saisir ne l'est pas

Tout utilisateur authentifié lit. La saisie, la correction, la suppression,
l'ajout d'un libellé et le choix de la devise sont réservés à `QUALITY_MANAGER`,
`DIRECTOR_QUALITY`, `ADMIN_TENANT` et `SUPER_ADMIN`, la même liste que
l'arbitrage des idées. Chaque écriture de ligne est inscrite au journal d'audit
chaîné (`coq.entry.recorded|revised|deleted`), sans donnée personnelle : ni le
nom du responsable, ni le commentaire.

Les écritures sont transactionnelles **au contrôleur** : la ligne et sa trace
d'audit sont validées ensemble, sans que la couche application ait à connaître
Spring.

Le ratio conformité / non-conformité vaut `null` quand il n'y a aucune perte.
Afficher 0 dirait le contraire de la réalité.

## Conséquences

- Pas encore d'entrée au catalogue des modules activables : l'entrée de menu
  n'a pas d'attribut `module`, comme la boîte à idées. L'ajouter demande un code
  de module côté api-core.
- Ce montant n'alimente pas encore le catalogue KPI (§6.6). Un KPI « COQ en %
  du CA » demandera le chiffre d'affaires, qui n'existe nulle part dans la
  plateforme aujourd'hui.
- La maquette propose aussi un bouton « Réinitialiser les données ». Il n'est pas
  repris : dans un QMS auditable, on supprime une ligne, on ne vide pas une
  période.
