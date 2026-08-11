# Module CAPA — Actions correctives & préventives

[← Retour à l'index](../README.md) · Route : **`/capa`** · Menu : *Qualité opérationnelle › CAPA*

## À quoi sert ce module

**CAPA** signifie *Corrective And Preventive Actions*. Une CAPA regroupe :

- les actions **correctives** : traiter la cause d'un problème déjà survenu ;
- les actions **préventives** : empêcher qu'un problème (potentiel) ne se produise.

Le module pilote le cycle de vie de chaque plan d'action, de l'ouverture à la vérification
d'efficacité.

## Parcours pas à pas

1. **Ouvrir** `/capa` : la liste des CAPA et leur statut.
2. **Créer une CAPA** : décrire le problème, la cause-racine, et planifier les actions.
3. **Assigner et échéancer** les actions à leurs responsables.
4. **Suivre** l'avancement jusqu'à la clôture.
5. **Vérifier l'efficacité** avant de clore définitivement.

## La nature de chaque action : endiguer n'est pas corriger

Chaque action porte une **nature**, à choisir à la saisie et corrigible ensuite
directement dans le tableau :

| Nature | Ce qu'elle fait | Exemples |
|---|---|---|
| **Endiguement** | Arrête l'effet **sans** toucher à la cause. Temporaire : on la lève une fois la cause traitée. | Trier le lot suspect, arrêter la ligne, prévenir le client, remettre en conformité l'existant |
| **Corrective** | Supprime la cause d'un écart **survenu**. | Recalibrer la machine, corriger la procédure, revoir le paramétrage |
| **Préventive** | Supprime la cause d'un écart qui **n'est pas encore** survenu. | Étendre le contrôle aux lignes voisines, former les équipes |

Sans cette distinction, un dossier où l'on a seulement trié le lot se lirait comme un
dossier où l'on a corrigé la machine : les deux afficheraient « toutes les actions
faites ». Le second seul empêche la récidive.

> Les actions saisies **avant** l'arrivée de cette colonne apparaissent comme
> *correctives* : c'est ce qu'elles étaient, faute d'un moyen de dire l'endiguement.

## Pourquoi un dossier refuse de se clôturer

Quand un dossier ne peut pas encore être clos, l'écran l'annonce **avant** que vous ne
cliquiez, dans un bandeau au-dessus de la vérification d'efficacité, et le bouton
« Efficace — clôturer » reste éteint. Quatre motifs possibles, cumulables :

- **Aucune action enregistrée** — il n'y a rien dont vérifier l'efficacité.
- **Des actions restent à terminer** (le nombre est indiqué).
- **Le dossier ne porte que des mesures d'endiguement** — elles arrêtent l'effet sans
  supprimer la cause. Ajoutez une action corrective ou préventive : **une seule suffit**.
- **Des non-conformités liées sont encore ouvertes** (le nombre est indiqué).

Le bouton **« Non efficace »**, lui, reste toujours accessible : constater que les
actions n'ont pas produit leur effet ne clôt rien, et doit pouvoir se consigner à tout
moment. Le dossier repart alors en traitement.

## Origines d'une CAPA

Une CAPA peut être ouverte manuellement ou déclenchée par un autre module, par exemple :

- depuis une [non-conformité](non-conformites.md) ;
- automatiquement par une **alerte SPC** : lorsque l'analyse [SPC](spc.md) d'un KPI détecte un
  procédé hors-contrôle, une CAPA corrective peut être ouverte (source `SPC_ALERT`).

## Bonnes pratiques

- **Corriger ET prévenir** : une CAPA qui ne fait que corriger laisse la porte ouverte à la
  récidive.
- **Reliez à la cause-racine** : appuyez-vous sur un [Ishikawa](ishikawa.md).
- **L'efficacité se mesure** : une action n'est efficace que si on le vérifie, pas parce qu'elle
  est « faite ».
