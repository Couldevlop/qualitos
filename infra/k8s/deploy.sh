#!/usr/bin/env bash
# Déploiement complet de QualitOS sur un cluster k3s, en UNE commande.
#
#   ./infra/k8s/deploy.sh preprod v0.1.1
#   ./infra/k8s/deploy.sh prod    v0.1.1
#
# Le script est IDEMPOTENT : le relancer sur un environnement déjà en place ne
# recrée rien et ne régénère aucun mot de passe. C'est la propriété qui rend le
# passage en production sûr — promouvoir une version consiste à rejouer la même
# commande avec un tag déjà éprouvé en préproduction, et rien d'autre ne bouge.
#
# Ce qu'il fait, dans l'ordre :
#   1. namespace
#   2. secrets d'infrastructure — CRÉÉS UNE SEULE FOIS. Les régénérer casserait
#      l'accès à une base déjà initialisée avec l'ancien mot de passe.
#   3. realm Keycloak — rendu une seule fois lui aussi, l'import Keycloak n'ayant
#      lieu qu'au premier démarrage sur une base vide.
#   4. dépendances d'état (PostgreSQL, Qdrant, MinIO, Ollama, Keycloak)
#   5. secrets applicatifs, dérivés du secret PostgreSQL
#   6. chart applicatif, au tag demandé
#
# Prérequis : kubectl et helm configurés sur le cluster cible, et un
# enregistrement DNS pointant vers le nœud pour l'hôte de l'environnement.

set -euo pipefail

ENV="${1:-}"
VERSION="${2:-}"

usage() {
  cat >&2 <<USAGE
usage: $0 <preprod|prod> <version>

  version : tag d'image publié par le pipeline de release, par exemple v0.1.1.
            Il est EXIGÉ : déployer « la dernière » sans savoir laquelle est un
            excellent moyen de ne plus pouvoir dire ce qui tourne.

exemples:
  $0 preprod v0.1.1
  $0 prod    v0.1.1
USAGE
  exit 2
}

[ -n "$ENV" ] && [ -n "$VERSION" ] || usage

case "$ENV" in
  preprod) NS=qualitos-preprod; HOST=preprod.qualitos.openlabconsulting.com ;;
  prod)    NS=qualitos;         HOST=qualitos.openlabconsulting.com ;;
  *)       usage ;;
esac

# Le pipeline de release publie ses images SANS le « v » du tag git
# (${GITHUB_REF_NAME#v}). On accepte donc les deux ecritures et on normalise :
# demander « v0.1.1 » pour obtenir un ImagePullBackOff sur un tag inexistant est
# une perte de temps evitable.
IMAGE_TAG="${VERSION#v}"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEPS="$ROOT/infra/k8s/deps"
CHART="$ROOT/infra/k8s/qualitos"
VALUES="$CHART/values-$ENV.yaml"

[ -f "$VALUES" ] || { echo "fichier de valeurs introuvable : $VALUES" >&2; exit 1; }

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
gen() { openssl rand -base64 24 | tr -d '/+=' | cut -c1-24; }

# Un secret n'est créé que s'il est absent. Sans ce garde-fou, relancer le script
# régénérerait les mots de passe alors que PostgreSQL conserve les anciens dans
# son volume : la base deviendrait injoignable sans que rien ne l'explique.
ensure_secret() {
  local name="$1"; shift
  if kubectl -n "$NS" get secret "$name" >/dev/null 2>&1; then
    echo "  secret $name : déjà présent, inchangé"
  else
    kubectl -n "$NS" create secret generic "$name" "$@" >/dev/null
    echo "  secret $name : créé"
  fi
}

say "Environnement $ENV — namespace $NS, hôte $HOST, images $IMAGE_TAG"

if ! getent hosts "$HOST" >/dev/null 2>&1; then
  echo "  ATTENTION : $HOST ne résout pas depuis cette machine." >&2
  echo "  Le déploiement se poursuit, mais cert-manager ne pourra pas obtenir de" >&2
  echo "  certificat et l'accès public restera indisponible tant que" >&2
  echo "  l'enregistrement DNS n'existe pas." >&2
fi

say "1/6 Namespace"
sed "s/__NAMESPACE__/$NS/g" "$DEPS/00-namespace.yaml" | kubectl apply -f -

