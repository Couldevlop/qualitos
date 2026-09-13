# ADR 0070 — Le référentiel APQP suit la langue ; ce que le client écrit lui appartient

- **Statut** : Accepté
- **Date** : 2026-09-13
- **Owners** : @Couldevlop
- **Portée** : Module APQP (phases et livrables d'amorçage, en-tête `Accept-Language`)
- **Révise** : [ADR 0068](./0068-un-livrable-apqp-se-coche-et-se-prouve.md) section 8
- **S'inscrit dans** : [ADR 0066](./0066-le-cycle-apqp-appartient-au-tenant.md) —
  le cycle est une donnée du tenant, amorcée à la première lecture

## Contexte

L'ADR 0068 avait tranché, section 8 : **les libellés du référentiel ne passent pas
par la traduction**. Le raisonnement tenait en deux temps. D'abord la propriété — le
cycle est amorcé en base au nom du tenant (ADR 0066), et une donnée ne se traduit
pas. Ensuite la fidélité — « Control plan » désigne un document normatif précis, et
une traduction libre en fait autre chose.

À l'usage, cette décision produisait un défaut visible : un utilisateur qui passait
l'application en anglais gardait un V français sous une interface anglaise, et
réciproquement. L'utilisateur a tranché en une phrase — « le multi-langue doit
fonctionner sans condition » — et c'est le bon arbitrage : le produit promet six
langues (§15.1), pas cinq langues et un écran.

La première décision confondait deux questions. **À qui appartient la ligne** est une
question de propriété. **Dans quelle langue se lit-elle** est une question
d'affichage. Le fait qu'une ligne soit stockée en base ne dit rien sur son auteur :
le texte d'amorçage vient de la plateforme, pas du client.

## Décision

La frontière est déplacée là où elle tient : **ce que la plateforme fournit se
traduit, ce que le client a écrit lui appartient.**

1. Chaque ligne d'amorçage porte sa **clé de référentiel** (`reference_key`,
   migration **V129**), sur les phases comme sur les livrables. Sans clé, aucune
   traduction n'est tentée.
2. À la lecture, le texte suit la langue demandée à **trois conditions cumulées** :
   la ligne vient du référentiel (clé non nulle), la clé est connue de la table de
   traduction, et **personne n'a retouché la ligne** (`updatedAt == createdAt`).
3. Dès qu'un utilisateur reformule un libellé, sa formulation gagne
   **définitivement, dans toutes les langues**. On ne traduit jamais ce qu'un
   utilisateur a écrit : ce serait réécrire sa phrase.
4. La langue vient du **build de l'application** (`LOCALE_ID` → `Accept-Language`),
   pas du navigateur. Le navigateur dit ce que l'utilisateur a réglé dans Chrome ;
   ce qui compte est ce qu'il a choisi dans QualitOS.
5. La réserve de fidélité survit **dans** la traduction et non dans son absence :
   les désignations normatives prennent le terme du métier — « Control plan » devient
   « Plan de surveillance », jamais « plan de contrôle » — et les sigles d'usage
   (PPAP, FAIR, MSA, Cpk) restent tels quels.

Périmètre traduit : 82 entrées dans les six langues servies (`fr` source, `en`, `es`,
`ar`, `ja`, `zh`) — 5 phases (titre, objet, question), 46 livrables, 21 sous-points et
intitulés de mesures.

## Alternatives écartées

**Traduire à l'amorçage, selon la langue du premier lecteur.** Une seule écriture,
aucune condition à la lecture — mais la langue du cycle serait alors figée par le
hasard de qui a ouvert l'écran le premier, et un tenant multilingue n'aurait jamais
les deux.

**Réamorcer le cycle à chaque changement de langue.** Détruit les cases cochées, les
pièces jointes et les formulaires remplis. Le contenu du client n'a pas à payer un
choix d'affichage.

**Stocker les six langues en base, une colonne par langue.** Six colonnes à migrer à
chaque langue ajoutée, et la question de la ligne réécrite reste entière : laquelle
des six versions le client vient-il de corriger ?

## Conséquences

- Migration **V129** (`reference_key VARCHAR(80)` sur les phases et les livrables).
- Les tables de traduction vivent dans le code du service, pas en base : elles sont
  du contenu de plateforme, livré et versionné avec lui.
- Une langue de plus coûte une colonne dans la table de traduction, pas une migration.
- Un libellé retouché sort de la traduction sans retour possible — c'est voulu, et
  c'est dit à l'utilisateur : la réinitialisation du cycle reste la porte de sortie.
- Le coût de lecture reste une comparaison de deux dates et un accès à une table en
  mémoire, par ligne. Aucun aller-retour supplémentaire en base.
