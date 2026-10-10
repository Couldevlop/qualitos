# ADR 0083 — Installer chez un client avec le même chemin que le SaaS

- **Statut** : Accepté
- **Date** : 2026-10-09
- **Owners** : @Couldevlop
- **Portée** : `infra/k8s/deploy.sh`, `infra/k8s/deps/*`, chart `qualitos`
  (`values-onprem.yaml`, montage de secrets), `infra/onprem/*` (nouveau), CI
- **S'inscrit dans** : ADR 0082 (édition et licence), CLAUDE.md §10.3, §13.4 (LDAP)

## Contexte

Le déploiement savait poser deux environnements, tous deux chez l'éditeur :
`deploy.sh preprod|prod`. Tout ce qui est propre à un site y était figé : le
domaine (une quinzaine d'occurrences), Let's Encrypt, le registre `ghcr.io`,
l'Ollama de l'hôte (`10.42.0.1`), la classe de stockage `local-path`, les
adresses de service dans le namespace `qualitos`. Le realm importait des comptes
de démonstration et de super-administration. Rien ne rattachait les comptes d'un
annuaire d'entreprise à un client.

## Décisions

### 1. Un seul script de déploiement, une troisième cible

`deploy.sh onprem <version> <qualitos.conf>` suit le même chemin que la
préproduction : secrets générés une fois, realm rendu une fois, dépendances,
secrets applicatifs, vidage de sûreté, chart. Deux scripts divergeraient ; un
correctif de la plateforme SaaS doit profiter aux clients, et inversement.

Préprod et prod sont inchangées. Un banc le vérifie : il déroule
`deploy.sh preprod` contre un faux cluster et compare ce qui est appliqué, et le
rendu Helm des deux environnements est identique avant et après.

### 2. Tout ce qui est propre au site vit dans `qualitos.conf`

Hôte, namespace, administrateur, licence, registre et miroir, certificats
(`letsencrypt`, `issuer` interne, ou `secret` déjà émis), classes d'ingress et de
stockage, WAF, modèle d'IA, annuaire. Le fichier est lu ligne à ligne et jamais
`source` : rien de ce qu'il contient ne s'exécute. Une variable d'environnement
l'emporte sur le fichier, ce qui permet de passer le mot de passe de l'annuaire
sans l'écrire.

### 3. Deux fichiers de valeurs

`values-onprem.yaml` porte ce qui vaut pour tout client : édition on-premise,
licence montée en fichier (le chart sait désormais monter un secret, sans
`subPath` pour que le renouvellement se propage sans redémarrage), services
joints par leur nom court (un seul namespace, quel qu'il soit), une réplique par
service. `site-values.sh` génère le reste depuis `qualitos.conf`.

Aucune annotation « snippet » d'ingress-nginx par défaut : les versions récentes
les refusent, et le conteneur web pose déjà sa CSP et ses en-têtes. Seul le WAF,
optionnel, en demande.

### 4. Images tierces depuis le registre du site

Une règle unique (`qos_mirror_ref`) donne la place d'une image tierce dans le
miroir du site : `postgres:17-alpine` → `<miroir>/library/postgres:17-alpine`,
`quay.io/keycloak/keycloak:25.0` → `<miroir>/keycloak/keycloak:25.0`. La même
règle servira à remplir le miroir (paquet hors ligne) : deux règles
divergeraient, et une image introuvable bloque un site isolé.

### 5. Le modèle d'IA : celui du site, sinon dans le cluster

`QOS_OLLAMA_URL` désigne le serveur du site ; vide, un Ollama est déployé dans le
cluster avec un volume pour ses modèles, tirés en arrière-plan. Les données ne
quittent pas le site (§12.2).

### 6. Un realm sans compte de démonstration

En on-premise, le realm ne garde que les comptes de service et un administrateur,
rattaché au client de la licence (`tenant_id` lu dans la licence), avec un mot de
passe provisoire et un second facteur exigés à la première connexion.

### 7. L'annuaire posé par kcadm, rattaché au client

Le fournisseur LDAP / Active Directory est créé après démarrage par `kcadm` : il
vaut ainsi pour un realm neuf comme pour un realm en service, et le mot de passe
de liaison reste dans la base de Keycloak (aucune ConfigMap). Le JSON passe par
l'entrée standard : le mot de passe n'apparaît sur aucune ligne de commande.
Deux mappeurs donnent à tout compte de l'annuaire le `tenant_id` du client et le
rôle « user ». L'annuaire est en lecture seule : QualitOS ne le modifie jamais.

Le fournisseur LDAP standard de Keycloak couvre Active Directory et OpenLDAP. Le
SPI maison (§13.4) reste réservé aux annuaires non standard ; il n'est pas encore
intégré à l'image Keycloak.

### 8. Vérifier avant de toucher au cluster

`install.sh verifier` contrôle outils, cluster, classes d'ingress et de stockage,
certificat, émetteur, accès au registre, mémoire, DNS, licence, annuaire, et
s'arrête avant toute modification si quelque chose manque, en disant comment le
corriger. `install.sh licence` ne fait que remplacer la licence.

## Conséquences

- Un banc bash (`infra/onprem/tests/run-tests.sh`, 74 vérifications) et un job CI
  `infra-onprem` : ShellCheck, bancs contre un faux cluster, rendu du chart dans
  les trois éditions, et refus de tout rendu on-premise qui porterait encore une
  adresse de la plateforme SaaS.
- La création de l'annuaire par `kcadm` n'est pas déroulée par le banc (il faut
  un Keycloak réel) : seul le JSON qu'elle envoie l'est.
- Reste à faire : mode léger docker-compose (serveur sans Kubernetes), paquet
  hors ligne (images, modèles, signatures vérifiables sans Internet),
  `upgrade.sh` avec retour arrière, sauvegarde hors du serveur, SPI LDAP intégré
  à l'image Keycloak.