say "2/6 Secrets d'infrastructure"
ensure_secret qualitos-postgres \
  --from-literal=POSTGRES_USER=qualitos \
  --from-literal=POSTGRES_PASSWORD="$(gen)" \
  --from-literal=NLQ_RO_PASSWORD="$(gen)"
ensure_secret qualitos-keycloak \
  --from-literal=KEYCLOAK_ADMIN=admin \
  --from-literal=KEYCLOAK_ADMIN_PASSWORD="$(gen)"
# Identifiants du stockage objet. Même règle que PostgreSQL : générés une seule
# fois, jamais régénérés — MinIO conserve les siens dans son volume, et les
# changer ici rendrait le stockage injoignable sans que rien ne l'explique.
#
# DEUX comptes, et non un seul. Le compte racine n'administre que MinIO ; celui
# de l'application ne sait que lire, écrire et supprimer des objets dans le
# bucket des pièces jointes (politique posée par le Job minio-init). Faire
# travailler l'engine sous le compte racine reviendrait à lui donner le droit de
# rendre le bucket public ou de l'effacer — pouvoirs dont il n'a aucun usage, et
# qu'un engine compromis retournerait contre le dossier d'audit.
ensure_secret qualitos-minio \
  --from-literal=MINIO_ROOT_USER=qualitos \
  --from-literal=MINIO_ROOT_PASSWORD="$(gen)" \
  --from-literal=STORAGE_S3_ACCESS_KEY=qualitos-engine \
  --from-literal=STORAGE_S3_SECRET_KEY="$(gen)"

say "3/6 Realm Keycloak"
if kubectl -n "$NS" get configmap qualitos-keycloak-realm >/dev/null 2>&1; then
  echo "  realm déjà publié, inchangé (l'import Keycloak n'a lieu qu'au premier démarrage)"
else
  QOS_HOST="$HOST" "$DEPS/render-realm.sh" "$NS"
fi

say "4/6 Dépendances d'état"
kubectl -n "$NS" apply -f "$DEPS/10-postgres.yaml" \
                       -f "$DEPS/30-qdrant.yaml" \
                       -f "$DEPS/40-ollama-external.yaml" \
                       -f "$DEPS/60-backup.yaml"
sed "s/__QOS_HOST__/$HOST/g" "$DEPS/20-keycloak.yaml" | kubectl -n "$NS" apply -f -

# Le travail d'initialisation du bucket est IMMUABLE une fois créé : le
# supprimer d'abord est ce qui rend le déploiement rejouable. Sans cela, la
# deuxième exécution échouerait sur un champ non modifiable, et l'échec
# porterait sur le bucket alors que rien ne va mal.
kubectl -n "$NS" delete job minio-init --ignore-not-found >/dev/null
kubectl -n "$NS" apply -f "$DEPS/50-minio.yaml"

kubectl -n "$NS" rollout status deploy/postgres --timeout=180s
kubectl -n "$NS" rollout status deploy/qdrant   --timeout=180s
kubectl -n "$NS" rollout status deploy/minio    --timeout=180s
kubectl -n "$NS" rollout status deploy/keycloak --timeout=420s

# Le bucket doit exister AVANT que l'engine n'accepte un dépôt : sans lui, la
# première pièce jointe échouerait sur « NoSuchBucket », longtemps après le
# déploiement et loin de sa cause.
if ! kubectl -n "$NS" wait --for=condition=complete job/minio-init --timeout=120s >/dev/null; then
  echo "  ATTENTION : le bucket de stockage n'a pas pu être créé." >&2
  echo "  Les pièces jointes (photos de NC, preuves CAPA) échoueront au dépôt." >&2
  kubectl -n "$NS" logs job/minio-init --tail=20 >&2 || true
fi

say "5/6 Secrets applicatifs"
# Les services Spring lisent DB_USER et DB_PASSWORD (cf. application.yml). On les
# dérive du secret PostgreSQL plutôt que de les saisir deux fois : deux sources
# pour le même mot de passe finissent toujours par diverger.
PG_USER="$(kubectl -n "$NS" get secret qualitos-postgres -o jsonpath='{.data.POSTGRES_USER}' | base64 -d)"
PG_PWD="$(kubectl -n "$NS" get secret qualitos-postgres -o jsonpath='{.data.POSTGRES_PASSWORD}' | base64 -d)"
NLQ_PWD="$(kubectl -n "$NS" get secret qualitos-postgres -o jsonpath='{.data.NLQ_RO_PASSWORD}' | base64 -d)"

