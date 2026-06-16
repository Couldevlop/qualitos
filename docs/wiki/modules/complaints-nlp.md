# Module Analyse NLP des réclamations

[← Retour à l'index](../README.md) · Route : **`/complaints-nlp`** · Menu : *Méthodes qualité › Réclamations IA*

## À quoi sert ce module

Ce module analyse le **texte d'une réclamation client** pour en extraire automatiquement :

- un **sentiment** (tonalité positive / négative) ;
- une **classification** (catégorie de la réclamation).

L'analyse s'appuie sur une approche NLP **lexicale** (sentiment + classification). Elle aide à
**prioriser** rapidement les réclamations : repérer celles à forte charge négative et les router
vers le bon traitement.

> Ce module d'**analyse IA** est distinct de la gestion CRUD des réclamations : il fournit la
> lecture automatique du texte, pas le suivi du dossier.

## Parcours pas à pas

1. **Ouvrir** `/complaints-nlp`.
2. **Soumettre le texte** d'une réclamation.
3. **Lire le résultat** : sentiment et catégorie proposés.
4. **Prioriser** : traitez en premier les réclamations au sentiment le plus négatif ou les
   catégories critiques.

## Bonnes pratiques

- **Aide à la priorisation, pas jugement final** : un humain confirme la catégorie sensible.
- **Reliez à l'action** : une réclamation critique peut ouvrir une [NC](non-conformites.md) ou une
  [CAPA](capa.md).
- **Surveillez les tendances** : une montée des sentiments négatifs sur une catégorie est un signal
  fort.
