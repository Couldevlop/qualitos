# Rôle — Auditeur

[← Retour à l'index](../README.md)

## À quoi sert ce rôle

L'**Auditeur** réalise les audits (internes, fournisseurs, préparation de certification) et produit
des rapports **indépendants**. Il dispose d'une **lecture étendue** sur les données qualité, mais
n'a pas vocation à piloter les cycles d'amélioration (c'est le rôle du
[Manager Qualité](manager-qualite.md)).

## Permissions clés (CLAUDE.md §16)

- **Lecture seule étendue** sur les modules qualité.
- **Génération de rapports d'audit** indépendants.

## Écrans et parcours typiques

### Conduire un audit

1. Ouvrir le module **Audits** (`/audits`) — voir [Audits](../modules/audits.md).
2. Suivre la checklist, consigner les constats (findings) et leur criticité.
3. Générer le **rapport d'audit**, qui peut être signé et ancré pour en garantir l'intégrité.

### Préparer une certification

- S'appuyer sur le [Standards Hub](../modules/standards-hub.md) (`/standards`) : exigences par
  clause, preuves rattachées, et **audit blanc** pour identifier les écarts avant l'audit officiel.

### Investiguer

- Consulter les [non-conformités](../modules/non-conformites.md), les
  [CAPA](../modules/capa.md), les indicateurs (`/kpis`) et les preuves rattachées.

## Bonnes pratiques

- **Indépendance** : l'auditeur constate, il ne corrige pas lui-même les écarts.
- **Preuves traçables** : chaque constat s'appuie sur une preuve identifiable.
- **Criticité cohérente** : qualifiez les écarts (mineur / majeur) selon des critères constants.

---

## Variante — Externe (auditeur tiers)

Le rôle **Externe** correspond à un auditeur tiers (organisme certificateur, client auditeur) :

- **Accès limité dans le temps** (fenêtre d'audit).
- **Lecture des preuves** rattachées au périmètre concerné.
- **Signature d'attestations**.

Cet accès est restreint au strict nécessaire et expire automatiquement.

## Liens utiles

- [Audits](../modules/audits.md) · [Standards Hub](../modules/standards-hub.md)
- [Directeur Qualité](directeur-qualite.md)
