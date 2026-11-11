# Conformité (GRC) — IA Act, RGPD et NIS 2

[← Retour à l'index](../README.md) · Route : **`/compliance`** · Menu : *Conformité (GRC) › Conformité*

## À quoi sert ce module

La page `/compliance` — « Gouvernance, risque & conformité » — est le **point d'entrée unique**
de vingt registres réglementaires, groupés en trois domaines : **IA Act**, **RGPD** et
**NIS 2**. Chaque tuile se déplie et mène aux écrans dédiés.

Ce regroupement est volontaire : vingt entrées dans le menu latéral rendaient le reste de
l'application illisible. Les routes profondes restent toutes accessibles directement.

> **Ce que ce module est, et ce qu'il n'est pas.** Ce sont des **registres de gouvernance** :
> ils tiennent la trace de vos déclarations, décisions, échéances et preuves réglementaires,
> avec les contrôles d'état qu'exige un texte de loi. Ce ne sont **pas** des capacités
> prédictives — aucun de ces écrans n'appelle de modèle d'IA pour décider à votre place. Le
> module « IA Act » gouverne vos systèmes d'IA ; il ne s'en sert pas.

## Parcours pas à pas

1. **Ouvrir** `/compliance`.
2. **Cliquer la tuile** du domaine concerné (IA Act, RGPD, NIS 2) : elle se déplie et liste ses
   registres.
3. **Ouvrir le registre** voulu. Chaque registre fonctionne sur le même principe : une liste
   filtrable, une fiche par enregistrement, et un cycle de vie (brouillon → actif → terminal)
   contrôlé par le serveur.

## Les registres IA Act

Règlement européen sur l'intelligence artificielle. Le **registre des systèmes** est le socle :
tous les autres registres se rattachent à un système qui y est déclaré.

| Écran | Route | À quoi il sert |
|---|---|---|
| Registre des systèmes d'IA | `/ai-systems` | Déclarer vos systèmes d'IA et leur **niveau de risque** : inacceptable, haut risque, risque limité, risque minimal. Point d'entrée obligatoire du domaine. |
| Système qualité IA (QMS) | `/ai-qms` | Les **9 sections obligatoires** du système de management de la qualité exigé de tout fournisseur de système à haut risque (Art. 17). Complétable progressivement en brouillon, puis approuvé — l'approbation enregistre le soumissionnaire **et** l'approbateur, la séparation des rôles étant exigée par l'article. |
| Conformité | `/ai-conformity` | Choisir et suivre la **procédure d'évaluation de la conformité** (Art. 43) : contrôle interne (Annexe VI) ou organisme notifié (Annexe VII) selon le classement du système. |
| Incidents IA | `/ai-incidents` | Déclarer un incident grave : **référence de la notification au régulateur**, analyse de cause racine, puis actions correctives. |
| Base de données UE | `/ai-eudb` | Tenir à jour l'enregistrement du système dans la base européenne. Toute **modification substantielle** doit être déclarée, avec sa date d'effet. L'édition n'est ouverte qu'en brouillon. |
| Analyse d'impact (FRIA) | `/fria` | L'**analyse d'impact sur les droits fondamentaux** (Art. 27), de sa création à son approbation. |
| Surveillance post-marché (PMM) | `/ai-pmm` | Le **plan de surveillance après mise sur le marché** : sa référence et son système ne changent pas ; modifier la fréquence **recalcule la prochaine échéance de revue**. |

## Les registres RGPD

| Écran | Route | À quoi il sert |
|---|---|---|
| Registre (RoPA) | `/ropa` | Le **registre des activités de traitement** (Art. 30). Une fiche passée à l'état actif devient **immuable** ; pour la corriger, il faut l'archiver. Les justifications manquantes sont signalées. |
| Consentements | `/consents` | Enregistrer un consentement et, surtout, son **retrait** (Art. 7§3) — un statut terminal interdit le retrait, parce qu'il n'y a plus de consentement à retirer. |
| Demandes (DSAR) | `/subject-requests` | Les **demandes d'exercice de droits** des personnes : suivi, puis réponse fournie (Art. 12§3), avec au besoin une **URL de preuve archivée**. |
| Mentions d'information | `/privacy-notices` | Les mentions remises aux personnes. Une mention **publiée est immuable** ; seul un brouillon peut être supprimé. |
| Analyses d'impact (DPIA) | `/dpia` | Les **analyses d'impact relatives à la protection des données**, de la création à la révision. |
| Désignations DPO | `/dpo-appointments` | Proposer, activer et modifier la **désignation du délégué à la protection des données**. |
| Rétention | `/retention` | Les **règles de conservation**. Pour une catégorie de données et une date de création, l'écran **calcule la date d'effacement attendue** selon la règle active en vigueur. |
| Transferts hors UE | `/cross-border` | Les **transferts internationaux** et leur encadrement. Un transfert terminé ne se modifie plus. |
| Sous-traitants (DPA) | `/processor-agreements` | Les **contrats de sous-traitance** (Art. 28), jusqu'à leur résiliation. |
| Violations de données | `/breaches` | Les **violations de données personnelles** : le délai légal de **72 h depuis la détection** (Art. 33§1) est rappelé à l'écran ; les mesures d'endiguement font passer la violation en statut « contenue ». |
| Décisions automatisées | `/automated-decisions` | Les traitements de **profilage et de décision automatisée** : profilage seul (Art. 4.4), décision sans effet juridique, décision **avec effet juridique** (Art. 22.1), avec leur base légale. |

## Les registres NIS 2

| Écran | Route | À quoi il sert |
|---|---|---|
| Mesures de cybersécurité | `/nis2-measures` | Le plan des **mesures de gestion des risques cyber** exigées par la directive. Référence et catégorie sont figées une fois la mesure planifiée. |
| Incidents cyber | `/cyber-incidents` | Les **incidents de sécurité** : endiguement, impact constaté, passage en statut « atténué », et **rattachement à une violation de données personnelles** quand l'incident en est une (RGPD Art. 33-34). |

## Comment lire ces registres

- **Les états ne sont pas décoratifs.** Brouillon, actif, publié, terminé : un enregistrement
  actif ou publié devient immuable, parce qu'un registre réglementaire qu'on réécrit ne prouve
  rien. Corriger passe par une nouvelle version ou un archivage.
- **Les échéances sont calculées**, pas saisies : date d'effacement attendue, prochaine revue de
  surveillance, délai de notification. C'est ce qui rend le registre utilisable en contrôle.
- **La séparation des rôles est tenue par le serveur** là où un texte l'exige : celui qui
  soumet n'est pas celui qui approuve.
- **Les références de notification au régulateur** sont des champs de premier plan, et pas des
  commentaires : c'est la pièce que l'autorité demande.

## Bonnes pratiques

- **Déclarez vos systèmes d'IA avant tout le reste** : sans fiche système, les registres de
  conformité, d'incidents et de surveillance n'ont rien à quoi se rattacher.
- **Tenez les 72 h en tête** : à la détection d'une violation, ouvrez la fiche immédiatement,
  même incomplète. L'horloge court depuis la détection, pas depuis la fin de l'analyse.
- **Reliez un incident cyber à sa violation** quand des données personnelles sont touchées : les
  deux textes s'appliquent, et deux dossiers déliés se contredisent tôt ou tard.
- **Ne confondez pas un registre et une preuve** : la preuve est la pièce jointe, le ticket, le
  rapport signé. Le registre dit où la trouver.
- **Les écarts de conformité se traitent comme les autres** : ouvrez une [CAPA](capa.md) ou une
  [non-conformité](non-conformites.md) plutôt que de laisser un commentaire dans la fiche.
