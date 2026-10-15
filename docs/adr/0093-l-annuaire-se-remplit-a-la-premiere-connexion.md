# ADR 0093 — L'annuaire se remplit à la première connexion

- **Statut** : Accepté
- **Date** : 2026-10-15
- **Owners** : @Couldevlop
- **Portée** : `api-core` (`user/MemberProvisioning`, `user/MemberProvisioningFilter`,
  `config/SecurityConfig`)
- **S'inscrit dans** : ADR 0079 (comptes de connexion), ADR 0082 (plafond de la
  licence), ADR 0091 (annuaire en lecture restreinte), ADR 0092 (une personne est
  un utilisateur de l'annuaire), CLAUDE.md §18.2.2 (client lu dans le jeton)

## Contexte

L'annuaire (`app_users`) ne se remplissait que par l'invitation ou l'onboarding
d'un client. Un utilisateur qui existait déjà dans Keycloak — compte de
démonstration, compte importé avec le realm, compte créé dans la console —
pouvait se connecter et travailler sans jamais apparaître dans l'annuaire : on
ne pouvait pas le désigner pilote, titulaire d'un rôle de validation MOC ou
responsable d'une action. La recette en préproduction l'a montré : `app_users`
vide alors que des membres se connectaient tous les jours.

## Décisions

### 1. Le jeton fait foi, à chaque connexion

Après la validation du jeton (filtre placé après `BearerTokenAuthenticationFilter`),
un utilisateur dont le jeton porte un client (`tenant_id`) reçoit son membre s'il
n'en a pas : compte = `sub`, adresse = `email`, nom affiché = `name`, sinon
prénom + nom, sinon `preferred_username`, rôles = rôles de plateforme du jeton
ramenés à la forme du realm (`PlatformRoles.fromToken`, les autres rôles du realm
sont ignorés). S'il en a un, l'adresse, le nom et les rôles qui ont changé dans
Keycloak y sont reportés. L'état actif n'est pas touché : un compte désactivé ne
se connecte pas.

### 2. Jamais pour ce qui n'est pas une personne d'un client

- jeton de service (claim `client_id`/`clientId`, ou `preferred_username` en
  `service-account-…`) : ignoré ;
- jeton sans client ou au client illisible (super administrateur de la
  plateforme) : ignoré ;
- jeton sans adresse exploitable (forme exigée par `chk_app_users_email`) ou au
  `sub` qui n'est pas un UUID : ignoré, avertissement sans donnée personnelle ;
- membre déjà rattaché à un AUTRE client : jamais déplacé (le client d'un membre
  ne change pas après sa création), avertissement.

### 3. Le client n'est jamais créé par la provision

Si la ligne `tenants` du client n'existe pas, rien n'est écrit et un
avertissement structuré le dit (`directory.provisioning.tenant-missing
tenant_id=…`, ni adresse ni nom). La ligne porte la facturation : elle naît de
l'API de création des clients (`/api/v1/tenants`, super administrateur), pas
d'une connexion. L'annuaire de ce client reste vide jusque-là.

### 4. Coût négligeable, sûr sous concurrence

Un cache en mémoire par compte (`sub`) garde l'empreinte du dernier jeton traité
(client, adresse, nom, rôles) cinq minutes : tant qu'elle ne change pas, aucune
lecture ni écriture. La provision tourne dans sa propre transaction. Deux
premières requêtes simultanées se disputent l'insertion : la contrainte unique
`keycloak_id` tranche, la perdante relit la ligne gagnante — jamais de 500. Une
panne de provision ne fait jamais échouer la requête : elle est journalisée et
la requête suivante réessaie. Le plafond de la licence on-premise (ADR 0082)
s'applique à l'inscription comme à l'invitation.

### 5. Aucun courriel

L'utilisateur est déjà entré : ni invitation, ni activation, ni mot de passe.
La provision n'appelle ni le fournisseur d'identité ni aucun envoi. Les envois
existants restent gouvernés par leurs seuls réglages :
`qualitos.identity.invitation-mode` (invitation et onboarding : `email` fait
envoyer par Keycloak `execute-actions-email`, qui exige un SMTP sur le realm),
`qualitos.mail.enabled` + `qualitos.mail.from` (factures d'api-core). Aucune
nouvelle propriété de courriel n'est introduite.

L'adresse provisionnée est celle que l'annuaire rend au sélecteur ; le
paramétrage MOC la recopie au moment où un titulaire est désigné et c'est cette
copie que lisent les notifications des titulaires. Le compte (`accountId` =
`sub` = `keycloak_id`) est le même partout.

## Conséquences

- Un utilisateur Keycloak existant apparaît dans l'annuaire dès sa première
  requête vers api-core, sans action d'administration.
- Renommer ou repromouvoir quelqu'un dans Keycloak se reflète dans l'annuaire à
  sa connexion suivante (au plus cinq minutes de cache).
- La table `tenants` reste la porte d'entrée d'un client : sans elle, pas
  d'annuaire.

## Vérification

- `MemberProvisioningTest` : création, mise à jour, rien d'écrit si rien ne
  change, cache, concurrence (insertion perdue puis relue), conflit persistant,
  membre d'un autre client, jetons de service et sans client, client absent
  (pas d'écriture, avertissement sans donnée personnelle), plafond de licence.
- `MemberProvisioningFilterTest` : une panne ne bloque pas la requête.
- `DirectoryAndUsersOnPostgresTest` (PostgreSQL réel) : inscription et mise à
  jour par l'annuaire, client absent, jeton de service, huit premières requêtes
  simultanées → une seule ligne, aucun appel au fournisseur d'identité.

## Références

CLAUDE.md §16, §18.2.2, §18.2.10 ; ADR 0079, 0082, 0091, 0092.
