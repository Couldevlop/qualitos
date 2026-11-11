# Assistant IA — Question en langage naturel, exploration et storyboard

[← Retour à l'index](../README.md) · Routes : **`/nlq`**, **`/nlq-explore`**, **`/storyboard`** ·
Menu : *Analyses IA*

## À quoi servent ces trois écrans

Trois façons de faire parler vos données qualité, de la plus directe à la plus narrative :

| Écran | Route | Ce qu'il rend |
|---|---|---|
| **Assistant IA** | `/nlq` | Une réponse rédigée, les lignes de données, et la requête qui les a produites |
| **Exploration NLQ** | `/nlq-explore` | La même chose avec un **graphique** généré, pour explorer |
| **Storyboard IA** | `/storyboard` | Un **récit** autour d'indicateurs que vous fournissez, pour une revue de direction |

Les deux premiers interrogent vos données ; le troisième ne les interroge pas — il commente des
chiffres que vous saisissez.

## Assistant IA (`/nlq`)

### Parcours pas à pas

1. **Ouvrir** `/nlq`.
2. **Poser la question en français courant** dans le champ « Votre question ». Des exemples sont
   proposés sous le champ : cliquez-en un pour le reprendre.
3. **Interroger**. L'écran annonce ce qu'il fait : « Traduction en requête sécurisée et
   exécution… ».

### Comment lire le résultat

- La **Réponse** est une phrase rédigée, accompagnée d'un **indice de confiance** en pourcentage.
  Une confiance basse n'invalide pas la réponse, elle vous dit de vérifier.
- Les **Résultats** affichent les lignes réellement lues, avec leur nombre. Les valeurs
  numériques sont doublées d'une barre pour se comparer d'un coup d'œil. S'il n'y a rien à
  montrer, l'écran le dit (« Aucune donnée ne correspond à cette question ») au lieu d'inventer.
- Une rangée de **pastilles** donne la traçabilité : la confirmation que le **filtre de votre
  organisation** a été appliqué, et la liste des **tables interrogées**.
- Le bouton de bascule affiche la **requête SQL générée**, telle qu'elle a été exécutée. C'est
  l'explicabilité du résultat : vous pouvez la relire, la contester, la faire vérifier.

### Les garde-fous

- **Lecture seule.** L'assistant ne sait que lire. Aucune question ne peut créer, modifier ou
  supprimer une donnée, et la requête générée est contrôlée avant exécution : un seul ordre, un
  `SELECT`, des tables prises dans une liste autorisée.
- **Votre organisation uniquement.** Le filtre de cloisonnement est ajouté automatiquement et
  l'écran le montre. Une question ne peut pas atteindre les données d'un autre client.
- **Vos données personnelles sont protégées** : la question et la réponse passent par un filtre
  qui masque les informations personnelles avant tout appel au modèle.
- **Les tentatives de détournement de l'assistant sont filtrées** avant qu'il ne soit sollicité.

## Exploration NLQ (`/nlq-explore`)

Même principe, orienté exploration : « Posez une question sur vos données qualité — elle est
traduite en requête sûre et un graphique est généré. »

1. **Poser la question**, ou partir d'un exemple.
2. Lire les trois panneaux : **Graphique**, **Résultats** (le tableau), et **Explicabilité**.
3. Le panneau **Explicabilité** affiche, sur demande, la requête générée. Le graphique n'est
   jamais présenté sans que la requête qui l'alimente puisse être lue.

Utilisez cet écran quand vous cherchez une tendance ou une répartition ; l'assistant `/nlq`
quand vous cherchez un chiffre.

## Storyboard IA (`/storyboard`)

Cet écran rédige le **commentaire** d'une série d'indicateurs, pour une revue de direction ou un
comité qualité. Il ne va chercher aucune donnée : **il ne commente que les chiffres que vous lui
donnez.**

### Parcours pas à pas

1. Renseigner la **période** (« Mai 2026 », « T1 2026 ») et, si utile, le **contexte**
   (« Site de Lyon, atelier mécanique »).
2. Ajouter les **indicateurs** un par un : libellé, valeur, tendance. Le bouton « Exemple »
   remplit la fiche pour voir le fonctionnement.
3. **Générer le récit**.

### Comment lire le résultat

- Le **Récit** est un texte prêt à relire et à amender. Il n'est pas un verdict.
- Le fournisseur de modèle qui l'a produit est indiqué (« Généré par »).
- Un bloc **« Chiffres absents des données source, à vérifier »** liste les nombres qui
  apparaissent dans le texte sans figurer dans les indicateurs fournis. Ce bloc est le point le
  plus important de l'écran : s'il n'est pas vide, le récit avance un chiffre que vous ne lui
  avez pas donné, et il faut le corriger avant diffusion.

## Le fournisseur de modèle est celui de votre organisation

Chaque client choisit son fournisseur d'IA, et ce choix est **imposé** à tous ces écrans. Il n'y
a aucun repli silencieux vers un autre fournisseur : si la politique du client n'est pas
lisible, l'appel échoue et le dit, plutôt que de dégrader la souveraineté de vos données sans
vous en informer. Le réglage se fait dans *Administration › Fournisseur d'IA*
(voir le [guide de l'administration du client](../roles/administration-du-client.md)).

Le volume d'appels est contingenté par organisation : si le quota est atteint, l'écran l'annonce
et vous pouvez relancer plus tard. Le reste de l'application n'est pas affecté.

## Bonnes pratiques

- **Posez une question à la fois.** « Combien de NC en mars sur le site A ? » donne un meilleur
  résultat que trois questions enchaînées.
- **Relisez la requête générée** avant de reprendre un chiffre dans un document opposable. Elle
  est affichée pour cela.
- **Ne faites pas du récit une preuve** : c'est un commentaire. Les preuves sont les
  enregistrements des modules, et les [rapports signés](rapport-8d.md) qu'ils produisent.
- **Vérifiez le bloc des chiffres non vérifiés** du storyboard, systématiquement.
- **L'IA propose, vous décidez** : aucune action n'est prise par ces écrans.
