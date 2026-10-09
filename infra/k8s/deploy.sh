#!/usr/bin/env bash
# Déploiement complet de QualitOS sur un cluster k3s, en UNE commande.
#
#   ./infra/k8s/deploy.sh preprod v0.1.1
#   ./infra/k8s/deploy.sh prod    v0.1.1
#   ./infra/k8s/deploy.sh onprem  v0.1.1 qualitos.conf   (installation chez un client)
#
# L'édition on-premise (ADR 0082, 0083) lit TOUT ce qui est propre au site dans
# qualitos.conf (hôte, certificats, registre, stockage, annuaire, licence) : le
# reste est le même chemin que la plateforme SaaS. On l'appelle d'ordinaire par
# infra/onprem/install.sh, qui vérifie d'abord les prérequis.
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
CONF="${3:-}"

usage() {
  cat >&2 <<USAGE
usage: $0 <preprod|prod> <version>
       $0 onprem <version> <qualitos.conf>

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

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
# shellcheck source=../onprem/lib.sh
. "$ROOT/infra/onprem/lib.sh"

EDITION=saas
RELEASE="qualitos-$ENV"
case "$ENV" in
  preprod) NS=qualitos-preprod; HOST=preprod.qualitos.openlabconsulting.com ;;
  prod)    NS=qualitos;         HOST=qualitos.openlabconsulting.com ;;
  onprem)
    [ -n "$CONF" ] || usage
    qos_load_conf "$CONF"
    qos_require QOS_HOST QOS_NAMESPACE QOS_ADMIN_EMAIL QOS_LICENSE_FILE QOS_REGISTRY
    EDITION=onprem
    NS="$QOS_NAMESPACE"
    HOST="$QOS_HOST"
    RELEASE=qualitos
    # Un chemin relatif dans qualitos.conf se lit depuis le dossier du fichier.
    case "$QOS_LICENSE_FILE" in
      /*) LICENSE_FILE="$QOS_LICENSE_FILE" ;;
      *)  LICENSE_FILE="$(cd "$(dirname "$CONF")" && pwd)/$QOS_LICENSE_FILE" ;;
    esac
    [ -f "$LICENSE_FILE" ] || { echo "licence introuvable : $LICENSE_FILE" >&2; exit 1; }
    TENANT_ID="$(qos_license_tenant "$LICENSE_FILE" || true)"
    [ -n "$TENANT_ID" ] || { echo "licence illisible : aucun client (tenantId) dans $LICENSE_FILE" >&2; exit 1; }
    ;;
  *)       usage ;;
esac

# Réglages du site, avec les valeurs de la plateforme SaaS par défaut : en
# préproduction et en production, rien ne change.
: "${QOS_STORAGE_CLASS:=local-path}"
: "${QOS_MIRROR_REGISTRY:=}"
: "${QOS_OLLAMA_URL:=}"
: "${QOS_OLLAMA_MODEL:=hf.co/OpenLLM-France/Lucie-7B-Instruct-v1.1-gguf:Q4_K_M}"

# Le pipeline de release publie ses images SANS le « v » du tag git
# (${GITHUB_REF_NAME#v}). On accepte donc les deux ecritures et on normalise :
# demander « v0.1.1 » pour obtenir un ImagePullBackOff sur un tag inexistant est
# une perte de temps evitable.
IMAGE_TAG="${VERSION#v}"

# Port local du tunnel vers Keycloak (cf. la configuration des paliers, plus bas).
STEP_UP_PORT="${STEP_UP_PORT:-18080}"
DEPS="$ROOT/infra/k8s/deps"
CHART="$ROOT/infra/k8s/qualitos"
VALUES="$CHART/values-$ENV.yaml"

[ -f "$VALUES" ] || { echo "fichier de valeurs introuvable : $VALUES" >&2; exit 1; }

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }

# Un manifeste de dépendance, rendu pour le site : hôte, modèle d'IA, classe de
# stockage, et images tirées du registre miroir s'il y en a un. Sans réglage de
# site (préprod, prod), le manifeste sort tel quel.
render_dep() {
  sed -e "s|__QOS_HOST__|$HOST|g" \
      -e "s|__OLLAMA_MODEL__|$QOS_OLLAMA_MODEL|g" \
      -e "s|storageClassName: local-path|storageClassName: $QOS_STORAGE_CLASS|" "$1" \
    | qos_mirror_images "$QOS_MIRROR_REGISTRY"
}
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
  if [ "$EDITION" = onprem ]; then
    QOS_HOST="$HOST" QOS_EDITION=onprem QOS_TENANT_ID="$TENANT_ID" QOS_ADMIN_EMAIL="$QOS_ADMIN_EMAIL" \
      "$DEPS/render-realm.sh" "$NS"
  else
    QOS_HOST="$HOST" "$DEPS/render-realm.sh" "$NS"
  fi
fi

say "4/6 Dépendances d'état"
# Un registre privé (miroir d'un site isolé) : le compte de service par défaut,
# celui des dépendances, reçoit le secret d'accès.
if [ -n "${QOS_PULL_SECRET:-}" ]; then
  kubectl -n "$NS" patch serviceaccount default --type merge \
    -p "{\"imagePullSecrets\":[{\"name\":\"$QOS_PULL_SECRET\"}]}" >/dev/null
fi
for dep in 10-postgres.yaml 30-qdrant.yaml 60-backup.yaml 20-keycloak.yaml; do
  render_dep "$DEPS/$dep" | kubectl -n "$NS" apply -f -
done
# Le modèle de langage : celui de l'hôte pour la plateforme SaaS ; chez un client,
# le sien s'il en a un (QOS_OLLAMA_URL), sinon un Ollama dans le cluster.
OLLAMA_IN_CLUSTER=0
if [ "$EDITION" = saas ]; then
  render_dep "$DEPS/40-ollama-external.yaml" | kubectl -n "$NS" apply -f -
elif [ -z "$QOS_OLLAMA_URL" ]; then
  OLLAMA_IN_CLUSTER=1
  kubectl -n "$NS" delete job ollama-models --ignore-not-found >/dev/null
  render_dep "$DEPS/45-ollama.yaml" | kubectl -n "$NS" apply -f -
fi

# Le travail d'initialisation du bucket est IMMUABLE une fois créé : le
# supprimer d'abord est ce qui rend le déploiement rejouable. Sans cela, la
# deuxième exécution échouerait sur un champ non modifiable, et l'échec
# porterait sur le bucket alors que rien ne va mal.
kubectl -n "$NS" delete job minio-init --ignore-not-found >/dev/null
render_dep "$DEPS/50-minio.yaml" | kubectl -n "$NS" apply -f -

kubectl -n "$NS" rollout status deploy/postgres --timeout=180s
kubectl -n "$NS" rollout status deploy/qdrant   --timeout=180s
kubectl -n "$NS" rollout status deploy/minio    --timeout=180s
kubectl -n "$NS" rollout status deploy/keycloak --timeout=420s
if [ "$OLLAMA_IN_CLUSTER" = 1 ]; then
  kubectl -n "$NS" rollout status deploy/ollama --timeout=300s
  # Les modèles se téléchargent en arrière-plan : plusieurs gigaoctets. On ne les
  # attend pas — l'application démarre sans eux, seules les fonctions d'IA
  # répondront « indisponible » le temps du téléchargement.
  echo "  modèles d'IA : téléchargement en arrière-plan (kubectl -n $NS logs -f job/ollama-models)"
fi

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
PROV_SECRET=""
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
  WEB_CID="$(kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh get clients -r qualitos     -q clientId=qualitos-web --fields id --format csv --noquotes 2>/dev/null | tr -d '
' | head -1)"
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
  # La sortie est CONSERVÉE et rendue en cas d'échec. Une première version
  # l'envoyait à /dev/null : le déploiement annonçait « paliers non posés » sans
  # dire pourquoi, et diagnostiquer demandait de rejouer le script à la main sur
  # le serveur. Un avertissement qui ne porte que le symptôme fait perdre le
  # temps qu'il prétend faire gagner.
  STEP_UP_LOG="$(mktemp)"

  # L'API d'administration est jointe par un TUNNEL vers le service, jamais par
  # l'URL publique. Deux raisons, dont la seconde a coûté un déploiement :
  #
  #   1. Les identifiants d'administration n'ont aucune raison de sortir du
  #      cluster pour y revenir.
  #   2. L'ingress porte ModSecurity et le Core Rule Set OWASP. Le WAF laisse
  #      passer les lectures et REFUSE les écritures de l'API d'administration —
  #      un PUT avec corps JSON revient en 403 au corps vide. Le script échouait
  #      donc dès sa première écriture, tandis que `kcadm`, qui travaille depuis
  #      l'intérieur du pod, réussissait les siennes : deux chemins, deux
  #      verdicts, et un diagnostic qui accusait le jeton.
  #
  # Affaiblir le WAF sur `/auth/admin` aurait été l'inverse du bon geste : cette
  # API est publiquement joignable, c'est précisément là qu'on veut un filtre.
  # Un tunnel oublie par une execution precedente tiendrait le port et ferait
  # echouer celui-ci sans rien dire de plus qu'« adresse deja utilisee ».
  pkill -f "port-forward svc/keycloak $STEP_UP_PORT" 2>/dev/null || true
  kubectl -n "$NS" port-forward svc/keycloak "$STEP_UP_PORT:8080" >/dev/null 2>&1 &
  PF_PID=$!

  # Attente de l'ouverture du tunnel. La forme `curl … && break` est PROSCRITE
  # ici : sous `set -e`, l'echec du dernier essai fait sortir la boucle en
  # erreur, et c'est tout le deploiement qui s'arrete — ce qui est arrive, avec
  # un minuteur repartant en boucle toutes les deux minutes. Un `if` echoue sans
  # consequence, et `sleep` referme chaque tour sur un succes.
  TUNNEL_PRET=0
  for _ in $(seq 1 20); do
    if curl -sf -o /dev/null -m 2 \
         "http://127.0.0.1:$STEP_UP_PORT/auth/realms/master/.well-known/openid-configuration"; then
      TUNNEL_PRET=1
      break
    fi
    sleep 1
  done

  if [ "$TUNNEL_PRET" = 0 ]; then
    echo "  ATTENTION : tunnel vers Keycloak non ouvert sur le port $STEP_UP_PORT." >&2
  fi

  if [ "$TUNNEL_PRET" = 1 ] \
     && KC_URL="http://127.0.0.1:$STEP_UP_PORT/auth" KC_REALM=qualitos \
        KC_ADMIN="$KC_ADMIN" KC_ADMIN_PASSWORD="$KC_PWD" \
        "$ROOT/infra/keycloak/apply-step-up.sh" >"$STEP_UP_LOG" 2>&1; then
    echo "  realm qualitos : authentification par paliers en place"
  else
    echo "  ATTENTION : paliers d'authentification non posés." >&2
    echo "  L'approbation d'un control plan et l'acceptation d'une proposition de" >&2
    echo "  révision répondront 403 tant que le realm ne publiera pas acr/amr." >&2
    echo "  --- sortie du script (20 dernières lignes) ---" >&2
    tail -20 "$STEP_UP_LOG" | sed 's/^/  | /' >&2
    echo "  --- fin ---" >&2
    echo "  Reprendre à la main, en passant par un tunnel et non par l'URL" >&2
    echo "  publique — le WAF de l'ingress refuse les écritures d'administration :" >&2
    echo "    kubectl -n $NS port-forward svc/keycloak $STEP_UP_PORT:8080 &" >&2
    echo "    KC_URL=http://127.0.0.1:$STEP_UP_PORT/auth KC_REALM=qualitos \\" >&2
    echo "    KC_ADMIN=... KC_ADMIN_PASSWORD=... infra/keycloak/apply-step-up.sh" >&2
  fi
  kill "$PF_PID" 2>/dev/null || true
  wait "$PF_PID" 2>/dev/null || true
  rm -f "$STEP_UP_LOG"

  AI_CID="$(kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh get clients -r qualitos \
    -q clientId=api-quality-engine-ai --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1)"
  if [ -n "$AI_CID" ]; then
    AI_SECRET="$(kubectl -n "$NS" exec "$KC_POD" -- /opt/keycloak/bin/kcadm.sh get "clients/$AI_CID/client-secret" \
      -r qualitos --fields value --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1)"
  fi

  # Compte de service qui crée et règle les comptes de connexion (ADR 0079).
  # Le realm-export ne se réimporte que sur une base vide : sur un realm déjà en
  # service, c'est ICI qu'il naît. kcadm travaille depuis le pod — pas de WAF.
  # Droits : manage-users, view-users, query-users du realm, et rien d'autre.
  KC="kubectl -n $NS exec $KC_POD -- /opt/keycloak/bin/kcadm.sh"
  PROV_CID="$($KC get clients -r qualitos -q clientId=qualitos-provisioner --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1 || true)"
  if [ -z "$PROV_CID" ]; then
    if $KC create clients -r qualitos -s clientId=qualitos-provisioner -s enabled=true \
         -s publicClient=false -s serviceAccountsEnabled=true -s standardFlowEnabled=false \
         -s directAccessGrantsEnabled=false -s implicitFlowEnabled=false \
         -s 'attributes."access.token.lifespan"=300' >/dev/null; then
      echo "  client qualitos-provisioner : créé"
    else
      echo "  ATTENTION : création du client qualitos-provisioner refusée par Keycloak." >&2
    fi
    PROV_CID="$($KC get clients -r qualitos -q clientId=qualitos-provisioner --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1 || true)"
  fi
  if [ -n "$PROV_CID" ]; then
    # Idempotent : réattribuer un rôle déjà porté ne fait rien.
    if $KC add-roles -r qualitos --uusername service-account-qualitos-provisioner \
         --cclientid realm-management --rolename manage-users --rolename view-users \
         --rolename query-users >/dev/null; then
      echo "  compte de service qualitos-provisioner : droits de gestion des utilisateurs en place"
    else
      echo "  ATTENTION : droits du compte de service qualitos-provisioner non posés." >&2
    fi
    PROV_SECRET="$($KC get "clients/$PROV_CID/client-secret" -r qualitos --fields value --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1 || true)"
  fi

  # Annuaire d'entreprise (LDAP / Active Directory) d'une installation on-premise
  # (ADR 0083). Posé par kcadm, après démarrage : il vaut ainsi pour un realm
  # déjà en service comme pour un neuf, et le mot de passe de liaison reste dans
  # la base de Keycloak — il ne transite par aucune ConfigMap. Le JSON est
  # transmis sur l'entrée standard : le mot de passe n'apparaît sur aucune ligne
  # de commande, donc dans aucune liste de processus.
  #
  # Chaque compte de l'annuaire reçoit le `tenant_id` du client de la licence
  # (mappeur d'attribut fixe) et le rôle « user » : sans le premier, son jeton ne
  # désignerait aucun client et toute requête répondrait 400.
  if [ "$EDITION" = onprem ] && [ -n "${QOS_LDAP_URL:-}" ]; then
    REALM_ID="$($KC get realms/qualitos --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1 || true)"
    LDAP_ID="$($KC get components -r qualitos -q name=annuaire --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1 || true)"
    LDAP_JSON="$(REALM_ID="$REALM_ID" python3 "$ROOT/infra/onprem/ldap-component.py" provider)"
    if [ -z "$LDAP_ID" ]; then
      if printf '%s' "$LDAP_JSON" | kubectl -n "$NS" exec -i "$KC_POD" -- /opt/keycloak/bin/kcadm.sh \
           create components -r qualitos -f - >/dev/null; then
        echo "  annuaire $QOS_LDAP_URL : relié"
      else
        echo "  ATTENTION : l'annuaire n'a pas pu être relié (adresse, DN de liaison ou mot de passe ?)." >&2
      fi
      LDAP_ID="$($KC get components -r qualitos -q name=annuaire --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1 || true)"
    else
      printf '%s' "$LDAP_JSON" | kubectl -n "$NS" exec -i "$KC_POD" -- /opt/keycloak/bin/kcadm.sh \
        update "components/$LDAP_ID" -r qualitos -f - >/dev/null \
        && echo "  annuaire $QOS_LDAP_URL : réglages à jour" \
        || echo "  ATTENTION : réglages de l'annuaire non mis à jour." >&2
    fi
    if [ -n "$LDAP_ID" ]; then
      for mapper in tenant role; do
        NAME="qualitos-$mapper"
        if [ -z "$($KC get components -r qualitos -q parent="$LDAP_ID" -q name="$NAME" --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -1 || true)" ]; then
          LDAP_ID="$LDAP_ID" TENANT_ID="$TENANT_ID" python3 "$ROOT/infra/onprem/ldap-component.py" "$mapper" \
            | kubectl -n "$NS" exec -i "$KC_POD" -- /opt/keycloak/bin/kcadm.sh create components -r qualitos -f - >/dev/null \
            && echo "  annuaire : mappeur $NAME posé" \
            || echo "  ATTENTION : mappeur $NAME non posé." >&2
        fi
      done
    fi
  fi
fi

if [ -n "$PROV_SECRET" ]; then
  echo "  secret du compte de service des comptes : récupéré depuis Keycloak"
else
  # Pas de repli : sans ce secret, créer un client ou inviter un membre répond
  # 502 avec un message clair, et aucun compte fantôme n'est annoncé.
  echo "  ATTENTION : secret de qualitos-provisioner introuvable — la création des" >&2
  echo "  clients et l'invitation des membres resteront indisponibles." >&2
fi
kubectl -n "$NS" create secret generic qualitos-api-core-identity \
  --from-literal=KEYCLOAK_PROVISIONER_SECRET="$PROV_SECRET" \
  --dry-run=client -o yaml | kubectl apply -f - >/dev/null
echo "  secret qualitos-api-core-identity : à jour"

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

# La licence d'une installation on-premise, montée en fichier dans api-core et le
# moteur (ADR 0082). Réappliquée à chaque passage : remplacer le fichier dans
# qualitos.conf puis relancer suffit à renouveler — les services la relisent
# d'eux-mêmes en moins d'une minute, sans redémarrage.
if [ "$EDITION" = onprem ]; then
  kubectl -n "$NS" create secret generic qualitos-license \
    --from-file=license.lic="$LICENSE_FILE" \
    --dry-run=client -o yaml | kubectl apply -f - >/dev/null
  echo "  secret qualitos-license : à jour ($(qos_license_customer "$LICENSE_FILE" || echo 'client ?'))"
fi

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
if helm status "$RELEASE" -n "$NS" >/dev/null 2>&1; then
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
VALUES_ARGS=(--values "$VALUES")
if [ "$EDITION" = onprem ]; then
  # Ce qui est propre au site : hôte, certificats, registre, modèle d'IA.
  SITE_VALUES="$(mktemp)"
  trap 'rm -f "$SITE_VALUES"' EXIT
  "$ROOT/infra/onprem/site-values.sh" > "$SITE_VALUES"
  VALUES_ARGS+=(--values "$SITE_VALUES")
fi
helm upgrade --install "$RELEASE" "$CHART" \
  --namespace "$NS" \
  "${VALUES_ARGS[@]}" \
  --set "global.imageTag=$IMAGE_TAG" \
  --wait --timeout 10m

say "Terminé"
kubectl -n "$NS" get pods
if [ "$EDITION" = onprem ]; then
  cat <<EOF

Installation   : $(qos_license_customer "$LICENSE_FILE" || echo "$HOST")
Namespace      : $NS
Version        : $IMAGE_TAG
Application    : https://$HOST
Administrateur : admin ($QOS_ADMIN_EMAIL) — mot de passe provisoire :
    kubectl -n $NS get secret qualitos-realm-accounts -o jsonpath='{.data.ADMIN_PASSWORD}' | base64 -d

Renouveler la licence : remplacer le fichier indiqué dans qualitos.conf, puis
relancer la même commande. Mettre à jour : infra/onprem/install.sh avec la
nouvelle version.

EOF
  exit 0
fi

cat <<EOF

Environnement  : $ENV
Namespace      : $NS
Version        : $IMAGE_TAG  (tag git $VERSION)
Application    : https://$HOST
Keycloak       : https://$HOST/auth

Promotion en production, une fois cette version validée :

    ./infra/k8s/deploy.sh prod $VERSION

EOF