for svc in api-core api-quality-engine; do
  kubectl -n "$NS" create secret generic "qualitos-$svc" \
    --from-literal=DB_USER="$PG_USER" \
    --from-literal=DB_PASSWORD="$PG_PWD" \
    --dry-run=client -o yaml | kubectl apply -f - >/dev/null
  echo "  secret qualitos-$svc : à jour"
done

# api-iot-hub n'utilise PAS les mêmes noms de variables que les deux autres
# services : IOT_DB_USERNAME / IOT_DB_PASSWORD (cf. son application.yml). Lui
# fournir DB_USER / DB_PASSWORD ne provoquait aucune erreur visible — le service
# retombait silencieusement sur ses valeurs par défaut et échouait bien plus loin
# sur « password authentication failed », ce qui laissait croire à un secret
# erroné alors que le secret était juste ignoré.
kubectl -n "$NS" create secret generic qualitos-api-iot-hub \
  --from-literal=IOT_DB_USERNAME="$PG_USER" \
  --from-literal=IOT_DB_PASSWORD="$PG_PWD" \
  --dry-run=client -o yaml | kubectl apply -f - >/dev/null
echo "  secret qualitos-api-iot-hub : à jour"

# Secret du client de service IA. Il est GÉNÉRÉ PAR KEYCLOAK à l'import du realm ;
# on le lit ici plutôt que de l'inventer, pour qu'il n'existe qu'à un seul endroit.
# Sans lui, l'engine ne peut obtenir aucun jeton et toutes les fonctions d'IA
# répondent 502 — exactement la panne rencontrée au premier déploiement.
KC_POD="$(kubectl -n "$NS" get pod -l app.kubernetes.io/name=keycloak --field-selector=status.phase=Running -o name | head -1)"
AI_SECRET=""
if [ -n "$KC_POD" ]; then
  KC_ADMIN="$(kubectl -n "$NS" get secret qualitos-keycloak -o jsonpath='{.data.KEYCLOAK_ADMIN}' | base64 -d)"
  KC_PWD="$(kubectl -n "$NS" get secret qualitos-keycloak -o jsonpath='{.data.KEYCLOAK_ADMIN_PASSWORD}' | base64 -d)"
  kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh config credentials \
    --server http://localhost:8080/auth --realm master --user "$KC_ADMIN" --password "$KC_PWD" >/dev/null 2>&1 || true

  # Anti-force-brute sur le realm MASTER. La console d'administration est
  # publiquement joignable sous /auth/admin : sans cette protection, le compte
  # super-admin peut être martelé sans limite. Le realm master N'EST PAS dans
  # realm-export.json — Keycloak le crée lui-même, jamais par import — il ne peut
  # donc être durci qu'ICI, après démarrage. Le realm applicatif `qualitos`, lui,
  # porte déjà sa protection et sa politique de mot de passe dans realm-export.json.
  # Idempotent : rejoué à chaque déploiement sans effet de bord.
  if kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh update realms/master \
       -s bruteForceProtected=true -s failureFactor=10 \
       -s waitIncrementSeconds=60 -s maxFailureWaitSeconds=900 >/dev/null 2>&1; then
    echo "  realm master : anti-force-brute actif"
  else
    echo "  ATTENTION : anti-force-brute du realm master non appliqué" >&2
  fi

  # Politique de mot de passe du realm `qualitos`, posée APRÈS l'import et non
  # dans realm-export.json À DESSEIN : à l'import, Keycloak valide les mots de
  # passe des comptes déjà présents (dont `demo/demo`) contre la politique et
  # refuse de démarrer si l'un ne la respecte pas (« invalidPasswordMinUpperCase »).
  # Appliquée par kcadm, elle ne contraint que les mots de passe FUTURS et laisse
  # les comptes de démonstration intacts. Idempotent.
  if kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh update realms/qualitos \
       -s 'passwordPolicy=length(12) and upperCase(1) and lowerCase(1) and digits(1) and notUsername(undefined)' >/dev/null 2>&1; then
    echo "  realm qualitos : politique de mot de passe posée"
  else
    echo "  ATTENTION : politique de mot de passe du realm qualitos non appliquée" >&2
  fi

  # URI de POST-DÉCONNEXION du client web. Posée ICI et pas seulement dans le
  # realm rendu, parce que l'import Keycloak n'a lieu qu'au TOUT PREMIER
  # démarrage : un environnement déjà installé ne verrait jamais la correction.
  #
  # Ce que l'oubli produisait, mesuré sur la préproduction : la connexion
  # fonctionne, la déconnexion répond « Invalid redirect uri » (HTTP 400) et
  # l'utilisateur reste bloqué sur une page d'erreur Keycloak. Le réglage est
  # DISTINCT des URI de redirection et Keycloak ne retombe pas dessus : attribut
  # absent = aucune redirection autorisée après déconnexion.
  WEB_CID="$(kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh get clients -r qualitos     -q clientId=qualitos-web --fields id --format csv --noquotes 2>/dev/null | tr -d '' | head -1)"
  if [ -n "$WEB_CID" ] && kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh update        "clients/$WEB_CID" -r qualitos        -s "attributes.\"post.logout.redirect.uris\"=https://$HOST/*" >/dev/null 2>&1; then
    echo "  client qualitos-web : redirection de déconnexion autorisée"
  else
    echo "  ATTENTION : URI de post-déconnexion non posée — la déconnexion" >&2
    echo "  répondra « Invalid redirect uri » et laissera l'utilisateur bloqué." >&2
  fi

  # Authentification par PALIERS (silver / gold). Sans elle, le jeton ne porte
  # aucune trace du second facteur et TOUTE approbation de control plan répond
  # 403 « step-up-required » — le contrôle est fail-closed à dessein (ADR 0059).
  #
  # Appelée depuis le déploiement et non laissée à un passage manuel : une
  # bascule d'environnement qui dépend d'une commande qu'on doit penser à taper
  # est une bascule qu'on oublie. Le script se sait rejouable et ne reconstruit
  # rien s'il trouve le realm déjà en place.
  #
  # L'échec n'ARRÊTE PAS le déploiement — il vaut mieux une plateforme en ligne
  # dont une action critique est refusée qu'une livraison bloquée — mais il est
  # signalé bruyamment, parce que le symptôme (403 à l'approbation) n'évoque pas
  # de lui-même sa cause.
  if KC_URL="https://$HOST/auth" KC_REALM=qualitos      KC_ADMIN="$KC_ADMIN" KC_ADMIN_PASSWORD="$KC_PWD"      "$ROOT/infra/keycloak/apply-step-up.sh" >/dev/null 2>&1; then
    echo "  realm qualitos : authentification par paliers en place"
  else
    echo "  ATTENTION : paliers d'authentification non posés." >&2
    echo "  L'approbation d'un control plan et l'acceptation d'une proposition de" >&2
    echo "  révision répondront 403 tant que le realm ne publiera pas acr/amr." >&2
    echo "  Reprendre à la main : KC_URL=https://$HOST/auth KC_REALM=qualitos \\" >&2
    echo "    KC_ADMIN=... KC_ADMIN_PASSWORD=... infra/keycloak/apply-step-up.sh" >&2
  fi

  AI_CID="$(kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh get clients -r qualitos \
    -q clientId=api-quality-engine-ai --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1)"
  if [ -n "$AI_CID" ]; then
    AI_SECRET="$(kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh get "clients/$AI_CID/client-secret" \
      -r qualitos --fields value --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1)"
  fi
