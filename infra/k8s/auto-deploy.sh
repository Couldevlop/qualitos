#!/usr/bin/env bash
# Déploiement continu TIRÉ PAR LE CLUSTER.
#
# Exécuté périodiquement sur le nœud (minuteur systemd, cf. infra/k8s/systemd/).
# Il surveille une branche, et lorsqu'un nouveau commit y apparaît ET que ses
# images sont publiées, il rejoue infra/k8s/deploy.sh.
#
# POURQUOI TIRER PLUTÔT QUE POUSSER. Un déploiement lancé depuis GitHub
# imposerait d'y déposer une clé d'accès au cluster : quiconque obtiendrait
# cette clé — ou compromettrait le fournisseur — obtiendrait la production.
# En inversant le sens, aucun secret ne quitte l'infrastructure. Le dépôt et les
# images sont publics et n'accordent, par construction, aucune prise.
#
# CONFIGURATION : /etc/qualitos/deploy.env (jamais versionné). Voir
# infra/k8s/systemd/deploy.env.example. Les identifiants d'exploitation
# — mots de passe de base, realm — ne sont PAS ici : deploy.sh les génère dans
# le cluster et ne les en fait jamais sortir.

set -euo pipefail

CONF="${QOS_DEPLOY_ENV:-/etc/qualitos/deploy.env}"
# shellcheck disable=SC1090
[ -f "$CONF" ] && . "$CONF"

REPO_URL="${QOS_REPO_URL:-https://github.com/Couldevlop/qualitos.git}"
BRANCH="${QOS_BRANCH:-main}"
ENVIRONMENT="${QOS_ENVIRONMENT:-preprod}"
WORKDIR="${QOS_WORKDIR:-/var/lib/qualitos/deploy}"
REGISTRY="${QOS_REGISTRY:-ghcr.io/couldevlop/qualitos}"
# Service témoin : si SON image existe pour ce commit, c'est que le pipeline a
# terminé. On évite ainsi de déployer un jeu d'images à moitié publié.
WITNESS="${QOS_WITNESS_IMAGE:-web}"

log() { printf '%s  %s\n' "$(date -Is)" "$*"; }

mkdir -p "$WORKDIR"
if [ ! -d "$WORKDIR/.git" ]; then
  log "clonage initial de $REPO_URL"
  git clone --quiet --branch "$BRANCH" "$REPO_URL" "$WORKDIR"
fi

cd "$WORKDIR"
git remote set-url origin "$REPO_URL"
git fetch --quiet origin "$BRANCH"
git checkout --quiet -B "$BRANCH" "origin/$BRANCH"

SHA="$(git rev-parse --short HEAD)"
TAG="main-$SHA"
STATE="$WORKDIR/.deployed"

if [ -f "$STATE" ] && [ "$(cat "$STATE")" = "$TAG" ]; then
  log "déjà déployé en $TAG — rien à faire"
  exit 0
fi

# On ne déploie que si les images existent RÉELLEMENT. Sans cette vérification,
# le minuteur se déclencherait pendant la construction et échouerait en
# ImagePullBackOff, laissant l'environnement dégradé jusqu'au passage suivant.
if ! skopeo inspect "docker://$REGISTRY/$WITNESS:$TAG" >/dev/null 2>&1; then
  log "images $TAG pas encore publiées — nouvelle tentative au prochain passage"
  exit 0
fi

log "déploiement de $TAG en $ENVIRONMENT"
export KUBECONFIG="${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}"

if ./infra/k8s/deploy.sh "$ENVIRONMENT" "$TAG"; then
  echo "$TAG" > "$STATE"
  log "déploiement de $TAG terminé"
else
  # L'état N'EST PAS mis à jour : le passage suivant réessaiera. Un déploiement
  # échoué doit se rejouer tout seul, pas attendre une intervention.
  log "ÉCHEC du déploiement de $TAG — sera réessayé"
  exit 1
fi
