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

log() { printf '%s  %s\n' "$(date -Is)" "$*"; }

mkdir -p "$WORKDIR"
if [ ! -d "$WORKDIR/.git" ]; then
  log "clonage initial de $REPO_URL"
  git clone --quiet --branch "$BRANCH" "$REPO_URL" "$WORKDIR"
fi

cd "$WORKDIR"
git remote set-url origin "$REPO_URL"
git fetch --quiet origin "$BRANCH"

# RÉINITIALISATION DURE, et non un simple checkout. Cette copie est un CACHE, pas
# un espace de travail : toute modification locale doit être écrasée. Un checkout
# ordinaire échoue dès qu'un fichier suivi a bougé — ce qui s'est produit après
# qu'une normalisation de fins de ligne a été appliquée sur place. Le minuteur
# échouait alors en boucle, toutes les deux minutes, sur « Your local changes
# would be overwritten », pendant que les images attendaient d'être déployées.
git checkout --quiet -B "$BRANCH" "origin/$BRANCH" 2>/dev/null || true
git reset --quiet --hard "origin/$BRANCH"
# Les fichiers non suivis sont supprimés SAUF l'état de déploiement, qui doit
# survivre au nettoyage — c'est lui qui évite de redéployer la même version.
git clean -qfd -e .deployed

# LONGUEUR FIXE, et non `--short` seul. Sans longueur explicite, git choisit
# l'abréviation d'après le nombre d'objets DU DÉPÔT où il tourne : le runner
# GitHub a publié `main-23978fd` (7) pendant que ce clone-ci demandait
# `main-23978fd2` (8). Le minuteur a tourné en boucle sur « images pas encore
# publiées », sans rien de faux dans ses journaux — il cherchait une étiquette
# qui n'existerait jamais. Les deux côtés fixent désormais douze caractères :
# déterministe, et assez long pour rester non ambigu quand le dépôt grossira.
SHA="$(git rev-parse --short=12 HEAD)"
TAG="main-$SHA"
STATE="$WORKDIR/.deployed"

if [ -f "$STATE" ] && [ "$(cat "$STATE")" = "$TAG" ]; then
  log "déjà déployé en $TAG — rien à faire"
  exit 0
fi

# On ne déploie que si TOUTES les images existent RÉELLEMENT.
#
# POURQUOI TOUTES, ET NON UN SERVICE TÉMOIN. La version précédente n'interrogeait
# qu'une seule image — `web` — en supposant que sa présence attestait la fin du
# pipeline. Cette supposition est fausse : la matrice de construction est en
# `fail-fast: false`, chaque service est bâti indépendamment, et l'un peut
# échouer quand les autres aboutissent. C'est arrivé le 23/11/2026 : Maven
# Central a répondu 429 à la construction d'`api-quality-engine` seul. L'image
# témoin était là, le minuteur a déployé, et le moteur de qualité est resté
# cinquante minutes en `ImagePullBackOff` — l'ingress rendant 503 sur toute
# l'API pendant une recette.
#
# La stratégie de remplacement aggrave la faute : sur un nœud unique
# (`maxSurge: 0`, cf. values-preprod.yaml), l'ancien pod s'arrête AVANT que le
# nouveau soit créé. Déployer une étiquette dont une image manque n'est donc pas
# une dégradation partielle, c'est une extinction : on échange un service qui
# fonctionne contre une image qui n'existe pas.
#
# Le correctif n'est pas la stratégie de remplacement — elle est un arbitrage
# assumé — mais le refus de déployer une étiquette incomplète.

