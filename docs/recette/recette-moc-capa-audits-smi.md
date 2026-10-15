# Fiche de recette — Gestion des changements (MOC), CAPA, audits, SMI

Environnement : préproduction, <https://preprod.qualitos.openlabconsulting.com/fr/>.
Version recettée : `main-f01c0c4b21a8`.
Références : ADR 0087 (MOC), 0088 (CAPA), 0089 (audits), 0090 (SMI), 0091 (annuaire), 0092 (personnes).

## 1. Comptes de recette

Tous les comptes appartiennent au même client que le compte `demo`. Le mot de
passe commun est consigné dans le fichier local d'identifiants de l'équipe
(non versionné) ; il n'est écrit nulle part dans le dépôt.

| Compte               | Personne          | Rôle                 | Fonction dans la recette                         |
| -------------------- | ----------------- | -------------------- | ------------------------------------------------ |
| `demo.admin`         | Claire Morel      | Administrateur client | Paramètres MOC, ajout de norme au SMI            |
| `demo.demandeur`     | Julien Faure      | Utilisateur          | Crée et conduit les demandes de changement       |
| `demo.resp-service`  | Karim Benali      | Utilisateur          | Titulaire du rôle « Responsable de service »     |
| `demo.hse`           | Sophie Lambert    | Utilisateur          | Titulaire du rôle « HSE »                        |
| `demo.hse-suppleant` | Thomas Girard     | Utilisateur          | Suppléant du rôle « HSE »                        |
| `demo.dir-technique` | Nadia Rousseau    | Utilisateur          | Titulaire du rôle « Directeur technique »        |
| `demo.dir-site`      | Philippe Marchand | Utilisateur          | Titulaire du rôle « Directeur de site »          |
| `demo.qualite`       | Isabelle Fontaine | Manager qualité      | Analyse des changements, CAPA, audits, rapports  |
| `demo.auditeur`      | Marc Delorme      | Auditeur             | Auditeur responsable des audits de recette       |

Particularités de connexion :