fi

if [ -n "$AI_SECRET" ]; then
  echo "  secret du client IA : récupéré depuis Keycloak"
else
  # On NE bascule PAS en dev-claims pour compenser : ce mode laisse n'importe quel
  # appelant se déclarer porteur de n'importe quel tenant. Mieux vaut une IA
  # indisponible et un message clair qu'une authentification de façade.
  echo "  ATTENTION : secret du client IA introuvable — les fonctions d'IA resteront" >&2
  echo "  indisponibles tant que le realm n'expose pas api-quality-engine-ai." >&2
fi

kubectl -n "$NS" create secret generic qualitos-api-quality-engine-ai \
  --from-literal=AI_CLIENT_SECRET="$AI_SECRET" \
  --dry-run=client -o yaml | kubectl apply -f - >/dev/null
echo "  secret qualitos-api-quality-engine-ai : à jour"

# Identifiants du stockage objet, remis à l'engine. Ce sont ceux du compte
# RESTREINT, jamais ceux du compte racine. Secret SÉPARÉ de celui des bases : les
# deux familles n'ont ni la même origine ni le même cycle de vie, et les mélanger
# obligerait à toucher aux mots de passe de base pour faire tourner une clé de
# stockage. Dérivés du secret MinIO plutôt que ressaisis : deux sources pour le
# même identifiant finissent toujours par diverger.
S3_USER="$(kubectl -n "$NS" get secret qualitos-minio -o jsonpath='{.data.STORAGE_S3_ACCESS_KEY}' | base64 -d)"
S3_PWD="$(kubectl -n "$NS" get secret qualitos-minio -o jsonpath='{.data.STORAGE_S3_SECRET_KEY}' | base64 -d)"
kubectl -n "$NS" create secret generic qualitos-api-quality-engine-storage \
  --from-literal=STORAGE_S3_ACCESS_KEY="$S3_USER" \
  --from-literal=STORAGE_S3_SECRET_KEY="$S3_PWD" \
  --dry-run=client -o yaml | kubectl apply -f - >/dev/null