# Les services que le chart déploiera réellement : ceux du `values.yaml` de base,
# avec les activations du fichier d'environnement par-dessus. Lire les valeurs
# plutôt qu'une liste écrite ici évite qu'un service ajouté au chart échappe au
# contrôle — l'oubli même qui a coûté la préproduction.
images_du_chart() {
  local base="$WORKDIR/infra/k8s/qualitos/values.yaml"
  local surcouche="$WORKDIR/infra/k8s/qualitos/values-$ENVIRONMENT.yaml"
  [ -f "$surcouche" ] || surcouche="$base"
  awk '
    # Un commentaire en colonne zéro n est pas une clé : il ne referme rien.
    # Sans cette ligne, il fermerait le bloc et TOUS les services suivants
    # sortiraient du contrôle — l oubli silencieux que ce garde-fou existe
    # justement pour empêcher.
    /^#/ { next }
    # Fin du bloc services: une clé de premier niveau le referme.
    /^[^[:space:]]/ { dans = ($0 ~ /^services:/) ; next }
    !dans { next }
    # `  <nom>:` ouvre un service.
    /^  [a-zA-Z0-9_-]+:[[:space:]]*$/ {
      nom = $1; sub(/:$/, "", nom); courant = nom
      if (!(courant in actif)) { actif[courant] = "true" }
      if (!(courant in image)) { image[courant] = courant }
      next
    }
    courant == "" { next }
    /^    enabled:[[:space:]]*/ { actif[courant] = ($2 == "false" ? "false" : "true"); next }
    /^    image:[[:space:]]*/   { image[courant] = $2; next }
    END {
      for (s in actif) { if (actif[s] == "true") { print image[s] } }
    }
  ' "$base" "$surcouche" | sort -u
}

ATTENDUES="$(images_du_chart)"
if [ -z "$ATTENDUES" ]; then
  # Aucune image déduite : le chart a changé de forme, ou le clone est incomplet.
  # On REFUSE de déployer. Prendre une liste vide pour « tout est là » rendrait
  # le garde-fou silencieusement inopérant, ce qui est pire que son absence.
  log "ÉCHEC : aucune image déduite des valeurs du chart pour $ENVIRONMENT — déploiement refusé"
  exit 1
fi

MANQUANTES=""
for img in $ATTENDUES; do
  # `skopeo` dans un `if` : avec `set -e`, un appel nu ferait sortir le script
  # sur la première image absente, sans nommer laquelle ni laisser le passage
  # suivant réessayer.
  if ! skopeo inspect "docker://$REGISTRY/$img:$TAG" >/dev/null 2>&1; then
    MANQUANTES="$MANQUANTES $img"
  fi
done

if [ -n "$MANQUANTES" ]; then
  # Attendre, et non échouer : au plus près d'un commit, les images sont en
  # cours de construction. Le passage suivant reprendra. Les images manquantes
  # sont NOMMÉES — « pas encore publiées » sans dire laquelle a fait perdre une
  # demi-journée de recherche.
  log "images $TAG incomplètes (manquent :$MANQUANTES) — nouvelle tentative au prochain passage"
  exit 0
fi

log "déploiement de $TAG en $ENVIRONMENT"
export KUBECONFIG="${KUBECONFIG:-/etc/rancher/k3s/k3s.yaml}"

if ./infra/k8s/deploy.sh "$ENVIRONMENT" "$TAG"; then
  # Le site de présentation (qualitos.fr) suit la même version, dans son propre
  # namespace. Il passe APRÈS l'application : si les deux ne peuvent pas être
  # déployés, c'est la démonstration qui doit l'être.
  #
  # Son échec N'EMPÊCHE PAS d'enregistrer l'état : l'application, elle, est en
  # place, et rejouer tout le déploiement à cause d'une page vitrine coûterait
  # plus qu'il ne rapporte. L'échec reste visible dans le journal du minuteur.
  #
  # APPELÉ PAR `bash`, PAS EN EXÉCUTABLE. Ce n'est pas un détail de style : la
  # première version écrivait `./infra/k8s/site/deploy.sh`, et ce fichier a été
  # créé depuis un poste Windows, où le bit d'exécution n'existe pas. Git l'a
  # donc enregistré en 100644, le nœud a répondu « Permission denied », le `||`
  # ci-dessous a avalé l'erreur, et le site n'a jamais été déployé — sans que
  # rien ne le dise ailleurs que dans le journal du minuteur. Le bit est remis à
  # 100755, mais `bash` retire la question : un prochain script créé sous
  # Windows ne pourra plus désactiver le site en silence.
  if [ "${QOS_DEPLOY_SITE:-1}" = "1" ]; then
    bash ./infra/k8s/site/deploy.sh "$TAG" || log "ATTENTION : le site n'a pas pu être déployé en $TAG"
  fi

  echo "$TAG" > "$STATE"
  log "déploiement de $TAG terminé"
else
  # L'état N'EST PAS mis à jour : le passage suivant réessaiera. Un déploiement
  # échoué doit se rejouer tout seul, pas attendre une intervention.
  log "ÉCHEC du déploiement de $TAG — sera réessayé"
  exit 1
fi
