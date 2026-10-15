# ADR 0092 — Une personne désignée par un dossier est un utilisateur de l'annuaire

- **Statut** : Accepté
- **Date** : 2026-10-15
- **Owners** : @Couldevlop
- **Portée** : `api-quality-engine` (registre des risques, APQP, coût de la
  qualité, plan d'actions Ishikawa, migration V142), `apps/web`
  (`shared/person-name`, `shared/user-picker`, modules GRC, IA, méthodes,
  administration)
- **S'inscrit dans** : ADR 0091 (annuaire en lecture restreinte), CLAUDE.md §16
  (rôles), §18.2.2 (client et acteur lus dans le jeton)

## Contexte

Décision du responsable produit : « dans la CAPA ou tout autre module, l'owner
doit être un utilisateur, pas une suite de numéros ». Or une quarantaine
d'écrans affichaient l'identifiant brut d'une personne (créateur, approbateur,
pilote, DPO, acteur du journal d'audit…), plusieurs formulaires demandaient de
TAPER un UUID (propriétaire d'une mesure NIS2, membre d'un cercle, soumissionnaire
d'un QMS IA, champion DMAIC, filtre « acteur » du journal), et cinq champs
serveur stockaient un nom en texte libre (propriétaire d'un risque ou d'une
opportunité, responsable d'un livrable APQP, d'une ligne de coût de la qualité,
d'une action Ishikawa), où deux orthographes faisaient deux personnes.

## Décisions

### 1. Choisir : `qos-user-picker`, jamais une saisie d'identifiant

Tout champ qui désigne une personne (owner, pilote, responsable, auditeur,
vérificateur, soumissionnaire, champion, membre, acteur filtré…) se choisit dans
l'annuaire du client par le sélecteur partagé de l'ADR 0091. Sa valeur est le
compte de connexion (`accountId`, le `sub` du jeton) : l'identité de référence
du moteur, jamais l'identifiant interne d'api-core.

### 2. Stocker : le compte, et le nom recopié au choix

Là où le serveur stocke déjà un nom, il gagne le compte à côté :

| Table | Colonne ajoutée | Nom (existant) |
| --- | --- | --- |
| `risk_register_risks`, `risk_register_opportunities` | `owner_user_id` | `owner` |
| `apqp_deliverables` | `owner_user_id` | `owner` |
| `coq_entries` | `responsible_user_id` | `responsible` |
| `ishikawa_actions` | `responsible_user_id` | `responsible` |

Une seule migration, V142, colonnes `UUID NULL`, sans reprise : on ne devine
pas un compte à partir d'un nom. Le nom recopié est le nom affiché de
l'annuaire (l'adresse seulement s'il n'y a pas de nom), borné à la longueur de
la colonne existante. Les DTO exposent `…UserId` à côté du nom ; un compte mal
formé est refusé en 400 avant le domaine ; client et acteur restent lus dans le
jeton. Le PFMEA portait déjà `action_owner_user_id` + `action_owner_name` : seul
l'écran change.

### 3. Afficher : toujours un nom, jamais un identifiant

`qos-person-name` (`shared/person-name`) affiche, dans l'ordre : le nom recopié
par le dossier, sinon le nom lu dans l'annuaire (`DirectoryService.getByAccount`,
mis en cache : une page au même auteur ne fait qu'un appel), sinon un libellé
neutre traduit (« Personne inconnue »). La lecture part au changement d'entrée,
pas à chaque détection. Les modules GRC et IA, dont le serveur ne rend que des
UUID, s'appuient sur cette résolution côté écran : pas de nom ajouté côté
serveur là où l'écran sait le résoudre.

### 4. Reprendre les saisies anciennes sans les casser

Une fiche d'avant l'annuaire n'a qu'un nom tapé : il reste lisible tel quel dans
les listes, exports CSV et tableaux de bord. À l'édition, l'écran affiche « Nom
enregistré : X. Choisissez cette personne dans l'annuaire » ; pour un risque,
un nom qui désigne EXACTEMENT une personne de l'annuaire (nom ou adresse,
accents et casse ignorés) est proposé d'office. Choisir la personne est alors
requis pour enregistrer depuis l'écran ; l'API, elle, accepte encore un nom
seul (clients d'intégration).

### 5. Les flux qui consomment ces champs suivent

L'ouverture d'une CAPA depuis un risque propose directement le compte du
propriétaire quand il en a un (`prefill.ownerAccountId`), et ne retombe sur le
rapprochement par le nom que pour une fiche ancienne. Les suggestions de noms
du registre et les exports restent sur le nom.

## Hors champ

- Les personnes **externes** décrites en clair (DPO désigné, contact DPO d'un
  sous-traitant, partie responsable d'un plan de surveillance IA) : ce sont des
  coordonnées, pas des membres du client.
- Le **propriétaire de KPI** d'un Industry Pack : c'est un rôle, pas une personne.
- Les créateurs implicites (« moi » à la création d'un cycle PDCA, d'un audit 5S,
  d'un document) : ils viennent de la session, rien n'est saisi.

## Tests d'invariant

- Recherche sur les gabarits : aucune interpolation d'un champ `…UserId`,
  `ownerId`, `createdBy`, `authorId`, `auditorId`, `reportedBy`… ne subsiste.
- `PersonNameComponent` : nom recopié prioritaire, résolution par l'annuaire,
  libellé neutre pour un compte inconnu, aucune lecture pour une valeur qui n'a
  pas la forme d'un compte.
- Serveur : aller-retour du compte et du nom (domaine, adaptateurs, service),
  400 sur un compte mal formé (contrôleurs), colonnes `uuid` facultatives (V142
  sur PostgreSQL).

## Références

- ADR 0091 — annuaire en lecture restreinte, `qos-user-picker`
- ADR 0088 — une CAPA, une action, une seule fenêtre (pilote nommé)
