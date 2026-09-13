# ADR 0069 — Une réclamation externe se rejette ; annuler n'est pas rejeter

- **Statut** : Accepté
- **Date** : 2026-09-12
- **Owners** : @Couldevlop
- **Portée** : Module Non-conformités (statuts, transitions, tuiles de l'écran externe)
- **S'inscrit dans** : la fusion des réclamations dans les NC externes (commit
  `86202ef`)

## Contexte

Les réclamations clients ont rejoint les non-conformités d'origine externe. Le
module réclamations avait sept statuts, dont `REJECTED` — « la réclamation a été
instruite et n'est pas retenue ». La fusion a conservé six statuts de NC, et
`REJECTED` est resté en route.

Depuis, une réclamation jugée non fondée n'avait **plus de sortie propre** : on
l'annulait. Or les deux gestes ne disent pas la même chose.

- **Annuler** dit « ce constat n'avait pas lieu d'être » — une saisie en double,
  une erreur de signalement.
- **Rejeter** dit « le constat a été examiné, voici pourquoi il n'est pas retenu ».

C'est la seconde phrase qu'un client vient lire, et celle qu'un auditeur demande.
L'annulation la remplaçait par un silence.

## Décisions

### 1. `REJECTED` est un statut réel, pas un libellé

**Alternative écartée : renommer la tuile « Annulées » en « Rejetées » sur l'écran
externe.** Aucun coût côté serveur, et c'était tentant. Mais le libellé aurait menti
sur la donnée : le statut stocké serait resté `CANCELLED`, l'export et le journal
d'audit auraient dit « annulée », et la distinction se serait perdue exactement là
où elle compte — dans la preuve.

### 2. Réservé à l'origine EXTERNAL, et la garde est au service

Un constat que l'organisation a fait elle-même ne se rejette pas : on le résout, on
le clôt, ou on l'annule. `NcService.reject` refuse donc le geste sur une NC interne,
en 409.

La garde est au **service** et non à l'écran seul : l'écran n'affiche le bouton que
sur l'externe, mais un appel direct à l'API contournerait une règle qui ne vivrait
que là.

### 3. Depuis `OPEN` ou `UNDER_ANALYSIS` seulement, et `REJECTED` est terminal

Une réclamation déjà résolue ou clôturée a reçu une réponse : la reprendre en rejet
réécrirait l'histoire que le client a lue. Et une fois rejetée, elle ne se modifie
plus — même garde que `CLOSED` et `CANCELLED`.

### 4. Le motif est obligatoire, et il a sa colonne

`@NotBlank`, 2000 caractères, vérifié aussi à l'écran (où `Validators.required`
laisserait passer des espaces).

**Alternative écartée : ranger le motif dans `resolution_note`.** Cette colonne dit
ce qui a **résolu** l'écart ; y écrire un refus aurait menti sur ce qui s'est passé,
et un export l'aurait présenté comme une résolution. D'où `rejection_reason` et
`rejected_at`, liées par une contrainte : un rejet sans motif n'est pas défendable,
une date sans motif ne dit rien.

### 5. La contrainte de statut de la base est reprise

La V73 énumère les statuts admis dans un `CHECK`. Sans la reprise de la V128, la
base aurait refusé tout rejet — et le code seul aurait laissé croire que le statut
existait, jusqu'au premier essai en préprod.

### 6. La tuile « Rejetées » ne surmonte que l'écran externe

Même raison que « Annulées » : sur l'interne, elle serait à zéro à perpétuité. Les
tuiles comptent le même périmètre que le tableau qu'elles surmontent — un chiffre
juste au mauvais endroit est un chiffre faux pour qui le lit.

## Conséquences

- Migration **V128** : `rejection_reason`, `rejected_at`, contrainte de cohérence,
  et reprise du `CHECK` des statuts.
- `NcStatistics` gagne `rejected` ; trois appelants du DTO ont été remis d'aplomb.
- Le front expose `REJECTED` au filtre de statut, au badge, et au dialogue de motif
  (`NcRejectDialogComponent`), distinct du dialogue de résolution.
