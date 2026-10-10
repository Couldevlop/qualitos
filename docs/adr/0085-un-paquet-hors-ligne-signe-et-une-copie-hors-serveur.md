# ADR 0085 — Un paquet hors ligne signé, une copie hors serveur

- **Statut** : Accepté
- **Date** : 2026-10-10
- **Owners** : @Couldevlop
- **Portée** : `infra/onprem/bundle/*` (nouveau), `infra/onprem/lib.sh`,
  `install.sh`, `compose/*`, `infra/k8s/deps/61-backup-offsite.yaml`, `deploy.sh`
- **S'inscrit dans** : ADR 0082 (licence hors ligne), 0083 (Kubernetes), 0084 (mode léger)

## Contexte

Les sites régulés (défense, santé, industrie sensible) n'ont pas d'accès Internet
depuis leurs serveurs. Les installations des ADR 0083 et 0084 tiraient leurs
images d'un registre public et les modèles d'IA d'Internet. Et les vidages
quotidiens restaient sur le disque du serveur qu'ils protègent.

## Décisions

### 1. Un paquet par version, construit chez l'éditeur

`bundle/build-bundle.sh <version> <sortie> [--modeles]` : les images de QualitOS,
toutes les images tierces des manifestes et du mode léger, les scripts
d'installation de la version, et, sur demande, les modèles d'IA. Le site
l'installe en renseignant `QOS_BUNDLE` dans `qualitos.conf`, en mode léger
comme en Kubernetes. Dans ce cas, les images sont poussées vers le registre du
site depuis le poste d'installation, et les modèles sont posés dans l'Ollama du
cluster.

### 2. Rien n'est chargé avant vérification

`SHA256SUMS` couvre chaque fichier du paquet ; il est signé Ed25519 par une clé
de l'éditeur dont la partie publique est **épinglée dans le dépôt**
(`editeur-paquet.pub`). Le site vérifie la signature, puis chaque empreinte,
refuse tout chemin absolu ou remontant, et seulement ensuite charge quoi que ce
soit. Le script de construction refuse de signer avec une autre clé que celle
épinglée. La clé privée ne quitte pas le poste de l'éditeur.

Ed25519 seul, et non la signature hybride des licences (Ed25519 + ML-DSA-65) :
la vérification doit tourner sur le serveur du site avec ses seuls outils, et
`openssl` n'y vérifie ML-DSA qu'à partir de la 3.5, encore rare. La licence, qui
porte les droits, reste hybride ; le paquet passera en hybride quand OpenSSL 3.5
sera courant chez les distributions visées. Exigence : OpenSSL 3.0 (`-rawin`).

### 3. Copie hors serveur, sans suppression

Le CronJob `backup-offsite` (et son équivalent en mode léger, qui en extrait le
script) recopie chaque jour les vidages **et le bucket des pièces jointes** vers
un stockage S3 en HTTPS (`QOS_BACKUP_S3_URL`). Les pièces jointes ne sont pas
dans les vidages : sans elles, une restauration rendrait des NC sans leurs
photos. La copie n'efface jamais rien à destination : une suppression locale
(rétention, erreur, rançongiciel) ne doit pas se propager. La rétention distante
et le verrouillage d'objets (WORM) se règlent sur le bucket de destination. La
copie du jour est relue (taille de chaque vidage) avant d'être déclarée faite.

### 4. Mise à jour

La mise à jour reste la même commande que l'installation (`install.sh` ou
`qualitos-compose.sh installer`) avec la nouvelle version, ou le nouveau paquet.
Elle prend d'abord un vidage de sûreté. Pas de script séparé : un second chemin
divergerait du premier.

## Conséquences

- Un site isolé s'installe et se met à jour sans aucun accès extérieur.
- Le paquet complet avec modèles pèse plusieurs gigaoctets ; sans `--modeles`,
  les fonctions d'IA restent indisponibles jusqu'à ce que le site fournisse un
  serveur d'IA.
- Les vidages copiés hors site ne sont pas chiffrés par QualitOS : le transport
  est en HTTPS (le clair est refusé), et le chiffrement au repos relève du
  stockage de destination. Un chiffrement côté client est une piste ouverte.
