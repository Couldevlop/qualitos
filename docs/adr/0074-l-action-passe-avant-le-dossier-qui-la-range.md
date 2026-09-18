# ADR 0074 — L'action passe avant le dossier qui la range, et un type nomme un travail

- **Statut** : Accepté
- **Date** : 2026-09-18
- **Owners** : @Couldevlop
- **Portée** : Module Non-conformités (bouton d'action), module APQP (types de projet),
  module Ishikawa (retour), barre latérale
- **Révise** : l'ADR 0072 §1 (le quatrième type de projet APQP)

## Contexte

Un lot de retouches issues d'un usage réel de la plateforme. Quatre d'entre elles
ne sont pas des ajustements d'affichage : elles changent ce que le produit dit à
l'utilisateur de faire. Ce sont celles-là qui sont consignées ici.

## Décisions

### 1. « Ajouter une action » remplace « Escalader CAPA », et le dossier devient une conséquence

Depuis une non-conformité, le bouton disait **« Escalader CAPA »** et ouvrait une
confirmation. On créait un dossier vide, puis il fallait le retrouver pour y
écrire ce qu'on allait faire.

C'est l'inverse de l'ordre mental. Au moment où l'on constate un écart, on a en
tête **l'action** — « refaire le réglage de la visseuse », « trier le lot 4412 » —
pas le dossier qui la rangera. Le produit demandait donc à l'utilisateur de
nommer une structure avant de nommer son intention.

Le bouton ouvre désormais **le formulaire d'action**, et le dossier CAPA se crée
en chemin s'il n'existe pas encore. Il reste la structure qui porte l'action,
son cycle de vie et sa vérification d'efficacité (ADR 0073) — il cesse
simplement d'être ce qu'on demande à l'utilisateur de faire.

**Ce formulaire est LE MÊME que « Ajouter une action » dans une CAPA**, et non un
jumeau : `CapaActionDialogComponent`, désormais déclaré dans un module à lui
(`CapaActionDialogModule`) pour que les deux écrans l'ouvrent. Deux formulaires
séparés auraient divergé au premier champ ajouté d'un seul côté — la nature de
l'action, l'échéance, le responsable ne se seraient plus posés de la même façon
selon la porte par laquelle on entre.

**Alternative écartée : importer `CapaModule` depuis le module des
non-conformités.** Le plus court chemin, et il aurait greffé les routes `/capa`
sous `/nc` — les écrans CAPA auraient répondu à une seconde adresse qui n'est pas
la leur.

**Conséquence assumée** : le bouton n'est plus barré quand la NC porte déjà une
CAPA. On ajoute une **deuxième** action, une troisième, autant que le traitement
en demande — ce que l'ancien geste, qui créait un dossier, ne pouvait pas faire.

### 2. `NEW_CUSTOMER` devient `MAJOR_MODIFICATION` : un type dit un TRAVAIL, pas un contexte commercial

L'ADR 0072 a fermé les types de projet APQP à quatre : `NPI`, `TOW`,
`NEW_CUSTOMER`, `OTHER`. Le troisième était mal choisi.

« Produit connu, client nouveau » décrit une **situation commerciale**. Le cycle
en V qu'elle ouvre est exactement celui d'un NPI ou d'un ToW selon ce qui change
réellement — donc le type n'apprenait rien à qui filtrait sa liste, alors que
c'est sa seule raison d'être (ADR 0072 §1 : « c'est sur lui qu'on filtre »).

Ce qui manquait, en revanche, c'est la **modification majeure** : un produit déjà
en série dont la définition, l'outillage ou un procédé spécial change assez pour
rouvrir un cycle APQP resserré. C'est le cas le plus fréquent après le NPI, et il
tombait faute de mieux dans `OTHER`, où il se mêlait à tout le reste.

**Les projets déjà saisis sont CONVERTIS, pas supprimés** (V133). Un projet porte
un cycle, des phases, des livrables et des pièces jointes : c'est du travail
réel. Le type le mieux aligné est `MAJOR_MODIFICATION`, qui ouvre le même cycle
resserré.

La migration **dépose** la contrainte `CHECK`, convertit, puis la **repose** avec
le nouveau jeu. L'ordre n'est pas négociable : reposer avant de convertir
échouerait sur la première ligne restée en `NEW_CUSTOMER` et arrêterait la mise à
jour de toute la base. C'est vérifié sur un vrai PostgreSQL
(`ApqpTypeModificationMajeureOnPostgresTest`), y compris le fait que la
contrainte **refuse désormais** l'ancienne valeur — convertir la donnée sans
resserrer la contrainte laisserait une base qui accepte encore une valeur que
plus aucun code ne sait lire.

### 3. Le retour d'un Ishikawa ramène à la non-conformité, pas à la liste

Un Ishikawa part presque toujours d'un écart déjà constaté : on l'ouvre depuis
une fiche de NC, on cherche les causes, et on veut revenir à **cette** fiche pour
la traiter. La flèche de retour ramenait à la liste de tous les diagrammes — il
fallait retrouver sa NC à la main, dans un écran qui ne parle même pas de
non-conformités.

Le diagramme porte déjà `ncId`. Quand il l'a, c'est là qu'on retourne, et
l'infobulle **dit où elle ramène** plutôt que de laisser deviner. Un diagramme
créé hors de toute NC retombe sur la liste, faute de mieux.

### 4. La CAPA rejoint le groupe « Non-conformité » dans la barre latérale

« CAPA » et « Efficacité CAPA » vivaient sous « Opérations », entre la
calibration et le parc IoT. Elles suivent le même geste que la non-conformité :
on constate un écart, puis on le traite. Les ranger ailleurs les éloignait de ce
qui les déclenche.

### 5. Le projet de reprise s'appelle en anglais

La V131 a créé un « Projet par défaut » par client. Ce nom est une **donnée**, pas
un libellé d'interface : il ne traverse pas la traduction et s'affichait en
français quelle que soit la langue — y compris dans le dossier PPAP, remis à un
donneur d'ordre (même parti que le rapport 8D, ADR 0071).

La V133 ne renomme **que les projets restés intacts** : nom d'origine *et*
description d'origine. Un client qui a renommé son projet a fait un choix, et le
lui défaire serait pire que le laisser en français — même règle que pour le
référentiel APQP (ADR 0070) : ce que le client écrit lui appartient.

## Conséquences

- ✅ Migration **V133** : conversion du type, échange de la contrainte, renommage
  conditionnel du projet de reprise.
- ✅ `CapaActionDialogModule` : un module d'un seul composant, importé par la CAPA
  et par les non-conformités.
- ⚠ Le sélecteur de date Material arrive dans le module APQP (échéance d'un
  livrable). Il **défait** un choix documenté — le champ date natif avait été
  retenu pour ne pas imposer un adaptateur de date à toute l'application. Le coût
  est contenu parce qu'APQP est chargé à la demande, mais il est réel, et le
  motif est un vrai défaut du champ natif : il se rend différemment selon le
  navigateur et suit la langue du système, pas celle de l'application.
- ⚠ Le calendrier rend un `Date` à **minuit local** là où le serveur attend
  « aaaa-mm-jj ». Toute traduction par `toISOString()` passerait par UTC et
  enregistrerait l'échéance **la veille** à l'est de Greenwich. Les deux sens
  passent par les composantes locales, et trois bancs tiennent l'aller-retour.

## Tests d'invariant

- `ApqpTypeModificationMajeureOnPostgresTest` (tag `migration`) — conversion,
  projets des autres types intacts, contrainte qui accepte le nouveau type et
  **refuse** l'ancien.
- `ApqpProjetsRepriseOnPostgresTest` — le projet de reprise porte son nom anglais.
- `nc-detail.component.spec.ts` — le formulaire s'ouvre sur le dossier existant
  sans en créer un second ; il s'ouvre sur le dossier **qui vient d'être créé**
  quand il en manquait un ; un serveur muet sur l'identifiant est dit et non
  contourné ; refermer sans rien ajouter ne recharge pas la fiche.
- `apqp-deliverable-detail-dialog.component.spec.ts` — l'aller-retour de date
  dans les deux sens, et l'échéance qu'on efface.

## Références

- `CLAUDE.md` §3.6 (référentiel transverse), §4.2 (CAPA), §5.2 (Industry Packs), §15.1 (i18n)
- ADR 0070 — ce que le client écrit lui appartient
- ADR 0071 — ce qui est remis à un donneur d'ordre se lit en anglais
- ADR 0072 — les types de projet APQP, révisé ici
- ADR 0073 — la vérification d'efficacité, que le dossier CAPA continue de porter
