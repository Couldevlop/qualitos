# Module PDCA — Roue de Deming

[← Retour à l'index](../README.md) · Route : **`/pdca`** · Menu : *Méthodes qualité › PDCA*

## À quoi sert ce module

Le **PDCA** (*Plan-Do-Check-Act*) structure l'amélioration continue en un cycle de 4 étapes :

1. **Plan** — planifier : analyser, fixer un objectif et un indicateur cible.
2. **Do** — faire : mettre en œuvre les actions.
3. **Check** — vérifier : mesurer les résultats par rapport à la cible.
4. **Act** — agir : standardiser ce qui marche, corriger le reste, relancer un cycle si besoin.

Dans QualitOS, un PDCA prend la forme d'un **cycle** que l'on fait **avancer** d'étape en étape,
avec une trace de chaque transition.

## Parcours pas à pas

1. **Ouvrir** le module via `/pdca` : la liste affiche les cycles existants et leur statut.
2. **Créer un cycle** (bouton de création) : titre, description, objectif.
3. **Ajouter des étapes** au cycle pour détailler les actions de chaque phase.
4. **Faire avancer** le cycle (action *advance*) lorsque la phase courante est terminée :
   le cycle passe à l'étape suivante (Plan → Do → Check → Act).
5. **Suivre** l'avancement depuis la fiche du cycle ; chaque transition est historisée.
6. Si un cycle n'a plus lieu d'être, il peut être **annulé**.

## Liens avec les autres modules

- Une cause identifiée dans un [Ishikawa](ishikawa.md) peut donner lieu à un cycle PDCA.
- Une [non-conformité](non-conformites.md) peut s'instancier en cycle PDCA selon sa gravité.
- L'avancement des cycles alimente les indicateurs (`/kpis`).

## Bonnes pratiques

- **Un cycle = un objectif mesurable** : sans indicateur cible, la phase *Check* n'a pas de sens.
- **Ne sautez pas le Check** : vérifiez avant de standardiser (*Act*).
- **Bouclez** : un PDCA bien mené se traduit soit par une standardisation, soit par un nouveau cycle.