- `demo.admin` porte le rôle d'administrateur du client : à la **première
  connexion par le navigateur**, Keycloak demande d'enrôler un code à usage
  unique (application d'authentification). C'est la politique du realm pour ce
  rôle, pas une panne.
- Émettre un rapport d'audit exige une **réauthentification forte** : au premier
  essai, `demo.qualite` devra lui aussi enrôler un code à usage unique.
- Utiliser une fenêtre de navigation privée par compte (ou se déconnecter entre
  deux comptes) : la session Keycloak est partagée par le navigateur.

## 2. Jeu de données en place

| Code           | Objet                                                                  | État au départ                                                                 |
| -------------- | ---------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `MOC-2026-001` | Remplacement de la pompe P-204 par un modèle à débit supérieur          | En validation, **54/100 Critique** ; Responsable de service : approuvé (« Conforme au plan de maintenance. ») ; HSE, Directeur technique, Directeur de site en attente |
| `MOC-2026-002` | Changement de fournisseur du lubrifiant de la ligne 3                  | En analyse, huit domaines évalués, **38/100 Majeur**, non soumise              |
| `MOC-2026-003` | Contournement temporaire de l'alarme niveau haut du réservoir T-12     | Brouillon, temporaire, analyse vide                                            |
| `MOC-2026-004` | Abaissement de la consigne de température du four F-2                  | **Rejetée** par HSE (Sophie Lambert), motif saisi                              |
| `MOC-2026-005` | Remplacement des flexibles hydrauliques de la presse PR-7              | En validation, Mineur 17/100, soumise **il y a quatre jours** (sert au rappel) |
| `MOC-2026-006` | Brouillon à supprimer — essai de recette                               | Brouillon (sert à la suppression)                                              |
| `NC-2026-0006` | Fuite d'huile au palier de la pompe P-204                              | Ouverte, majeure                                                               |
| `AUD-2026-0002`| Audit interne — magasin pièces de rechange                             | Planifié, checklist de trois questions, **non démarré**                       |
| `AUD-2026-0003`| Audit interne — maîtrise des modifications, Unité 2                    | **En cours**, checklist de trois questions, aucun constat                      |
| `AUD-2026-0004`| Audit interne — maintenance des équipements tournants                  | **Clôturé**, deux constats (NC mineure 7.1.3, observation 8.5.6), rapport en brouillon |
| `R-005`        | Rupture du circuit de refroidissement de la ligne 2 en période estivale | À traiter, propriétaire Sophie Lambert                                          |

Paramètres MOC : seuils et poids par défaut (Majeur à partir de 25, Critique à
partir de 50, tous les poids à 1, rappel à 3 jours). Titulaires : Responsable de
service → Karim Benali ; HSE → Sophie Lambert et Thomas Girard ; Directeur
technique → Nadia Rousseau ; Directeur de site → Philippe Marchand.

Le SMI ne contient que l'ISO 9001 : l'ajout de l'ISO 14001 fait partie de la recette.

## 3. Règle transverse : jamais d'identifiant technique

À **chaque** écran parcouru ci-dessous, une personne (demandeur, titulaire,
décideur, pilote de CAPA, auditeur responsable, propriétaire de risque, auteur
d'un événement d'historique) s'affiche par son **nom** — à défaut par son
adresse —, **jamais** par un identifiant du type `8fb8df9f-b44f-…`. Tout UUID
visible est une anomalie à relever avec la capture de l'écran.

## 4. Gestion des changements (MOC)

### MOC-01 — Créer une demande

- **Compte** : `demo.demandeur`.
- **Étapes** : Menu « Gestion des changements » (`/fr/changes`) → « Nouvelle
  demande » → titre « Remplacement du capteur de niveau LT-12 », type
  Permanent, site « Parc de stockage », description et justification → Enregistrer.
- **Attendu** : la demande apparaît en tête de liste avec un code
  `MOC-2026-0NN`, statut Brouillon, demandeur « Julien Faure », classification
  « Non évalué ».

### MOC-02 — Analyse d'impact sur huit domaines et score

- **Compte** : `demo.demandeur` (sa demande de MOC-01).
- **Étapes** : « Envoyer en analyse » → onglet Analyse → évaluer les huit
  domaines un par un : Sécurité Moyen, Santé/Environnement Faible, Qualité
  Faible, Production Élevé, Maintenance Moyen, Coûts Moyen, Réglementaire Aucun,
  Formation Moyen.
- **Attendu** : tant qu'un domaine manque, le score reste « non évalué » ; au
  huitième, le score affiche **54/100** et la classification **Critique** ;
  l'historique porte « Analyse terminée — 54/100 ». L'aperçu des validations
  liste les quatre rôles (Critique).
- **Variante** : remettre Production à Faible → le score descend à 46/100,
  **Majeur**, et l'aperçu ne montre plus « Directeur de site ».

### MOC-03 — Soumission bloquée si l'analyse est incomplète

- **Compte** : `demo.demandeur`.
- **Étapes** : créer une demande, l'envoyer en analyse, n'évaluer que trois
  domaines, cliquer « Soumettre à validation ».
- **Attendu** : refus explicite « Les huit domaines d'impact doivent être
  évalués avant la soumission à validation » ; la demande reste En analyse.

### MOC-04 — Soumission et notifications dans la cloche

- **Compte** : `demo.demandeur`, puis chaque titulaire.
- **Étapes** : soumettre la demande complète de MOC-02 (Critique). Se connecter
  ensuite en `demo.resp-service`, `demo.hse`, `demo.hse-suppleant`,
  `demo.dir-technique`, `demo.dir-site`.
- **Attendu** : statut En validation, score figé. Chaque titulaire et le
  suppléant HSE voient dans la **cloche** « MOC-… attend votre validation (rôle
  …) » ; le lien ouvre la demande. Dans la liste, la demande est marquée « à
  valider par moi ». Le demandeur ne reçoit pas cette notification.

### MOC-05 — Validation par chaque rôle, dont le suppléant

- **Comptes** : `demo.hse-suppleant`, `demo.dir-technique`, `demo.dir-site` sur
  `MOC-2026-001`.
- **Étapes** : ouvrir `MOC-2026-001` → onglet Validations → sur sa propre carte,
  « Approuver » avec un commentaire facultatif. Le **suppléant** HSE statue à la
  place de la titulaire.
- **Attendu** : chaque carte passe à Approuvé avec le nom du décideur (« Thomas
  Girard » pour HSE, et non Sophie Lambert) et la date ; un titulaire ne voit le
  bouton que sur la carte de son rôle. Après la dernière approbation, la demande
  passe **Approuvée** et le demandeur reçoit la notification « approuvée ».
  Une fois HSE tranché par le suppléant, la titulaire `demo.hse` ne doit plus
  pouvoir statuer sur cette carte (« Ce rôle a déjà statué »).

### MOC-06 — Refus sans commentaire impossible

- **Compte** : un titulaire dont la carte est en attente (par exemple
  `demo.dir-site` sur `MOC-2026-001`).
- **Étapes** : « Rejeter » sans saisir de commentaire.
- **Attendu** : le bouton de confirmation reste inactif, ou l'enregistrement est
  refusé avec « Un refus doit être motivé par un commentaire » ; la carte reste
  En attente.

### MOC-07 — Un refus rejette la demande

- **Compte** : `demo.resp-service` sur la demande soumise en MOC-04 (garder
  `MOC-2026-005` en attente pour le rappel, MOC-10).
- **Étapes** : « Rejeter » avec le motif « Capteur non référencé au catalogue
  des pièces de rechange ».
- **Attendu** : la demande passe **Rejetée** immédiatement, les autres cartes ne
  sont plus actionnables ; le demandeur reçoit « MOC-… est rejetée » avec
  le motif ; les autres titulaires en attente (HSE et son suppléant) reçoivent
  « votre validation n'est plus attendue ». L'historique porte le refus et son
  motif.

### MOC-08 — Réouverture d'une demande rejetée

- **Compte** : `demo.demandeur` sur `MOC-2026-004` (rejetée par HSE).
- **Étapes** : lire le motif dans l'historique → « Rouvrir l'analyse » → ajuster
  un domaine → soumettre de nouveau.
- **Attendu** : la demande repart En analyse, ses cartes repartent de zéro à la
  nouvelle soumission ; l'historique conserve l'ancien refus et trace la
  réouverture. Un autre utilisateur non gestionnaire (`demo.hse`) ne voit pas
  le bouton « Rouvrir ».

### MOC-09 — Auto-validation interdite

- **Préparation** (compte `demo.admin`, Paramètres MOC) : ajouter Julien Faure
  comme second titulaire du rôle « Directeur technique ».
- **Compte** : `demo.demandeur` sur une de SES demandes en validation.
- **Attendu** : aucun bouton de décision sur ses propres cartes, même pour un
  rôle dont il est titulaire ; un appel direct est refusé (« Le demandeur ne
  peut pas valider sa propre demande »). Si Julien Faure était **seul**
  titulaire d'un rôle requis, la soumission elle-même serait refusée.
- **Remise en état** : retirer Julien Faure des titulaires.

### MOC-10 — Rappel des validateurs

- **Comptes** : `demo.resp-service`, `demo.hse`, `demo.hse-suppleant`.
- **Étapes** : ouvrir la cloche, puis `MOC-2026-005` → Historique. Ne statuer
  sur cette demande qu'après avoir constaté le rappel : il ne concerne que les
  cartes encore en attente.
- **Attendu** : la demande est soumise depuis plus de trois jours ; au passage
  horaire suivant (premier rappel déjà parti le 15/10 à 07 h 32 UTC), chaque
  titulaire en attente a reçu un rappel (« Rappel :
  MOC-2026-005 attend votre validation depuis 4 jours (rôle …) »), et l'historique porte « Rappel envoyé —
  Responsable de service, HSE ». Le rappel ne se répète pas avant trois jours.

### MOC-11 — Export Excel

- **Compte** : n'importe lequel (`demo.qualite`).
- **Étapes** : liste des demandes → « Exporter ».
- **Attendu** : un classeur `demandes-de-changement-AAAA-MM-JJ.xlsx` avec une
  ligne par demande : code, titre, type, statut, score, classification, niveaux
  des huit domaines et décisions par rôle avec le **nom** des décideurs.

### MOC-12 — Suppression d'un brouillon

- **Compte** : `demo.demandeur` sur `MOC-2026-006`, puis `demo.qualite`.
- **Étapes** : ouvrir le brouillon → « Supprimer » → confirmer.
- **Attendu** : le brouillon disparaît de la liste. Le bouton n'existe pas sur
  une demande en analyse, en validation ou close.

### MOC-13 — Paramètres MOC réservés à l'administrateur

- **Comptes** : `demo.qualite` et `demo.demandeur`, puis `demo.admin`.
- **Étapes** : tenter d'ouvrir `/fr/admin/moc`.
- **Attendu** : seuls les administrateurs du client voient l'entrée de menu et
  l'écran ; pour les autres, l'écran est inaccessible (refus d'accès, aucune
  donnée affichée).

### MOC-14 — Effet des seuils et des poids

- **Compte** : `demo.admin` (Paramètres MOC), puis `demo.demandeur`.
- **Étapes** : passer le seuil critique de 50 à 60 → Enregistrer. Ouvrir une
  demande en analyse et réévaluer un domaine. Puis remettre 50, passer le poids
  de « Production » à 3, et réévaluer de nouveau.
- **Attendu** : la règle est « score < seuil majeur : Mineur ; < seuil critique :
  Majeur ; au-delà : Critique ». Avec le seuil à 60, une demande à 54 devient
  Majeure ; avec Production à poids 3 et niveau Élevé, le score augmente. Les
  demandes **déjà soumises** gardent leur score et leurs cartes figés. Un
  réglage incohérent (seuil majeur ≥ seuil critique, tous les poids à 0) est
  refusé avec un message clair. **Remettre les valeurs par défaut en fin de
  scénario.**

## 5. CAPA

### CAPA-01 — Une seule fenêtre de création, depuis chaque source

- **Compte** : `demo.qualite`.
- **Étapes** : ouvrir « Créer une CAPA » depuis :
  1. la liste des CAPA (`/fr/capa`) ;
  2. la non-conformité `NC-2026-0006` ;
  3. un constat de `AUD-2026-0004` (la NC mineure 7.1.3) ;
  4. le risque `R-005` (`/fr/risques`) ;
  5. le SMI (`/fr/smi`, cellule de la matrice des risques).
- **Attendu** : c'est **la même fenêtre** à chaque fois, la source étant
  pré-renseignée et non modifiable quand on part d'une NC, d'un constat ou d'un
  risque. Une CAPA créée apparaît dans la liste avec un lien retour vers sa source.

### CAPA-02 — Question de vérification d'efficacité

- **Compte** : `demo.qualite`.
- **Étapes** : dans la fenêtre, répondre à la question « Faut-il vérifier
  l'efficacité ? ». Essayer d'enregistrer sans répondre ; puis répondre Oui, en
  désignant Sophie Lambert et une consigne de vérification.
- **Attendu** : sans réponse, l'enregistrement est impossible. Avec « Oui », la
  CAPA porte la vérification, la personne désignée et sa consigne.

### CAPA-03 — Une CAPA, une action

- **Compte** : `demo.qualite`.
- **Étapes** : ouvrir une CAPA créée.
- **Attendu** : plus aucun bouton « Ajouter une action » ; la CAPA est l'action.

### CAPA-04 — Pilote affiché par son nom

- **Compte** : `demo.qualite`, puis `demo.hse`.
- **Étapes** : choisir comme pilote « Sophie Lambert » dans le sélecteur de
  personnes (recherche « Lamb »), puis afficher la CAPA dans la liste et dans
  sa fiche.
- **Attendu** : la recherche propose la personne par son nom ; la liste et la
  fiche affichent « Sophie Lambert », jamais un identifiant. `demo.hse` voit la
  CAPA qui lui est confiée.

## 6. Audits

### AUD-01 — Constat bloqué tant que l'audit n'est pas démarré

- **Compte** : `demo.qualite` sur `AUD-2026-0002` (planifié).
- **Attendu** : pas d'ajout de constat possible (bouton absent ou refus « un
  constat ne se pose que pendant l'audit ») ; après « Démarrer », l'ajout est
  possible.

### AUD-02 — Ajouter un constat sur un audit en cours, puis créer une CAPA

- **Compte** : `demo.qualite` (ou `demo.auditeur`) sur `AUD-2026-0003`.
- **Étapes** : répondre à une question de la checklist, ajouter un constat « NC
  mineure » rattaché à la question 8.5.6, puis « Créer une CAPA » depuis ce
  constat.
- **Attendu** : le constat s'affiche avec son auteur nommé ; la CAPA est liée
  au constat (CAPA-01).

### AUD-03 — Rapport (« Report »)

- **Compte** : `demo.qualite` sur `AUD-2026-0004` (clôturé).
- **Étapes** : onglet Rapport → compléter points forts, axes de progrès,
  conclusion → Enregistrer le brouillon.
- **Attendu** : le rapport reprend la synthèse et les chiffres clés (1 NC
  mineure, 1 observation, taux de conformité de la checklist) ; le brouillon
  se modifie tant qu'il n'est pas émis.

### AUD-04 — Émission réservée

- **Comptes** : `demo.auditeur` puis `demo.qualite`, sur `AUD-2026-0004`.
- **Étapes** : « Émettre le rapport ».
- **Attendu** : `demo.auditeur` ne peut pas émettre (bouton absent ou refus).
  `demo.qualite` est invité à se réauthentifier avec un second facteur, puis le
  rapport est **émis et figé** (plus modifiable), avec la date et le nom de
  l'émetteur.

### AUD-05 — Joindre un document (« Add document »)

- **Compte** : `demo.qualite` sur `AUD-2026-0003`.
- **Étapes** : onglet Documents → joindre un PDF (≤ taille annoncée) → le
  télécharger → le supprimer.
- **Attendu** : le document apparaît avec son nom, sa taille, la date et le nom
  de qui l'a joint ; le téléchargement rend le même fichier ; un type non
  admis ou un fichier trop lourd est refusé avec un message clair.

### AUD-06 — Auditeur responsable

- **Compte** : `demo.qualite`.
- **Étapes** : créer un audit en désignant « Marc Delorme » comme auditeur
  responsable via le sélecteur de personnes ; ouvrir les audits de recette.
- **Attendu** : l'auditeur responsable s'affiche par son nom dans la liste et
  dans la fiche (« Marc Delorme » sur `AUD-2026-0002` à `0004`).

## 7. SMI

### SMI-01 — Ajout de l'ISO 14001 par l'administrateur

- **Compte** : `demo.admin` sur `/fr/smi`.
- **Étapes** : « Ajouter une norme » → l'ISO 14001:2015 est proposée (suggérée,
  structure commune avec l'ISO 9001) → l'ajouter.
- **Attendu** : l'ISO 14001 rejoint l'ISO 9001 dans le SMI ; elle n'est plus
  proposée à l'ajout ensuite.

### SMI-02 — Normes côte à côte

- **Compte** : `demo.admin` puis `demo.qualite`.
- **Attendu** : le tableau de bord SMI affiche les deux normes côte à côte
  (conformité par norme, matrice des exigences) ; aucune promesse de
  « mutualisation des preuves » n'est affichée.

### SMI-03 — Bouton absent pour les autres rôles

- **Comptes** : `demo.qualite`, `demo.auditeur`, `demo.demandeur`.
- **Attendu** : pas de bouton « Ajouter une norme » ; un appel direct est refusé.

## 8. Anomalies relevées à la mise en place du jeu, corrigées (15/10/2026)

Relevées en préparant ce jeu de données, corrigées par la PR #172 (déployée
sur `main-90d489bf6282`) et contrôlées le même jour par l'API, avec les comptes
de recette. Elles restent à confirmer à l'écran pendant la recette.

