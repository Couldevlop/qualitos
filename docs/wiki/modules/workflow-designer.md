# Module Designer de workflow — Cartographier un processus en BPMN 2.0

[← Retour à l'index](../README.md) · Route : **`/workflow-designer`** ·
Menu : *Méthodes qualité › Designer de workflow*

## À quoi sert ce module

Le designer sert à **dessiner et publier vos processus qualité** au format **BPMN 2.0**, sans
écrire de code : un éditeur graphique avec palette et glisser-déposer, une **version** qui
s'incrémente à chaque enregistrement, et un cycle **brouillon → publié → archivé**.

C'est l'outil de la **cartographie des processus** que réclament les normes de système de
management (ISO 9001 §4.4, et la cartographie attendue à l'étape 6 d'une feuille de route de
certification) : un processus dessiné, nommé, versionné, et qu'on peut montrer.

Lisez d'abord la section [« Ce que le module ne fait pas »](#ce-que-le-module-ne-fait-pas) :
elle évite un contresens coûteux.

## Parcours pas à pas

### 1. La liste

`/workflow-designer` liste vos définitions : nom, description, **version**, statut. Elle se
filtre par statut. Chaque ligne porte ses deux actions de cycle de vie — **Publier**,
**Archiver**.

### 2. Créer et dessiner

1. **Nouveau workflow**.
2. L'éditeur s'ouvre sur un diagramme **neuf, réduit à un événement de début** — vous partez
   d'une page blanche utilisable, pas d'un modèle imposé.
3. Donner un **nom** (obligatoire) et une description.
4. Composer le diagramme à la souris : tâches, passerelles, événements, flux de séquence.
5. **Enregistrer**.

L'éditeur graphique est chargé **à la demande**, au moment où vous ouvrez un diagramme : c'est
une bibliothèque lourde, et l'écran reste léger pour qui ne s'en sert pas. Le premier
affichage du canevas peut donc prendre un instant, et l'écran le dit.

### 3. Faire vivre la définition

| État | Ce qui est permis |
| --- | --- |
| **Brouillon** | tout : renommer, redessiner, enregistrer autant de fois que nécessaire |
| **Publié** | lecture seule — le diagramme est **figé** |
| **Archivé** | lecture seule, définitif |

Deux conséquences à connaître avant de publier :

- **Un diagramme publié ne se modifie plus.** C'est ce qui donne du sens à la publication :
  une cartographie qu'on retouche après l'avoir montrée ne prouve rien. L'éditeur passe en
  lecture seule et le dit.
- **Publier ne se défait pas.** D'un brouillon ou d'un publié, on ne va plus que vers
  l'archivage.

### 4. La version

La version s'incrémente **à chaque enregistrement** d'un brouillon, pas seulement à la
publication. Elle vous dit combien de fois la définition a été reprise, et elle est affichée
dans la liste comme dans l'éditeur.

## Comment lire ce qui est stocké

Ce que la plateforme conserve, c'est le **XML BPMN 2.0** du diagramme, par organisation, avec
son nom, sa description, son statut, sa version, qui l'a créé et qui l'a modifié en dernier.
La taille d'un diagramme est bornée à **512 ko de XML** — largement au-delà de ce qu'un
processus lisible occupe ; la limite est là pour qu'un diagramme pathologique ne pénalise
personne.

Le format étant du BPMN 2.0 standard, le XML est lisible par n'importe quel outil BPMN.

## Ce que le module ne fait pas

Il faut le dire clairement, parce que le mot « workflow » invite à le croire :

- **Le diagramme n'est pas exécuté.** Rien dans la plateforme n'interprète ce BPMN pour faire
  avancer un dossier, affecter une tâche ou envoyer une notification. C'est un **outil de
  cartographie et de documentation**, pas un moteur d'orchestration.
- **Il ne règle aucun circuit de validation.** Les circuits qui, eux, gouvernent réellement des
  objets — l'approbation d'un document de la GED, la validation d'une demande de modification,
  le parcours d'une [CAPA](capa.md) — se paramètrent dans *Administration*, par type d'objet, et
  ne lisent pas les diagrammes dessinés ici.
- **Il ne génère pas de formulaire.** Dessiner une tâche ne crée pas l'écran qui la réalise.

Autrement dit : le designer documente le processus tel que vous voulez qu'il soit tenu ; ce qui
le tient, ce sont les modules métier et leurs paramètres.

## Bonnes pratiques

- **Un diagramme, un processus.** Un diagramme qui couvre trois processus ne sera lu par
  personne, et son archivage emportera les trois.
- **Restez en brouillon tant que le processus bouge.** La publication est le geste par lequel
  vous figez une référence ; ce n'est pas une sauvegarde.
- **Nommez la définition comme la norme la nomme.** « Maîtrise des informations documentées »
  se retrouve plus vite que « Procédure docs v3 » quand l'auditeur demande la cartographie.
- **Reliez la cartographie aux exigences** via le [Standards Hub](standards-hub.md), qui porte
  le rattachement des processus aux clauses d'une norme.
- **Archivez au lieu de supprimer.** Un processus remplacé reste la trace de ce qui se faisait
  l'an dernier — exactement ce qu'un audit de surveillance vient regarder.
