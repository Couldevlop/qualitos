# Module Audits

[← Retour à l'index](../README.md) · Route : **`/audits`** · Menu : *Qualité opérationnelle › Audits*

## À quoi sert ce module

Le module **Audits** permet de programmer et conduire des audits (internes, fournisseurs,
préparation de certification), de consigner les **constats** (findings) et de produire des
**rapports**.

## Parcours pas à pas

1. **Ouvrir** `/audits` : la liste des audits planifiés et réalisés.
2. **Créer / programmer un audit** : périmètre, référentiel visé, date.
3. **Conduire l'audit** : parcourir la checklist, consigner les constats et leur criticité.
4. **Générer le rapport** d'audit, qui peut être signé et ancré pour en garantir l'intégrité.
5. **Traiter les écarts** : chaque constat peut donner lieu à une [CAPA](capa.md) ou une
   [non-conformité](non-conformites.md).

## Liens avec les autres modules

- En préparation de certification, l'**audit blanc** du [Standards Hub](standards-hub.md) aide à
  cibler les clauses à risque avant l'audit officiel.
- Les écarts non levés alimentent les [CAPA](capa.md) et les indicateurs (`/kpis`).

## Bonnes pratiques

- **Indépendance** : l'auditeur constate, le responsable du périmètre corrige.
- **Criticité constante** : qualifiez les écarts (mineur / majeur) selon des critères stables.
- **Bouclez la levée** : un constat n'est clos que lorsque sa preuve de levée est enregistrée.

## Pour aller plus loin

- Rôle dédié : [Auditeur](../roles/auditeur.md).