1. **Annuaire en erreur 500** (`GET /api/v1/directory/users`, `GET
   /api/v1/users`) : le filtre par client d'api-core liait `tenant_id` en
   texte face à une colonne uuid. Contrôle : 200 pour les neuf comptes.
2. **Annuaire vide** : aucune ligne `tenants` ni `app_users` pour le client de
   démonstration. La fiche du client a été créée une fois (`demo-qualitos`) ;
   chaque compte entre dans l'annuaire à sa première connexion (ADR 0093), sans
   aucun e-mail. Contrôle : les neuf comptes apparaissent avec leur nom.
3. **Invitation d'un membre** : Keycloak supprimait `tenant_id` (attribut non
   déclaré dans le profil utilisateur) et les rôles en minuscules violaient la
   contrainte de la base. Contrôle : `demo.invite@demo.qualitos.local` invité
   en mode mot de passe temporaire, porte le client dans Keycloak, aucun e-mail.
4. **Suppression d'un brouillon par son demandeur** : la règle générique des
   suppressions répondait 403. Contrôle : 204 pour le demandeur, 403 pour
   l'auditeur.
5. **Notifications du co-titulaire** : quand un titulaire statue, la
   notification « attend votre validation » des autres titulaires du rôle passe
   en lue et ils reçoivent « votre validation n'est plus attendue ».
   Contrôle : MOC-2026-008, HSE approuvé par `demo.hse`, `demo.hse-suppleant` libéré.
