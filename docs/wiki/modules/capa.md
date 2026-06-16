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
