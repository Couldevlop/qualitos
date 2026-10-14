# ADR 0087 — La gestion des changements par analyse d'impact et validation par rôles (MOC)

- **Statut** : Accepté
- **Date** : 2026-10-14
- **Owners** : @Couldevlop
- **Portée** : api-quality-engine (`change`, `notifications`, `riskregister`, `smi`,
  `authz`, migration V138), écran `/changes` et `/admin/moc`
- **S'inscrit dans** : CLAUDE.md §4.8 (Change Management), §11.5 (audit),
  §16 (rôles), §18.2.2 (acteur et client issus du jeton), ADR 0078 (droits par
  client), ADR 0080 (circuits, acteur du jeton), ADR 0086 (RLS)

## Contexte

Le module « changements » d'origine (V22) suivait une demande par une liste
d'approbateurs NOMINATIFS ajoutés à la main, des liens vers des objets impactés
et une priorité. Il ne répondait pas à la pratique d'une gestion des changements
(MOC) : on n'y évaluait pas l'impact domaine par domaine, la classe du changement
ne décidait pas de qui valide, et n'importe qui pouvait statuer pour n'importe qui.

La maquette produit décrit un écran unique : liste filtrée à gauche, demande à
droite avec une frise Brouillon → En analyse → En validation → Approuvé, et quatre
onglets (Demande, Analyse d'impact, Validation, Historique).

## Décisions

### 1. Le module évolue en place

La table `change_requests` et ses identifiants restent : le registre des risques
cite un changement comme origine (`PlatformRiskSourceCatalog`) et le tableau de
bord SMI en relit les échéances. Le registre prend désormais la **justification**
comme cause et la **synthèse de l'analyse d'impact** (`impact_summary`, natures
d'impact des domaines touchés, du plus fort au plus faible) comme effet. Le SMI
compte comme « en attente de validation » le statut `IN_VALIDATION`.

L'ancien circuit par personne (`change_approvals`), les liens d'objets
(`change_impacts`), la priorité, le responsable, la date de mise en œuvre et le
motif de rejet sont retirés. La date de mise en œuvre **prévue** est gardée,
facultative, pour les échéances du SMI.

### 2. Cycle de vie

```
DRAFT ─(envoyer en analyse)→ IN_ANALYSIS ─(soumettre)→ IN_VALIDATION
IN_VALIDATION ─(tous les rôles requis approuvent)→ APPROVED
IN_VALIDATION ─(un seul refus, motivé)→ REJECTED ─(rouvrir l'analyse)→ IN_ANALYSIS
```

- Seul un brouillon se supprime ; brouillon et analyse se modifient.
- La soumission exige les **huit domaines évalués** (refus 409
  `moc.analysis-incomplete`, bouton bloqué et expliqué à l'écran).
- Une demande n'est **conduite** (modifiée, supprimée, envoyée en analyse,
  évaluée, soumise, rouverte) que par son **demandeur** (acteur du jeton) ou par un
  **gestionnaire des changements** (droit `change.analyze`) ; 403
  `moc.not-owner` sinon. L'écran le sait par `canManage`, calculé par le serveur.
- Le code `MOC-AAAA-NNN` est attribué par le serveur, séquentiel par client et
  par année (table `change_code_counters`, lue sous verrou) ; un brouillon
  supprimé ne rend pas son numéro, un code repris d'avant le MOC n'est pas
  réattribué.

### 3. Score et classification

Huit domaines : Sécurité, Santé et environnement, Qualité, Production,
Maintenance, Coûts, Réglementaire, Formation. Niveaux : Aucun 0, Faible 1,
Moyen 2, Élevé 3.

```
score = arrondi( Σ(niveau_d × poids_d) / Σ(3 × poids_d) × 100 )
```

Poids par domaine réglé par client, entier 0 à 10, 1 par défaut (la formule
redonne alors la somme sur 24 : la maquette, 13/24, donne **54**). Un domaine de
poids 0 doit être évalué mais ne compte pas ; au moins un poids est non nul.

Seuils par client, entiers, `0 < majeur < critique ≤ 100`, **par défaut 25 et 50** :

| Score        | Classe   |
| ------------ | -------- |
| < 25         | Mineur   |
| 25 – 49      | Majeur   |
| ≥ 50         | Critique |

Ce choix fait de l'exemple de la maquette (54) un changement critique, comme elle
l'indique. Tant qu'un domaine n'est pas évalué : pas de score, classe « Non évalué ».

### 4. Validation par rôles, titulaires et séparation des tâches

Les rôles de validation sont des **fonctions de l'organisation du client**
(« Responsable de service », « HSE », « Directeur technique », « Directeur de
site » par défaut), distincts des rôles d'habilitation de la plateforme. Chaque
rôle porte :

- les classifications qui l'exigent (par défaut Mineur → Responsable de service +
  HSE ; Majeur → + Directeur technique ; Critique → + Directeur de site) ;
- ses **titulaires**, choisis dans l'annuaire du client (principal puis
  suppléants), identifiés par leur identifiant de connexion ; leur adresse est
  **recopiée** à l'affectation, pour que tout utilisateur voie des noms sans lire
  l'annuaire (réservé aux administrateurs).

