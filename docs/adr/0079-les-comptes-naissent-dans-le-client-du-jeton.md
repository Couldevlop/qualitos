# ADR 0079 — Les comptes naissent dans le client du jeton

- **Statut** : Accepté
- **Date** : 2026-10-08
- **Owners** : @Couldevlop
- **Portée** : api-core (`identity`, `/api/v1/tenants/onboard`, `/api/v1/users/invite`),
  realm Keycloak (`qualitos-provisioner`), `deploy.sh`, écrans `/admin/clients`
  et `/admin/team`
- **S'inscrit dans** : CLAUDE.md §16 (rôles), §21 (abstraction `IdentityProvider`),
  §18.2 (tenant issu du jeton, aucun secret en clair), ADR 0078 (droits par client)

## Contexte

Créer un client n'écrivait qu'une ligne en base : ni compte d'administrateur, ni
module. Aucun écran ne l'appelait. Inviter un membre était impossible :
`POST /api/v1/users` exigeait l'identifiant d'un compte Keycloak déjà créé à la
main. Les rôles réglés dans « Équipe & habilitations » allaient dans une table
qu'aucun service ne lisait — tous autorisent depuis le jeton. Et la lecture
d'un membre par identifiant échappait au filtre par client.

## Décisions

### 1. Un port `IdentityProvider`, un adaptateur Keycloak

api-core parle à l'API d'administration de Keycloak par un compte de service
dédié, `qualitos-provisioner` (client_credentials). Il n'a que `manage-users`,
`view-users`, `query-users` du realm ; les rôles attribuables se lisent par
compte (`role-mappings/realm/available`), sans droit de lecture sur tout le
realm. api-core borne CHAQUE opération au client du jeton de l'appelant avant
d'appeler Keycloak : le compte de service ne connaît aucun client.

### 2. Le client est un attribut du compte

`tenant_id` est posé sur le compte à sa création ; le mappeur existant le publie
dans le jeton. Un compte invité naît dans le client du JETON de l'administrateur
qui invite ; le premier administrateur d'un client naît dans le client qu'on
vient de créer. Aucun corps de requête ne désigne un client.

### 3. Rôles : le compte fait foi

Changer les rôles ou l'état d'un membre agit d'abord sur son compte Keycloak
(c'est lui que le jeton reflète), puis sur la table qui en garde la copie
affichée. `super_admin` n'est jamais attribuable ; les rôles du realm que
QualitOS n'administre pas (offline_access…) ne sont jamais touchés.

### 4. Créer un client : tout ou rien, puis les modules un par un

`POST /api/v1/tenants/onboard` (super administrateur) : l'entreprise et son
administrateur s'enregistrent dans une transaction, le compte est créé au
milieu ; si l'enregistrement échoue, le compte est supprimé (compensation). Les
modules s'ouvrent ensuite par le moteur, avec le jeton du super administrateur
(même chemin que l'abonnement) ; un refus est rendu module par module, sans
annuler le client.

### 5. L'entrée du nouveau membre

Le realm n'a pas de SMTP aujourd'hui : un mot de passe provisoire (16
caractères, sans caractère ambigu) est rendu UNE fois, en réponse non mise en
cache, et Keycloak exige d'en changer à la première connexion. Avec
`IDENTITY_INVITATION_MODE=email`, Keycloak envoie le lien et aucun mot de passe
ne sort. Le mot de passe n'est jamais journalisé.

### 6. Correctif : un membre se lit dans son client

`findById`, `findByKeycloakId`, `update`, `deactivate` vérifient que le membre
est du client du jeton (404 indiscernable de l'absence). Le filtre Hibernate par
client ne s'applique pas à une lecture par clé primaire.

### 7. Déploiement

Le realm ne se réimporte que sur une base vide : `deploy.sh` crée le client sur
un realm en service, pose ses droits (idempotent), lit son secret et le remet à
api-core (`qualitos-api-core-identity`). L'API d'administration se joint par
l'adresse INTERNE (`IDENTITY_KEYCLOAK_URL`) : le WAF de l'ingress refuse ses
écritures. Sans secret, créer ou modifier un compte répond 502 — jamais de
compte annoncé qui n'existe pas.

## Conséquences

- Écran `/admin/clients` (super administrateur) : liste, assistant en trois
  étapes (entreprise, modules avec dépendances cochées d'office, administrateur),
  remise des identifiants.
- Écran Équipe : « Inviter un membre ».
- Les changements de comptes sont journalisés en log structuré ; api-core n'a pas
  encore de journal chaîné (à rapprocher de celui du moteur).

## Vérification

- `KeycloakIdentityProviderTest` (contre un faux serveur HTTP qui vérifie chaque
  appel : création, rôles, mot de passe provisoire, e-mail, compensation, jeton
  du compte de service), `UserServiceTest` (isolation par client, invitation,
  compensation), `TenantOnboardingServiceTest`, contrôleurs.
- Web : `clients.component.spec`, `credential-reveal.component.spec`,
  `tenant-team.component.spec`.
