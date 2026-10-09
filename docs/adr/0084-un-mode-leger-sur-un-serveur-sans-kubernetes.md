# ADR 0084 — Un mode léger sur un serveur, sans Kubernetes

- **Statut** : Accepté
- **Date** : 2026-10-10
- **Owners** : @Couldevlop
- **Portée** : `infra/onprem/compose/*` (nouveau), `infra/keycloak/render_realm.py`
  (extrait de `render-realm.sh`), `infra/onprem/tests/*`, CI
- **S'inscrit dans** : ADR 0082 (édition et licence), ADR 0083 (installation
  Kubernetes), CLAUDE.md §10.3

## Contexte

L'ADR 0083 installe QualitOS chez un client sur Kubernetes. Beaucoup de sites
visés (PME, laboratoire, établissement isolé) n'ont ni cluster ni personne pour
en tenir un : un serveur Linux avec Docker est ce qu'ils savent exploiter. Leur
imposer k3s ajoute une couche à apprendre, à mettre à jour et à diagnostiquer.

Le risque d'un second mode d'installation est connu : il dérive. Une variable
ajoutée au chart pour le moteur, un correctif du script d'initialisation de
PostgreSQL, un réglage de Keycloak — chacun oublié d'un côté, et un client porte
un comportement que plus personne n'éprouve.

## Décisions

### 1. docker compose, un installateur, le même `qualitos.conf`

`infra/onprem/compose/qualitos-compose.sh verifier|installer|licence|sauvegarder|etat`.
Le fichier de site est celui de l'installation Kubernetes ; trois clés lui sont
propres : `QOS_TLS_CERT`/`QOS_TLS_KEY` (le certificat en fichiers, faute de
secret TLS), `QOS_COMPOSE_DIR` et `QOS_BACKUP_DIR`.

`installer` est rejouable : la première fois il installe, ensuite il met à jour,
avec un vidage de sûreté juste avant. Les secrets (`.env`, mode 600) sont générés
une fois et jamais régénérés : PostgreSQL, Keycloak et MinIO gardent les leurs
dans leurs volumes, en changer les rendrait injoignables. Les réglages, eux,
suivent `qualitos.conf` à chaque passage.

### 2. Une seule source pour ce qui existe déjà

- **Scripts** : l'initialisation de PostgreSQL, celle du stockage objet et le
  vidage quotidien sont *extraits* des manifestes Kubernetes à l'installation
  (`extract-script.py`), jamais recopiés.
- **Realm** : la règle de rendu est sortie de `render-realm.sh` dans
  `infra/keycloak/render_realm.py`, appelée par les deux modes.
- **Keycloak après démarrage** : les mêmes réglages que `deploy.sh` (anti-force
  brute du realm master, politique de mot de passe, déconnexion, paliers
  d'authentification, compte de service des invitations, secrets des clients
  techniques, annuaire via `ldap-component.py`).
- **Images tierces** : versions lues dans les manifestes, miroir du site par la
  même règle (`qos_mirror_ref`).

### 3. Un banc de parité avec le chart

`compose_parity.py` compare le rendu Helm on-premise et `docker-compose.yml` :
chaque variable que le chart pose en clair pour un service doit exister, avec la
même valeur, dans le service compose. La CI le déroule sur le rendu de référence,
et valide le fichier par `docker compose config`. Une variable ajoutée au chart
sans l'être au mode léger fait échouer la CI.

### 4. Un proxy nginx tient le rôle de l'ingress

Mêmes routes que l'ingress (socle vers `api-core`, IoT, moteur, Keycloak sous
`/auth`, application), TLS terminé ici, 80 renvoyé vers 443. Les pièces jointes
servies par le stockage objet reçoivent toujours leurs en-têtes durcis (lecture
seule, CSP `sandbox`, `nosniff`, `no-store`) : la limite des « snippets » d'un
ingress-nginx récent ne s'applique pas ici. Le résolveur de Docker et des noms en
variables : le proxy démarre même si un service est absent et suit un conteneur
redémarré.

### 5. Durci par défaut, rien d'exposé sauf le proxy

Chaque conteneur : `no-new-privileges`, toutes capacités retirées (rendues une à
une là où l'image en a besoin), système de fichiers en lecture seule pour les
services applicatifs. Seul le proxy publie des ports. Keycloak écoute aussi sur
la boucle locale du serveur, pour que l'installateur le configure sans traverser
le proxy. Les services joignent l'URL publique (émetteur des jetons) par le proxy
du serveur même (`host-gateway`), sans dépendre du DNS du site.

## Conséquences

- Un site sans Kubernetes s'installe en une commande, sur un serveur de 16 Go
  (8 Go si le site fournit son serveur d'IA).
- Pas de haute disponibilité : un serveur, une réplique par service. Un site qui
  en a besoin prend l'installation Kubernetes.
- Les vidages quotidiens restent sur le serveur tant que `QOS_BACKUP_DIR` ne
  pointe pas vers un montage distant. Le guide le dit ; l'envoi hors site
  outillé fait partie du lot suivant (paquet hors ligne, mise à jour, sauvegarde
  hors serveur).
- Le paquet hors ligne (images préchargées) se branche par `QOS_COMPOSE_PULL=false`.
