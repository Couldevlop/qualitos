# ADR 0099 — La GED se valide par des signataires désignés

- **Statut** : Accepté
- **Date** : 2026-10-23
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`docs`, migration V148), fiche et dialogues de
  la GED, menu d'administration
- **Remplace pour la GED** : ADR 0080 (circuits par rôles) et ADR 0095
  (interrupteur du circuit), qui restent en vigueur pour les revues déjà ouvertes
- **Complète** : ADR 0092 (une personne est un utilisateur de l'annuaire),
  ADR 0094 (GED)

## Contexte

Le circuit par rôles de l'ADR 0080 se règle une fois par client : « une
personne ayant tel rôle approuve ». En recette, les responsables qualité ont
demandé l'inverse : choisir, pour chaque document, les personnes qui le
signent, comme le MOC présente ses validateurs, avec une carte par signataire
et les boutons Approuver / Rejeter. Un document de production n'est pas signé
par les mêmes personnes qu'une procédure RH.

La recette a aussi relevé des versions soumises sans fichier : leur validation
portait sur une fiche vide.

## Décisions

### 1. De un à trois signataires, choisis à la soumission

Soumettre une version (`PATCH …/versions/{id}/submit`) exige un corps
`{ signatories: [{ userId, name }] }` de 1 à 3 personnes, dans l'ordre. Chacune
est un utilisateur de l'annuaire (compte de connexion, ADR 0092) ; son nom est
figé avec la décision, pour que la fiche se lise même après un départ. Le
serveur refuse (400) une liste vide ou trop longue, une personne en double, ou
l'auteur de la version.

Les signataires vivent dans `document_version_signatories` (V148) : rang 1 à 3,
une seule fois par rang et par personne et par version (contraintes d'unicité),
décision `PENDING` / `APPROVED` / `REJECTED` avec sa date et son commentaire.
La table porte `tenant_id` : la RLS du client s'applique (ADR 0086).

### 2. On signe dans l'ordre 1 → 2 → 3

Seul le premier signataire en attente peut décider ; un autre reçoit 403. La
dernière approbation fait passer la version à `APPROVED` (approbateur et date
de la version : ce dernier signataire). Un seul rejet, motivé, renvoie la
version en brouillon avec sa raison ; la soumission suivante choisit de
nouveau ses signataires et repart du rang 1.

La vue « À valider par moi » ne montre une version qu'à la personne dont c'est
le tour.

### 3. La désignation fait le droit de signer

Approuver et rejeter ne demandent plus `document.approve` au contrôleur :
le service vérifie que l'utilisateur est le signataire attendu. Un signataire
sans rôle qualité peut donc signer. Soumettre exige toujours `document.submit`.

### 4. Pas de soumission sans fichier

Une version sans fichier ne part pas en signature (409). La création d'un
document exige le fichier à l'écran ; le serveur, lui, garde la règle au seul
moment qui compte, la soumission, parce qu'un brouillon peut se créer avant
l'envoi du fichier.

### 5. Les revues ouvertes avant cette décision vont à leur terme

Une version déjà en revue sous un circuit par rôles n'a pas de signataire :
elle se décide encore par le droit `document.approve` et son passage
(ADR 0080). La soumission n'ouvre plus de passage. L'entrée « Circuits de
validation » quitte le menu d'administration : régler des étapes qui ne
s'appliqueraient à rien induirait en erreur. L'écran et l'API restent tant
que des passages anciens sont ouverts.

### 6. À l'écran, comme le MOC

La fiche montre une carte par signataire (« Signataire 1 », « Signataire 2 »,
« Signataire 3 »), son nom, son état et sa date, son commentaire. Celui dont
c'est le tour voit Approuver (vert) et Rejeter (rouge) ; Rejeter ouvre le motif
obligatoire et « Confirmer le rejet ». Le parcours du panneau suit les mêmes
rangs. Une fois la version approuvée puis publiée, les cartes de sa dernière
version signée restent affichées, en lecture seule : elles sont la trace de qui
a signé, quand, et avec quel commentaire. Le statut affiché d'un document en vigueur se lit « Approuvé ».

## Conséquences

- Une table de plus, sans donnée personnelle au-delà du nom affiché du
  signataire, déjà connu de l'annuaire du client.
- Les tests d'intégration du circuit par rôles ouvrent leur passage
  directement : ils rejouent les revues anciennes.
- ⚠ Dette : l'écran `/admin/circuits` et `circuit_settings` pourront partir
  quand plus aucun passage `DOCUMENT_VERSION` ne sera ouvert.

## Tests d'invariant

- `DocumentServiceTest` : ordre de signature, 403 hors tour, 409 sans fichier
  ou tout décidé, refus des listes invalides, revue ancienne par droit.
- `DocumentControllerTest` : corps de soumission validé (400), signature
  ouverte à un signataire sans rôle qualité.
- `GedIntegrationTest` : cycle complet avec deux signataires et vue « À valider
  par moi » au tour de chacun.
- `GedSignatairesOnPostgresTest` : contraintes de V148 et suppression en
  cascade sur PostgreSQL.
- Côté web : dialogue de soumission, cartes de signature, parcours, fichier
  obligatoire à la création.

## Références

- CLAUDE.md §4.1 (Document Control, workflow d'approbation), §18.2 règle 2
  (`tenant_id` du jeton).
- ADR 0080, 0086, 0087 (validation du MOC), 0092, 0094, 0095.
