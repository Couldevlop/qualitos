# Installer QualitOS chez un client (on-premise)

Ce guide s'adresse à l'intégrateur qui pose QualitOS sur l'infrastructure d'un
client. Il suppose un cluster Kubernetes disponible (k3s convient très bien sur un
serveur seul) — sans cluster, voir « Mode léger » plus bas. ADR 0082 (édition et
licence), ADR 0083 (installation), ADR 0084 (mode léger).

## Ce qu'il faut avant de commencer

| Élément | Détail |
| --- | --- |
| Cluster Kubernetes | 1.29 ou plus récent. Un serveur k3s suffit pour un site. |
| Contrôleur d'ingress | ingress-nginx (classe `nginx`), ou celle du site via `QOS_INGRESS_CLASS`. |
| Stockage | Une classe de stockage persistante. k3s fournit `local-path`. |
| Certificat | Un certificat pour le nom d'hôte, posé en secret TLS — ou cert-manager avec l'autorité interne du site. |
| DNS | Un enregistrement pour le nom d'hôte, pointant vers l'ingress. Les pods doivent aussi pouvoir le résoudre. |
| Mémoire | 16 Go allouables conseillés avec le modèle d'IA dans le cluster ; 8 Go si le site fournit son propre serveur Ollama. |
| Poste d'installation | `kubectl` et `helm` configurés sur le cluster, `openssl`, `python3`, `curl`. |
| Licence | Le fichier `.lic` remis par l'éditeur. |

## Installer

1. Copier `infra/onprem/qualitos.conf.example` en `qualitos.conf` et le renseigner.
   Chaque réglage y est expliqué. Aucun mot de passe de l'application n'y figure :
   ils sont générés à l'installation et gardés dans des secrets Kubernetes.

2. Poser le certificat (mode `secret`, le plus courant sur site) :

   ```bash
   kubectl create namespace qualitos
   kubectl -n qualitos create secret tls qualitos-tls --cert=cert.pem --key=cle.pem
   ```

3. Vérifier les prérequis. Rien n'est modifié dans le cluster :

   ```bash
   ./infra/onprem/install.sh verifier qualitos.conf
   ```

   Chaque prérequis manquant est nommé, avec la commande pour le corriger.

4. Installer :

   ```bash
   ./infra/onprem/install.sh qualitos.conf 1.4.0
   ```

   À la fin, l'installation affiche l'adresse de l'application et le mot de passe
   **provisoire** de l'administrateur (`admin`). À sa première connexion, il
   choisit son mot de passe et enrôle un second facteur (application TOTP).

5. L'administrateur règle ensuite son organisation dans l'application :
   *Administration › Équipe*, *Rôles et droits*, *Circuits de validation*.

Le modèle d'IA (≈ 6 Go) se télécharge en arrière-plan quand le site a accès à
Internet ; l'application est utilisable tout de suite, les fonctions d'IA le
deviennent à la fin du téléchargement :

```bash
kubectl -n qualitos logs -f job/ollama-models
```

