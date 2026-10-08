# ADR 0082 — Une édition on-premise, et une licence signée hors ligne

- **Statut** : Accepté
- **Date** : 2026-10-08
- **Owners** : @Couldevlop
- **Portée** : `libs/licensing` (nouveau), api-core (`edition`), api-quality-engine
  (`edition`, `tenantmodules`), web (bandeau, page « Licence », menu), ingress
- **S'inscrit dans** : CLAUDE.md §0 (SaaS, on-premise, hybride), §10.3 (niveaux de
  déploiement), §11.4 (signatures hybrides post-quantiques), §12.2 (souveraineté)

## Contexte

QualitOS se vend en SaaS et en on-premise (CLAUDE.md §0). Seul le SaaS existait :
une installation chez un client aurait ouvert la console éditeur (création de
clients, facturation), n'aurait eu aucun moyen de savoir quels modules le client
a achetés, et aucun moyen de borner son usage. Les sites visés (défense, santé,
industrie sensible) sont souvent sans accès à Internet : tout contrôle doit
fonctionner hors ligne.

## Décisions

### 1. Une seule base de code, une édition choisie à l'installation

`QUALITOS_EDITION=saas|onprem` (défaut `saas`). Une valeur inconnue empêche le
démarrage : retomber en silence sur SaaS ouvrirait la console éditeur chez un
client.

### 2. Une licence signée par l'éditeur, vérifiée sans réseau

Un fichier JSON : un contenu (client, identifiant du client dans l'installation,
palier, modules, nombre d'utilisateurs, dates, délai de grâce) et sa signature.
La signature est **hybride**, Ed25519 **et** ML-DSA-65 (§11.4) : les deux sont
exigées, chacune par la clé publique de l'éditeur **épinglée** dans
l'application. La clé que porte le fichier n'est jamais crue sur parole.

Les clés privées ne quittent jamais le poste de l'éditeur : elles ne sont ni
dans le dépôt (garde-fou `.gitignore`), ni sur un serveur client. Le fichier des
clés privées est créé lisible par son seul propriétaire (POSIX `rw-------`, ACL
réduite au propriétaire sous NTFS). L'outil `LicenseTool` (`keygen`, `issue`,
`inspect`) émet et relit les licences ; `inspect` sans clés vérifie exactement
comme le fera le client.

### 3. Une licence défaillante met en lecture seule, elle n'arrête rien

Absente, mal signée, pas encore en vigueur, ou échue au-delà du délai de grâce :
toute écriture est refusée (403 `license-read-only`), avant la lecture du corps.
Les lectures et les exports passent toujours : les enregistrements qualité sont
des preuves, et une licence échue ne les confisque pas. L'application démarre
quoi qu'il arrive — un serveur qui redémarre en boucle chez un client n'aide
personne à corriger.

Le fichier est relu au plus une fois par minute : renouveler, c'est remplacer le
fichier, sans redémarrer. L'échéance tombe à l'heure même entre deux lectures.

### 4. En on-premise, la licence ouvre les modules

Le socle est toujours ouvert ; la licence ouvre d'office les modules qu'elle
couvre (`*` : tous) et fixe le palier. Le client peut fermer un module de sa
licence, jamais en ouvrir un qu'elle ne couvre pas (409). Aucune ligne
d'activation n'est créée : rien ne peut diverger entre la base et la licence.

### 5. Un seul client, créé au démarrage

Le client de la licence naît au démarrage d'api-core avec l'identifiant que fixe
la licence — celui que portent les comptes dans leur jeton (`tenant_id`) et qui
rattache les données d'une licence à la suivante. Son nom et son plan suivent la
licence.

### 6. La console éditeur n'existe pas en on-premise

Clients, abonnements, prix, factures (`@SaasOnly`) répondent 404. Le menu cache
« Clients » en on-premise et « Licence » en SaaS.

### 7. Le nombre d'utilisateurs

Inviter, créer ou réactiver un membre respecte le nombre de comptes actifs de la
licence (409), vérifié **avant** de créer le compte de connexion. Désactiver un
compte libère une place. `0` = sans limite.

### 8. Ce que voit l'écran

`GET /api/v1/edition` (api-core) : édition, état de la licence et sa raison,
échéance, modules, utilisateurs actifs et plafond. Un bandeau prévient un mois
avant l'échéance, pendant la grâce, et en lecture seule. La page « Licence »
dit tout cela et comment renouveler.

## Conséquences

- Nouveau module Maven `libs/licensing` ; les Dockerfiles le copient, l'ingress
  route `/api/v1/edition` vers api-core.
- La paire de clés de l'éditeur a été générée ; la clé publique est épinglée
  (`libs/licensing/src/main/resources/qualitos-license/editor-public-keys.properties`).
  Changer de clés demande une nouvelle version de l'application : c'est voulu,
  une clé qui se remplacerait par configuration se remplacerait aussi par un tiers.
- Reste à faire (lots suivants) : chart Helm paramétrable et `install.sh`,
  mode léger docker-compose, paquet hors ligne (images, modèles d'IA,
  signatures vérifiables sans Internet), `upgrade.sh`, sauvegarde hors du serveur.
