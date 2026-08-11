# Module 5 Pourquoi — Recherche de cause-racine

[← Retour à l'index](../README.md) · Route : **`/five-whys`** · Menu : *Qualité opérationnelle › 5 Pourquoi*

## À quoi sert ce module

La méthode des **5 Pourquoi** descend d'un problème constaté jusqu'à sa **cause-racine**, en
demandant « pourquoi ? » à chaque réponse obtenue. Cinq est un ordre de grandeur, pas une
règle : on s'arrête quand on tient une cause sur laquelle on peut réellement agir.

Elle complète l'[Ishikawa](ishikawa.md), qui **ouvre** l'éventail des causes possibles, là où
les 5 Pourquoi **creusent** une piste jusqu'au fond.

## Parcours pas à pas

1. **Ouvrir** `/five-whys` : la liste des analyses en cours et conclues.
2. **Créer une analyse** : énoncer le **problème** de façon factuelle (ce qu'on a constaté,
   pas ce qu'on en suppose).
3. **Répondre au premier pourquoi**, puis demander « pourquoi ? » à cette réponse, et ainsi
   de suite.
4. **Conclure la cause-racine** lorsque la descente atteint une cause actionnable.
5. **Traiter** : la cause-racine peut ouvrir une [CAPA](capa.md) ou alimenter un
   [Ishikawa](ishikawa.md).

## Lire la cascade

L'analyse s'affiche sous forme de **cascade en escalier** : chaque encart descend vers le
suivant, et la teinte glisse du rouge (le symptôme constaté) vers le vert (la cause-racine).
Cette forme dit ce qu'une liste à puces ne dit pas — qu'il s'agit d'une **descente**, où
chaque niveau explique celui du dessus. Quand la cause-racine est conclue, elle ferme le
dessin.

Le dessin **illustre** ; la **liste placée en dessous reste la référence** : elle porte le
texte **entier** de chaque pourquoi (le dessin peut abréger un énoncé trop long) et c'est
**là que l'on saisit et modifie**. Une analyse de trois pourquoi comme de sept se dessine
sans réglage.

## Liens avec les autres modules

- Une non-conformité ou une [CAPA](capa.md) peut lancer une analyse 5 Pourquoi.
- La cause-racine conclue peut être reprise comme cause dans un [Ishikawa](ishikawa.md), ou
  traitée par un cycle [PDCA](pdca.md).

## Bonnes pratiques

- **Restez factuel** : une réponse qui commence par « il aurait fallu » est une solution,
  pas une cause.
- **Une seule chaîne à la fois** : si deux causes cohabitent, ouvrez deux analyses — ou
  passez à l'[Ishikawa](ishikawa.md), fait pour l'éventail.
- **Visez l'actionnable** : « manque de rigueur » n'est pas une cause-racine, c'est un
  jugement. Descendez jusqu'à ce qui se corrige.
- **Ne forcez pas jusqu'à cinq** : trois pourquoi qui touchent le fond valent mieux que
  cinq qui tournent en rond.
