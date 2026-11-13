# ADR 0119 — La fiche CAPA montre son avancement, et non une liste d'actions

- **Statut** : Accepté
- **Date** : 2026-11-13
- **Owners** : @Couldevlop
- **Portée** : `apps/web` (`features/capa` : `CapaDetailComponent`,
  `CapaModule`). Aucun changement de serveur, aucun changement de schéma : les
  actions restent plusieurs, l'API qui les lit et les écrit ne bouge pas.
- **Révise** : **ADR 0115** (un dossier porte plusieurs actions, présentées en
  tableau) — pour la **fiche du dossier** uniquement. La pluralité des actions
  reste vraie en base et à l'API, et le tableau reste là où les actions se
  décident : sur la fiche de l'écart.
- **Prolonge** : **ADR 0117** (le statut du dossier se déduit de ses actions).
  C'est cette déduction que la carte d'avancement rend lisible.
- **S'inscrit dans** : ADR 0088 (une CAPA naît avec son action),
  CLAUDE.md §4.2 (CAPA), §15.1 (sobriété)

## Contexte

Une CAPA **est** une action. Elle naît avec la sienne, posée au moment où le
dossier s'ouvre (ADR 0088) : pilote, échéance, critère d'efficacité. Dans
l'écrasante majorité des dossiers, cette action est la seule — l'ADR 0115 a
ouvert la porte à plusieurs, mais c'est depuis la **fiche de l'écart** qu'on
les ajoute, parce que c'est là qu'on décide quoi faire.

La fiche du dossier affichait pourtant un tableau d'actions, avec son bouton
« Ajouter », ses lignes, ses colonnes et ses suppressions. Dans le cas
courant, ce tableau avait une seule ligne — qui répétait, dans un cadre à
part, le pilote et l'échéance déjà écrits en tête de fiche.

Trois conséquences observées :

1. **Une liste à l'intérieur de son propre élément.** Un lecteur qui voit un
   tableau d'actions dans une action en conclut que la CAPA se décompose. Elle
   ne se décompose pas : c'est l'écart qui porte plusieurs actions.
2. **Deux endroits pour le même geste.** On pouvait ajouter une action depuis
   la fiche de l'écart et depuis la fiche du dossier, avec deux formulaires et
   deux façons de nommer la même chose.
3. **Le mécanisme central devenu invisible.** Depuis l'ADR 0117, le statut du
   dossier *se déduit* de l'avancement de son travail. Le tableau montrait des
   statuts d'actions sans jamais dire que c'étaient eux qui pilotaient le
   dossier — le lecteur voyait une conséquence, jamais sa cause.

## Décision

La fiche du dossier CAPA remplace le tableau d'actions par une **carte
d'avancement**, qui énonce la règle de l'ADR 0117 et donne le seul geste qui
l'actionne : *démarrer* le travail met le dossier en cours, le *terminer* le
résout.

Trois états sont distingués, parce que les trois existent en base :

| Ce que porte le dossier | Ce que montre la carte |
| --- | --- |
| **Une action** — le cas de tout dossier ouvert depuis l'ADR 0088 | Son statut, et le bouton qui la fait avancer d'un cran |
| **Aucune** — dossiers ouverts avant cette règle | Pourquoi il n'en a pas, et qu'il restera ouvert tant qu'aucune ne lui est rattachée |
| **Plusieurs** — ajoutées depuis l'écart (ADR 0115) | Leur nombre, et un lien vers la fiche de l'écart, où elles se lisent et s'avancent |

Le **bloc des preuves reste** sur la fiche : c'est là qu'elles se déposent, et
elles se rattachent au dossier, pas à une ligne de tableau.

## Conséquences

**Ce qu'on gagne.** Un seul endroit où une action se crée — la fiche de
l'écart, là où elle se décide. Une fiche de dossier qui dit enfin ce qui fait
bouger son statut. Et la disparition d'un formulaire d'ajout en double.

**Ce qu'on perd.** Depuis la fiche d'un dossier à plusieurs actions, on ne
peut plus les avancer sur place : il faut ouvrir l'écart. C'est un clic de
plus pour une minorité de dossiers, et c'est assumé — le prix d'avoir un seul
endroit où ce travail vit.

**Ce qui ne change pas.** Le serveur, le schéma, l'API des actions. Un dossier
peut toujours porter plusieurs actions, et le panneau qui les présente en
tableau continue de servir depuis la fiche de l'écart.

## Tests d'invariant

`capa-detail.fiche.spec.ts` :

- la fiche **ne déclare pas** le panneau de tableau d'actions — la doublure du
  panneau reste déclarée dans le banc exprès, pour que son absence du rendu
  prouve quelque chose plutôt que de passer pour un oubli d'import ;
- les trois états rendent trois textes distincts, repérés par leurs
  `data-test` (`avancement-statut`, `avancement-sans-action`,
  `avancement-plusieurs`) ;
- le bouton d'avancement envoie le `PATCH` d'action attendu, puis recharge le
  dossier — c'est le rechargement qui fait apparaître le statut déduit.

## Références

- ADR 0088 — une CAPA naît avec son action
- ADR 0115 — un dossier peut porter plusieurs actions
- ADR 0117 — le dossier CAPA reflète ses actions
- CLAUDE.md §4.2, §15.1
