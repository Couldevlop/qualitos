# Installer QualitOS chez un client (on-premise)

Ce guide s'adresse à l'intégrateur qui pose QualitOS sur l'infrastructure d'un
client. Il suppose un cluster Kubernetes disponible (k3s convient très bien sur un
serveur seul). ADR 0082 (édition et licence), ADR 0083 (installation).

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
