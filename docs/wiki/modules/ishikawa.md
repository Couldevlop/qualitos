# Module Ishikawa — Diagramme de causes

[← Retour à l'index](../README.md) · Route : **`/ishikawa`** · Menu : *Méthodes qualité › Ishikawa*

## À quoi sert ce module

Le **diagramme d'Ishikawa** (dit « en arête de poisson » ou « 5M / 6M ») classe les causes
possibles d'un problème par catégories — les fameux « M » : **Main-d'œuvre, Méthode, Matière,
Matériel, Milieu, Mesure**. Il aide une équipe à explorer toutes les pistes avant de conclure
trop vite.

QualitOS y ajoute une **suggestion de causes par IA** : à partir du problème décrit, le module
propose des causes probables par branche, que vous validez ou écartez.

## Parcours pas à pas

1. **Ouvrir** `/ishikawa` : la liste des diagrammes existants.
2. **Créer un diagramme** : décrire le **problème** (l'effet à analyser).
3. **Ajouter des causes** par catégorie (branche).
4. **Demander des suggestions IA** (action *suggérer des causes*) : le module propose des causes
   probables que vous pouvez retenir et compléter.
5. **Affiner** : conserver, modifier ou supprimer les causes jusqu'à un diagramme complet.

> Les suggestions IA sont une aide à la réflexion : **vous restez décideur** sur les causes retenues.

## Lire l'arête de poisson

La fiche affiche le diagramme sous sa **forme conventionnelle** : la tête porte le problème,
l'épine y mène, et chaque branche oblique est une catégorie qui l'attaque. C'est cette figure
qui montre ce qu'une liste ne montre pas — que toutes les familles de causes convergent vers
un même effet.

Le dessin **illustre** ; les **cartes de branche placées en dessous restent la référence** :
- elles portent les libellés **entiers** (le dessin peut en abréger un trop long) ;
- elles déplient la **hiérarchie complète** des sous-causes, que le dessin résume par un
  compteur (« +3 ») afin de ne pas devenir illisible ;
- c'est **là que l'on agit** : ajouter, renommer, supprimer une cause.

Le nombre de branches suit le mode retenu (6M, 7M ou 8M) et le tracé s'y adapte seul. En
arabe, la figure se lit de droite à gauche : la tête passe à gauche.

## Liens avec les autres modules

- Une cause retenue peut être traitée par un cycle [PDCA](pdca.md) ou un projet [DMAIC](dmaic.md).
- L'analyse de cause-racine d'une [CAPA](capa.md) peut s'appuyer sur un Ishikawa.

## Bonnes pratiques

- **Un problème clair** : formulez l'effet de façon précise et factuelle.
- **Creusez chaque branche** : ne vous arrêtez pas à la première cause évidente.
- **Distinguez cause et symptôme** : visez la cause-racine, pas l'effet visible.
