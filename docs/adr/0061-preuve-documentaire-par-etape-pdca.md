# ADR 0061 — La preuve documentaire attachée à l'étape PDCA, pas au cycle

- **Statut** : Accepté
- **Date** : 2026-08-27
- **Owners** : @Couldevlop
- **Portée** : module PDCA uniquement (`/pdca/:id`), pièce jointe simple

## Contexte

Le tableau des étapes d'un cycle PDCA affichait la phase, le libellé, le statut,
l'échéance et la date de mise à jour. Rien d'autre. Une étape marquée `DONE` ne
prouvait donc rien : elle **affirmait**.

La demande est venue du terrain, et sa formulation contient déjà la réponse :
« l'évidence de la mise en place d'une action, c'est toujours un document
signé ». Autrement dit, ce que l'auditeur réclame en face d'une ligne cochée,
c'est une pièce — relevé, procédure approuvée, constat photographique — pas une
case.

Le module CAPA porte déjà exactement ce mécanisme sur ses actions (ADR 0052).
La question n'était donc pas *comment* mais *à quel niveau rattacher la pièce*,
et *que reprendre du mécanisme existant*.

## Décisions

### 1. La preuve se rattache à l'ÉTAPE, jamais au cycle

Les preuves CAPA connaissent deux niveaux : le **dossier** — le niveau que
désigne la norme — et l'**action**. Le PDCA n'en a qu'un.

La raison n'est pas la simplicité, c'est la sémantique : dans un cycle PDCA,
l'unité qui se prouve est l'étape. Un cycle ne se démontre pas en bloc ; il se
démontre par la somme de ses étapes, chacune avec sa pièce. Ouvrir un niveau
« cycle » aurait produit un fourre-tout où l'on verse ce qu'on ne sait pas
rattacher — et un dossier d'audit se juge à ce qu'on peut rattacher.

`step_id` est donc `NOT NULL`, là où `capa_evidences.action_id` est nullable.

### 2. Une pièce par étape, garantie en base et pas seulement au service

La cellule « Preuve » du tableau montre **un** document. Deux pièces rendraient
la cellule indécidable : il faudrait en cacher une, ou faire déborder la colonne.

Le service compte avant d'écrire, mais un comptage applicatif ne tient pas
devant deux dépôts concurrents : les deux liraient zéro et les deux passeraient.
D'où la contrainte `UNIQUE (step_id)` en base — c'est elle qui décide, le
comptage ne sert qu'à rendre le refus lisible.

Remplacer se fait donc en deux gestes — retirer, puis reverser — et **les deux
se consignent**. Un remplacement silencieux ferait disparaître une pièce d'un
dossier d'audit sans laisser de trace.

### 3. Ni signature ML-DSA ni ancrage blockchain à ce stade

Tranché avec le demandeur. La valeur immédiate est de **pouvoir produire le
document** ; l'horodatage cryptographique de ce document est un chantier
distinct, qui suppose de décider quoi signer (la pièce ? la ligne ? le cycle
entier ?) et à quel moment de la vie du cycle.

Ce qui est déjà tenu, en revanche : chaque dépôt et chaque retrait est inscrit
au journal chaîné du tenant (§11.5), avec l'auteur, l'étape visée et le nom du
document — jamais la clé d'objet, qui donnerait un chemin de stockage dans un
journal qui se relit et s'exporte.

### 4. Le mécanisme de stockage est repris à l'identique, pas généralisé

Mêmes bornes que les preuves d'action CAPA : 10 Mo par pièce, liste blanche de
sept types MIME, vérification de la signature binaire contre le type déclaré,
extension dérivée du type validé et **jamais** du nom fourni par le client, nom
d'origine conservé mais neutralisé, clé d'objet construite uniquement à partir
d'identifiants tenus par la plateforme. Le plafond cumulé (25 Mo) se compte au
**cycle**, qui est l'unité qu'un tenant crée et multiplie.

Le code est **dupliqué** dans le paquet `pdca` plutôt que factorisé dans un
service partagé. C'est délibéré, et c'est le choix qui demande le plus de
justification :

- Les deux modules ont des **verrous d'état différents** (un dossier CAPA clos
  ou rejeté ; un cycle PDCA terminé ou annulé), des **niveaux de rattachement
  différents** (nullable ici, obligatoire là) et des **plafonds comptés sur des
  unités différentes**. Un service commun aurait porté ces trois divergences en
  paramètres, et un paramètre de bornes est exactement ce qu'on finit par
  passer de travers.
- Ce qui mérite d'être partagé l'**est déjà** : le port `ObjectStorage`, le
  balayage des orphelins et son contrat `StoredObjectOwner`, le journal d'audit.
  La duplication porte sur la politique, pas sur l'infrastructure.

Si un troisième module réclame le même mécanisme, la factorisation redeviendra
la bonne réponse — avec trois exemples sous les yeux plutôt que deux.

### 5. Le module revendique ses binaires face au balayeur d'orphelins

`PdcaStepEvidenceObjectOwner` implémente `StoredObjectOwner` (ADR 0056). Sans
lui, le balayage prendrait toute preuve d'étape pour un orphelin passé le délai
de grâce et l'effacerait : le cycle pointerait vers des documents disparus, et
rien ne dirait pourquoi.

### 6. La colonne se place après l'échéance, pas en fin de ligne

« Pour quand » puis « et voici que c'est fait ». Rejeter la pièce en fin de
ligne l'aurait éloignée de la date qu'elle justifie — et la lecture d'un tableau
se fait de gauche à droite, une paire à la fois.

## Conséquences

- Migration `V119__create_pdca_step_evidences.sql` : table de métadonnées
  seulement, cascade depuis le cycle **et** depuis l'étape (une étape supprimée
  hors phase active doit emporter sa pièce, sinon la ligne survivrait en
  désignant une étape qui n'existe plus).
- Trois routes sous `/api/v1/pdca/cycles/{id}` : `GET /step-evidences` (toutes
  les pièces du cycle en un appel — une requête par ligne ferait autant d'allers
  et retours que d'étapes pour remplir une seule colonne), `POST
  /steps/{stepId}/evidences`, `DELETE /steps/{stepId}/evidences/{evidenceId}`.
- Le retrait tombe sous la règle `DELETE /api/v1/**` du socle : manager qualité
  ou plus. Un opérateur verse une pièce, il ne la retire pas — retirer une preuve
  d'un dossier d'audit n'est pas un geste de saisie.
- Quand le stockage objet est coupé, les trois routes répondent 503 et la colonne
  annonce son état plutôt que d'afficher un bouton inerte.

## Alternatives écartées

- **Un champ texte « lien vers la preuve »** : sans dépôt, la pièce vit ailleurs
  et l'audit ne la retrouve pas. Un lien mort dans un dossier de certification
  vaut une absence de preuve.
- **Réutiliser `capa_evidences` avec une colonne `pdca_step_id`** : deux clés
  étrangères mutuellement exclusives sur la même table, et une contrainte
  d'unicité qui devrait porter tantôt sur l'une tantôt sur l'autre. Le coût
  d'une table de plus est très inférieur.
- **Plusieurs pièces par étape** : voir décision 2. La liste appartient au
  niveau dossier, que le PDCA n'a pas.
