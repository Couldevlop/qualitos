# ADR 0135 — Une CAPA porte une action, et une seule

- **Statut** : accepté
- **Date** : 2026-11-22
- **Portée** : modèle d'une CAPA et de son action, tableaux d'actions des
  disciplines D3, D5 et D7 du traitement 8D, registre des CAPA
- **Rétablit** : l'**ADR 0088 §1** (« une CAPA porte une action »)
- **Révise** : l'**ADR 0115** (« une CAPA porte plusieurs actions, montrées en
  tableau »), qui avait révisé l'ADR 0088 §1 sur ce seul point

## Contexte

L'ADR 0115 a fait du dossier CAPA un conteneur : un dossier, plusieurs actions,
d'où le tableau des actions sur la fiche du dossier. L'usage en production a
démenti ce modèle, et le démenti est venu d'un symptôme qu'on ne pouvait pas
corriger sans trancher.

Les trois disciplines d'actions du traitement 8D — endiguer (D3), corriger la
cause (D5), prévenir la récidive (D7) — rendaient **un tableau par dossier**,
puisqu'un dossier portait un tableau. Écrire une action depuis la section CAPA
ouvre un dossier ; cette action apparaissait donc dans un **second tableau**,
sous le premier, au lieu de rejoindre la ligne de ses pareilles. Vu de
l'utilisateur : « elle crée un autre tableau comme si elle refusait de se mettre
dans le tableau existant ».

Le réflexe aurait été de rassembler les tableaux et de s'arrêter là. Il laissait
l'autre sens cassé : une action **ajoutée depuis une discipline** entrait comme
action supplémentaire du dossier voisin, et le registre des CAPA ne la montrait
pas — il montrait son dossier, dont le titre était celui d'une autre action. On
ne pouvait donc pas faire l'aller-retour entre le registre et le 8D, alors que
c'est précisément ce que la méthode demande.

Deux lectures expliquaient le symptôme, et elles s'excluent :

1. **Le dossier est le porteur.** Il faut alors un dossier principal de l'écart,
   et les actions s'y rangent. C'est l'ADR 0115 poussé jusqu'au bout.
2. **L'action est le dossier.** Un dossier CAPA *est* une action : son intitulé,
   son pilote, son échéance, son critère d'efficacité et son verdict décrivent
   un seul geste.

## Décision

**Une CAPA porte une action, et une seule. Chaque action est une CAPA de plein
droit ; il n'existe pas de CAPA principale qui porterait des actions.**

Conséquences tenues :

### 1. Les disciplines rassemblent, elles ne répètent plus

Un tableau par discipline, et un seul, quel que soit le nombre de dossiers de
l'écart. Il rassemble les actions **de sa nature** portées par **tous** les
dossiers de l'écart : une ligne, une CAPA. Le verrou de clôture s'applique donc
**ligne par ligne** — les actions d'un dossier réglé ne bougent plus, celles de
son voisin ouvert continuent — là où un verrou de tableau les aurait liées.

### 2. Ajouter dans une discipline ouvre une CAPA

Le bouton d'ajout d'une discipline n'ajoute plus une action au dossier voisin :
il **ouvre une CAPA de plus** sur l'écart, par la fenêtre de création des CAPA —
la même interface, et non une jumelle. La **nature est imposée** par la
discipline d'où l'on part : endiguement au D3, corrective au D5, préventive au
D7, les deux autres cases verrouillées (ADR 0130). La **criticité** est héritée
du dossier déjà posé : c'est celle de l'écart, et la redécider dossier par
dossier ferait diverger deux chiffres sur un seul fait.

La CAPA ainsi ouverte **cite l'écart en source**, par sa référence. C'est ce lien
que la fiche de l'écart et son rapport 8D suivent déjà pour rassembler tous les
dossiers (ADR 0121), et c'est lui qui fait que l'aller-retour marche désormais
dans les deux sens.

### 3. Le premier dossier garde son chemin

Ouvrir la **première** CAPA d'un écart reste une escalade : elle pose le lien que
l'écart porte et fait franchir l'étape « action définie ». Les suivantes citent
la référence. Les deux chemins aboutissent au même ensemble, celui dont dépend la
clôture.

## Ce que cela ne décide pas

Le serveur **accepte encore** plusieurs actions par dossier : la règle est tenue
par les écrans, pas par le domaine. Les dossiers créés sous l'ADR 0115 qui
portent plusieurs actions restent lisibles et modifiables — les refuser aurait
rendu de l'historique inaccessible. Interdire l'ajout côté domaine, et reprendre
les dossiers existants, est une dette consignée ici.

## Conséquences

- Le registre des CAPA montre chaque action, puisque chaque action est un
  dossier. C'est l'autre sens de l'aller-retour, et il était impossible avant.
- Un écart porte autant de dossiers que d'actions décidées. Le décompte des
  motifs de blocage portant sur les actions (et non sur les dossiers), le compte
  annoncé et le nombre de lignes concordent.
- La fiche d'un dossier n'a plus de liste d'actions à montrer : elle montre
  l'avancement de la sienne.

## Dette

- Le domaine n'interdit pas encore l'action surnuméraire ; les écrans seuls
  l'empêchent.
- Les dossiers hérités dont le titre est celui de leur action gardent ce titre.