Sur un site sans Internet, utiliser le paquet hors ligne (images et modèles
chargés à l'avance) : voir son guide dédié.

## Relier l'annuaire de l'entreprise

Renseigner `QOS_LDAP_*` dans `qualitos.conf` et relancer l'installation. Les
comptes de l'annuaire :

- se connectent avec leurs identifiants habituels (QualitOS ne modifie jamais
  l'annuaire : lecture seule) ;
- sont rattachés au client de la licence ;
- reçoivent le rôle « utilisateur ». L'administrateur leur donne ensuite d'autres
  rôles dans *Rôles et droits*.

Le mot de passe de liaison peut être passé par la variable d'environnement
`QOS_LDAP_BIND_PASSWORD` plutôt qu'écrit dans le fichier. Il est conservé dans la
base de Keycloak, jamais dans une ConfigMap.

Pour un annuaire non standard, voir `docs/runbooks/keycloak-spi-ldap.md`.

## Renouveler la licence

```bash
./infra/onprem/install.sh licence qualitos.conf
```

(après avoir remplacé le fichier indiqué par `QOS_LICENSE_FILE`). La nouvelle
licence est prise en compte en moins d'une minute, sans redémarrage. La page
*Administration › Licence* affiche l'échéance, les modules et les places.

Sans licence valable, l'application passe en **lecture seule** : tout reste
consultable et exportable, plus rien ne se modifie. Elle prévient un mois avant
l'échéance, puis pendant le délai de grâce.

## Mettre à jour

```bash
./infra/onprem/install.sh qualitos.conf 1.5.0
```

Un vidage de sûreté de la base est pris juste avant la mise à jour. Les
migrations de schéma ne se défont pas : un retour arrière passe par la
restauration de ce vidage (voir `sauvegarde-et-restauration.md`).

## Mode léger : un serveur, sans Kubernetes

Pour un site qui n'a pas de cluster : un serveur Linux avec Docker suffit
(ADR 0084). Même `qualitos.conf`, plus trois clés : `QOS_TLS_CERT` et
`QOS_TLS_KEY` (le certificat et sa clé en fichiers PEM), `QOS_COMPOSE_DIR` (où
vit l'installation, `/opt/qualitos` par défaut) et `QOS_BACKUP_DIR`.

| Élément | Détail |
| --- | --- |
| Serveur | Linux, Docker 24+ avec `docker compose` v2, ports 80 et 443 libres. |
| Mémoire | 16 Go avec le modèle d'IA sur le serveur ; 8 Go si le site fournit son serveur Ollama. |
| Outils | `python3`, `openssl`, `curl`. |
| DNS | Le nom d'hôte pointe vers ce serveur. |

```bash
sudo ./infra/onprem/compose/qualitos-compose.sh verifier  qualitos.conf
sudo ./infra/onprem/compose/qualitos-compose.sh installer qualitos.conf 1.4.0
```

`verifier` contrôle aussi que le certificat est valide, couvre le nom d'hôte et
correspond à sa clé. `installer` est rejouable : relancé avec une nouvelle
version, il prend un vidage de sûreté puis met à jour. Les secrets générés à la
première installation (`/opt/qualitos/.env`, lisible par root seulement) ne
changent jamais ; ne pas les modifier à la main.

| Besoin | Commande |
| --- | --- |
| Renouveler la licence | `qualitos-compose.sh licence qualitos.conf` (prise en compte en moins d'une minute) |
| Vidage immédiat | `qualitos-compose.sh sauvegarder qualitos.conf` |
| État des services | `qualitos-compose.sh etat qualitos.conf` |
| Journaux d'un service | `cd /opt/qualitos && docker compose logs -f api-quality-engine` |

Les vidages quotidiens (14 jours gardés) vont dans `QOS_BACKUP_DIR`. Tant que ce
dossier est sur le disque du serveur, ils ne protègent pas de sa perte : le placer
sur un montage distant, ou le recopier ailleurs chaque jour.

## Site sans Internet : le paquet hors ligne

L'éditeur remet un dossier par version (ADR 0085) : images, scripts
d'installation, modèles d'IA s'ils sont demandés. Le copier sur le serveur (ou le
poste d'installation), puis renseigner `QOS_BUNDLE=<dossier>` dans `qualitos.conf`
et lancer l'installation habituelle — mode léger ou Kubernetes, même commande
pour installer et pour mettre à jour.

- Le paquet est **vérifié avant tout chargement** : signature de l'éditeur sur
  `SHA256SUMS`, puis l'empreinte de chaque fichier. Un paquet modifié en route est
  refusé, et rien n'est posé. OpenSSL 3.0 ou plus récent est requis.
- En Kubernetes, les images sont poussées depuis le poste d'installation vers
  `QOS_REGISTRY` et `QOS_MIRROR_REGISTRY` : `docker login` sur le registre du site
  au préalable.
- Vérification à la main, si besoin :
  `openssl base64 -d -A -in SHA256SUMS.sig -out sig.bin && openssl pkeyutl -verify -pubin -inkey infra/onprem/bundle/editeur-paquet.pub -rawin -in SHA256SUMS -sigfile sig.bin && sha256sum -c SHA256SUMS`

Côté éditeur : `QOS_BUNDLE_KEY=<clé privée> ./infra/onprem/bundle/build-bundle.sh 1.4.0 /chemin/qualitos-1.4.0 --modeles`.

## Copie hors serveur

Renseigner `QOS_BACKUP_S3_URL` (`https://hôte/bucket/préfixe`), `QOS_BACKUP_S3_ACCESS_KEY`
et `QOS_BACKUP_S3_SECRET_KEY`, puis relancer l'installation. Chaque jour, les
vidages **et les pièces jointes** sont recopiés vers ce stockage, en HTTPS
seulement, sans jamais rien y effacer. Sur le bucket de destination : activer le
verrouillage d'objets (WORM) et une règle de rétention.

## Ce qui diffère de la plateforme SaaS

- Un seul client, créé au premier démarrage avec l'identifiant que fixe la licence.
- Pas de console éditeur (clients, abonnements, factures) : elle n'existe pas.
- Les modules ouverts sont ceux de la licence.
- Une réplique par service, et le modèle d'IA dans le cluster si le site n'en a pas.
- Aucune annotation « snippet » d'ingress par défaut : le conteneur web pose
  lui-même ses en-têtes de sécurité (CSP, etc.). Si le contrôleur les autorise
  (`allow-snippet-annotations`), activer `QOS_INGRESS_SNIPPETS=true` : les pièces
  jointes reçoivent alors des en-têtes durcis (bac à sable CSP, lecture seule).
  Sans eux, la défense repose sur la liste fermée des types de pièces acceptés
  (PDF et images), vérifiés à leur signature binaire. Le WAF (`QOS_WAF=true`)
  suppose ces annotations autorisées.

## Diagnostiquer

| Symptôme | Piste |
| --- | --- |
| Bandeau « lecture seule » | *Administration › Licence* dit pourquoi (absente, échue, non vérifiée). |
| Connexion refusée pour un compte de l'annuaire | `kubectl -n qualitos logs deploy/keycloak` ; vérifier `QOS_LDAP_USERS_DN` et le compte de liaison. |
| Fonctions d'IA indisponibles | Modèle encore en téléchargement (`job/ollama-models`), ou `QOS_OLLAMA_URL` injoignable depuis le cluster. |
| Image introuvable (`ImagePullBackOff`) | Registre ou miroir mal renseigné, ou secret d'accès (`QOS_PULL_SECRET`) absent. |