echo "  secret qualitos-api-quality-engine-storage : à jour"

kubectl -n "$NS" create secret generic qualitos-ai-service \
  --from-literal=NLQ_READONLY_DSN="postgresql://qualitos_nlq_ro:${NLQ_PWD}@postgres:5432/qualitos_quality" \
  --dry-run=client -o yaml | kubectl apply -f - >/dev/null
echo "  secret qualitos-ai-service : à jour"

# Vidage de SÛRETÉ, uniquement sur un environnement DÉJÀ installé — à la première
# installation il n'y a rien à sauver, et le CronJob vient d'être créé.
#
# POURQUOI JUSTE AVANT, alors qu'une sauvegarde nocturne existe : les migrations
# Flyway ne se défont pas. Revenir à une image antérieure la placerait devant un
# schéma qu'elle ne connaît pas, et le seul retour arrière qui tienne restaure la
# base. Or restaurer celle de la nuit précédente perdrait tout ce qui a été écrit
# depuis. Le filet doit dater du saut, pas de la veille.
#
# L'échec du vidage n'ARRÊTE PAS le déploiement : refuser de livrer parce qu'une
# sauvegarde a échoué transformerait une gêne en blocage. Il est signalé, bruyamment.
if helm status "qualitos-$ENV" -n "$NS" >/dev/null 2>&1; then
  say "5bis/6 Vidage de sûreté avant mise à jour"
  kubectl -n "$NS" delete job vidage-avant-deploiement --ignore-not-found >/dev/null
  if kubectl -n "$NS" create job --from=cronjob/postgres-backup vidage-avant-deploiement >/dev/null 2>&1 &&
     kubectl -n "$NS" wait --for=condition=complete job/vidage-avant-deploiement --timeout=600s >/dev/null 2>&1; then
    echo "  vidage pris — un retour arrière reste possible"
  else
    echo "  ATTENTION : le vidage de sûreté a échoué." >&2
    echo "  Le déploiement se poursuit, mais AUCUN retour arrière ne sera" >&2
    echo "  possible sur la base si cette version se révèle mauvaise." >&2
    kubectl -n "$NS" logs job/vidage-avant-deploiement --tail=10 >&2 2>/dev/null || true
  fi
fi

say "6/6 Chart applicatif"
helm upgrade --install "qualitos-$ENV" "$CHART" \
  --namespace "$NS" \
  --values "$VALUES" \
  --set "global.imageTag=$IMAGE_TAG" \
  --wait --timeout 10m

say "Terminé"
kubectl -n "$NS" get pods
cat <<EOF

Environnement  : $ENV
Namespace      : $NS
Version        : $IMAGE_TAG  (tag git $VERSION)
Application    : https://$HOST
Keycloak       : https://$HOST/auth

Promotion en production, une fois cette version validée :

    ./infra/k8s/deploy.sh prod $VERSION

EOF