À la soumission, une carte par rôle requis est créée et **figée** avec le libellé
du rôle : renommer ou réordonner les rôles ne change ni les demandes en
validation ni l'historique. La soumission est refusée (409, rôle nommé) si un
rôle requis n'a aucun titulaire, ou n'a pour titulaire que le demandeur.

Qui peut statuer se résout **au moment de la décision**, sur les titulaires
courants du rôle (absences, départs) :

- seul un titulaire du rôle statue sur sa carte (403 `moc.not-holder`) ;
- **jamais le demandeur** (403 `moc.self-validation`, séparation des tâches) ;
- un même utilisateur titulaire de plusieurs rôles statue sur chaque carte
  séparément ;
- un refus exige un commentaire (400 `moc.comment-required`, et contrainte en
  base) ; **un seul refus rejette** la demande ; toutes les cartes approuvées
  l'approuvent ;
- la décision enregistre l'utilisateur réel et son nom (lu dans le jeton) ;
- deux décisions simultanées sont sérialisées par verrou optimiste sur la demande.

Une demande rejetée peut être rouverte en analyse ; ses cartes sont supprimées
(l'historique garde chaque décision) et recréées à la soumission suivante.

### 5. Paramètres MOC : administrateur du client seulement

Seuils, poids, délai de rappel, rôles, titulaires et rôles requis se règlent dans
« Paramètres MOC » (`/admin/moc`, console d'administration, raccourci depuis
l'écran MOC), en lecture comme en écriture sous `authz.manage` — le droit déjà
réservé à l'administrateur du client pour les circuits de validation (ADR 0080).
Les autres utilisateurs ne voient que les valeurs **appliquées** dans les
demandes (poids affiché près d'un domaine s'il diffère de 1, validations
requises).

Un client sans réglage reçoit les valeurs livrées (aucune ligne en base). Un
enregistrement :

- **reclasse aussitôt** les demandes en brouillon ou en analyse (seuils, poids) ;
- **ne touche jamais** aux demandes en validation, approuvées ou rejetées :
  score, classification et cartes sont figés à la soumission ;
- refuse (409) de retirer un rôle ou de le vider de ses titulaires tant qu'une
  carte en attente l'attend ;
- prévient les **nouveaux** titulaires d'un rôle qu'une demande attend.

Validation serveur : rôles 1 à 20, libellés non vides, uniques sans casse, 100
caractères au plus, au moins un rôle par classification, titulaires distincts
(20 au plus par rôle), délai de rappel 1 à 30 jours.

Droits du catalogue (ADR 0078) : `change.request` (pilotage + utilisateur :
demander et conduire SES demandes), `change.analyze` (pilotage : gérer toutes les
demandes). Statuer ne dépend pas d'un droit de la plateforme mais d'être
titulaire.

### 6. Historique et journal

Chaque transition et chaque décision s'écrit à l'historique de la demande
(`change_history`, le plus récent en haut) et au journal d'audit chaîné
(`moc.*` sur `change-request`, `moc.settings.updated`). Le journal survit à la
suppression d'un brouillon.

### 7. Notifications et rappels, sur les canaux existants

Les notifications in-app étaient déjà adressées à un utilisateur
(`recipientUserId`, filtré par le jeton) : aucun ciblage à ajouter. Le port
gagne une seule opération, `markReadForRecipientAndLink`, pour retirer une tâche
devenue sans objet. Le courriel réutilise la brique SMTP du rappel d'audit
(`AuditReminderMailer`, inactive par défaut) : sans SMTP, aucun envoi et aucune
erreur.

| Événement | Destinataires |
| --------- | ------------- |
| Soumission (et nouvelle soumission) | Titulaires de chaque rôle requis, une notification par personne listant ses rôles ; jamais le demandeur |
| Décision finale | Le demandeur (in-app), avec le motif du refus |
| Refus | Titulaires des cartes encore en attente : leurs notifications « à valider » sont marquées lues, une information les prévient |
| Décision d'un validateur | Ses propres notifications de la demande sont marquées lues |
| Nouveau titulaire d'un rôle attendu | Ce titulaire |
| Rappel | Titulaires courants des rôles en attente |

Le lien mène à `/changes/{id}`, la demande sélectionnée. Pas de nom de personne
dans le texte : le code, l'intitulé et les rôles. Le courriel part vers
l'adresse d'annuaire des titulaires ; le demandeur n'est prévenu qu'en interne
(son adresse n'est pas conservée).

**Rappel** : une tâche planifiée horaire (même forme que le rappel d'audit)
parcourt les cartes en attente des demandes en validation. Délai par client
(3 jours par défaut, 1 à 30) compté en jours calendaires depuis la soumission
puis depuis le dernier rappel (`last_reminded_at` par carte), à l'horloge
injectée. Le droit d'envoyer se réserve par UPDATE conditionnel avant l'envoi :
pas de doublon entre répliques ni dans la journée. Le rappel cesse de lui-même
dès que la carte est décidée ou que la demande quitte la validation. La tâche
lit tous les clients, puis traite client par client en posant son contexte et en
le retirant.

### 8. Reprise des demandes existantes (V138)

| Ancien statut | Nouveau | Raison |
| ------------- | ------- | ------ |
| DRAFT | DRAFT | — |
| SUBMITTED, UNDER_REVIEW | IN_ANALYSIS | sans analyse par domaine, elles ne peuvent pas être devant des validateurs par rôle ; elles repartent en analyse |
| APPROVED | APPROVED | — |
| IMPLEMENTED | APPROVED | elles avaient été approuvées ; la mise en œuvre sort du MOC |
| REJECTED | REJECTED | — |
| CANCELLED | REJECTED | closes sans suite ; rouvrables si besoin |

Rien ne se perd : l'ancien statut (avec date de mise en œuvre et motif), les
décisions de l'ancien circuit et les objets liés sont écrits à l'historique
(`LEGACY_*`) ; l'ancienne « analyse de risque » devient la justification ; toute
demande reprise est réputée permanente.

### 9. Extraction Excel

`GET /api/v1/changes/export.xlsx` (Apache POI, déjà présent pour l'export
produit) : une ligne par demande — code, titre, type, site, demandeur, date,
statut, classification, score, niveau par domaine, décisions (rôle, décision,
auteur, date, commentaire). En-têtes figés, auto-filtre ; cellules écrites comme
texte (pas d'injection de formule).

### 10. Isolation

Toutes les nouvelles tables portent `tenant_id` : la politique RLS leur est posée
automatiquement (ADR 0086). Le client vient toujours du jeton ; une demande d'un
autre client répond 404.

## Conséquences

- ✅ La classe du changement décide de qui valide, réglable par client, et seul un
  titulaire statue — jamais le demandeur.
- ✅ Les demandes engagées ne bougent pas quand le paramétrage change.
- ⚠ L'annuaire vit dans api-core : le moteur qualité ne sait pas si un titulaire a
  été désactivé. L'écran de paramétrage le signale (annuaire lu par
  l'administrateur) ; un compte désactivé ne peut plus se connecter, donc plus
  statuer. La soumission exige un titulaire nommé, pas un titulaire actif.
- ⚠ Les libellés des rôles livrés sont en français : ce sont des données du
  client, renommables.
- ⚠ Les messages des notifications et du classeur sont en français (produits hors
  requête ou pour un tableur), comme le rappel d'audit.

## Tests d'invariant

- `MocScoringTest`, `MocSettingsTest` : formule pondérée (54, poids 0), seuils et
  bornes, règles du paramétrage.
- `MocFlowIntegrationTest` (H2) : cycle complet, soumission bloquée, titulaire et
  suppléant, auto-validation interdite, refus motivé, un refus rejette,
  réouverture, suppression du brouillon seul, conduite réservée au demandeur ou au
  gestionnaire, reclassement des seules demandes ouvertes, rôle attendu non
  retirable, nouveau titulaire prévenu, codes séquentiels, isolation client.
- `MocReminderIntegrationTest` : rien avant le délai, J+3, J+6, pas de doublon,
  arrêt à la décision, délai du client, contexte posé client par client.
- `MocNotifierTest` : destinataires, pas de doublon, demandeur exclu, SMTP absent.
- `MocControllerTest` : droits livrés (403 pour un non-administrateur sur les
  paramètres, un auditeur ne demande rien), codes stables des refus, export.
- `HexagonalArchitectureTest` : `change.domain` sans framework.

## Références

CLAUDE.md §4.8, §11.5, §16, §18.2 ; ADR 0078, 0080, 0081, 0086.
