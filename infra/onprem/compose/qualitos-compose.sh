#!/usr/bin/env bash
# QualitOS — mode léger : un serveur seul, sans Kubernetes (ADR 0084).
#
#   ./infra/onprem/compose/qualitos-compose.sh verifier qualitos.conf
#   ./infra/onprem/compose/qualitos-compose.sh installer qualitos.conf <version>
#   ./infra/onprem/compose/qualitos-compose.sh licence  qualitos.conf
#   ./infra/onprem/compose/qualitos-compose.sh sauvegarder qualitos.conf
#   ./infra/onprem/compose/qualitos-compose.sh etat     qualitos.conf
#
# Même qualitos.conf que l'installation Kubernetes : seuls QOS_TLS_CERT et
# QOS_TLS_KEY (le certificat en fichiers) et QOS_COMPOSE_DIR (où vit
# l'installation, /opt/qualitos par défaut) lui sont propres.
#
# `installer` est rejouable : il installe la première fois, met à jour ensuite
# (avec un vidage de sûreté de la base juste avant), et ne régénère jamais un
# secret déjà créé — PostgreSQL, Keycloak et MinIO gardent les leurs dans leurs
# volumes, et les changer les rendrait injoignables.

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ONPREM="$(cd "$HERE/.." && pwd)"
ROOT="$(cd "$ONPREM/../.." && pwd)"
DEPS="$ROOT/infra/k8s/deps"
# shellcheck source=../lib.sh
. "$ONPREM/lib.sh"

say()  { printf '\n\033[1m== %s\033[0m\n' "$*"; }
ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$*" >&2; WARNINGS=$((WARNINGS + 1)); }
ko()   { printf '  \033[31m✗\033[0m %s\n' "$*" >&2; ERRORS=$((ERRORS + 1)); }
ERRORS=0
WARNINGS=0

usage() { sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//' >&2; exit 2; }

CMD="${1:-}"; CONF="${2:-}"; VERSION="${3:-}"
[ -n "$CMD" ] && [ -n "$CONF" ] || usage
case "$CMD" in
  installer) [ -n "$VERSION" ] || usage ;;
  verifier|licence|sauvegarder|etat) ;;
  *) usage ;;
esac

qos_load_conf "$CONF"
CONF_DIR="$(cd "$(dirname "$CONF")" && pwd)"
abs() { case "$1" in /*) printf '%s' "$1" ;; '') printf '' ;; *) printf '%s/%s' "$CONF_DIR" "$1" ;; esac; }

: "${QOS_COMPOSE_DIR:=/opt/qualitos}"
: "${QOS_MIRROR_REGISTRY:=}"
: "${QOS_OLLAMA_URL:=}"
: "${QOS_OLLAMA_MODEL:=hf.co/OpenLLM-France/Lucie-7B-Instruct-v1.1-gguf:Q4_K_M}"
: "${QOS_KEYCLOAK_ADMIN_PORT:=18080}"
LICENSE_FILE="$(abs "${QOS_LICENSE_FILE:-}")"
TLS_CERT="$(abs "${QOS_TLS_CERT:-}")"
TLS_KEY="$(abs "${QOS_TLS_KEY:-}")"
DIR="$QOS_COMPOSE_DIR"

dc() { docker compose --project-directory "$DIR" -f "$DIR/docker-compose.yml" --env-file "$DIR/.env" "$@"; }

# Les images tierces, depuis le miroir du site s'il y en a un — même règle que
# l'installation Kubernetes (qos_mirror_ref). Versions : celles des manifestes.
image_of() { grep -m1 -oE "image: *[^ ]*$1[^ ]*" "$2" | sed 's/image: *//'; }
IMG_POSTGRES="$(qos_mirror_ref "$(image_of postgres "$DEPS/10-postgres.yaml")" "$QOS_MIRROR_REGISTRY")"
IMG_KEYCLOAK="$(qos_mirror_ref "$(image_of keycloak "$DEPS/20-keycloak.yaml")" "$QOS_MIRROR_REGISTRY")"
IMG_QDRANT="$(qos_mirror_ref "$(image_of qdrant/qdrant "$DEPS/30-qdrant.yaml")" "$QOS_MIRROR_REGISTRY")"
IMG_OLLAMA="$(qos_mirror_ref "$(image_of ollama/ollama "$DEPS/45-ollama.yaml")" "$QOS_MIRROR_REGISTRY")"
IMG_MINIO="$(qos_mirror_ref "$(image_of minio/minio "$DEPS/50-minio.yaml")" "$QOS_MIRROR_REGISTRY")"
IMG_MC="$(qos_mirror_ref "$(image_of minio/mc "$DEPS/50-minio.yaml")" "$QOS_MIRROR_REGISTRY")"
IMG_NGINX="$(qos_mirror_ref "nginx:1.27-alpine" "$QOS_MIRROR_REGISTRY")"

# ============================================================== vérifier ====
verifier() {
  echo
  echo "Vérification des prérequis — ${QOS_HOST:-?} (mode léger)"
  local v
  for v in QOS_HOST QOS_ADMIN_EMAIL QOS_LICENSE_FILE QOS_REGISTRY QOS_TLS_CERT QOS_TLS_KEY; do
    if [ -n "${!v:-}" ]; then ok "$v renseigné"; else ko "$v manquant dans $CONF"; fi
  done
  [[ "${QOS_ADMIN_EMAIL:-}" =~ ^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$ ]] \
    || ko "QOS_ADMIN_EMAIL n'est pas une adresse e-mail : ${QOS_ADMIN_EMAIL:-}"

  local tool
  for tool in docker python3 curl openssl; do
    if command -v "$tool" >/dev/null 2>&1; then ok "outil $tool"; else ko "outil $tool introuvable"; fi
  done
  if docker compose version >/dev/null 2>&1; then ok "docker compose $(docker compose version --short 2>/dev/null)"
  else ko "docker compose (v2) introuvable"; fi
  if docker info >/dev/null 2>&1; then ok "démon Docker joignable"
  else ko "démon Docker injoignable (droits ? sudo ?)"; fi

  if [ -n "$LICENSE_FILE" ] && [ -f "$LICENSE_FILE" ] && qos_license_tenant "$LICENSE_FILE" >/dev/null; then
    ok "licence de « $(qos_license_customer "$LICENSE_FILE") »"
  else
    ko "licence introuvable ou illisible : ${LICENSE_FILE:-(non renseignée)}"
  fi

  if [ -f "$TLS_CERT" ] && [ -f "$TLS_KEY" ]; then
    if openssl x509 -in "$TLS_CERT" -noout -checkend 0 >/dev/null 2>&1; then ok "certificat en cours de validité"
    else ko "certificat expiré ou illisible : $TLS_CERT"; fi
    if openssl x509 -in "$TLS_CERT" -noout -checkhost "${QOS_HOST:-}" 2>/dev/null | grep -q "does match"; then
      ok "certificat valable pour ${QOS_HOST:-}"
    else
      ko "le certificat ne couvre pas ${QOS_HOST:-}"
    fi
    if [ "$(openssl x509 -in "$TLS_CERT" -noout -pubkey 2>/dev/null | openssl sha256)" \
         = "$(openssl pkey -in "$TLS_KEY" -pubout 2>/dev/null | openssl sha256)" ]; then
      ok "clé privée assortie au certificat"
    else
      ko "la clé privée ne correspond pas au certificat"
    fi
  else
    ko "certificat ou clé introuvable (QOS_TLS_CERT, QOS_TLS_KEY)"
  fi

  local port
  for port in 80 443; do
    if command -v ss >/dev/null 2>&1 && ss -ltn 2>/dev/null | awk '{print $4}' | grep -qE "[:.]$port\$"; then
      if docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^qualitos-proxy'; then ok "port $port tenu par QualitOS"
      else ko "port $port déjà occupé sur ce serveur"; fi
    else
      ok "port $port libre"
    fi
  done

  local mem_go
  mem_go="$(awk '/MemTotal/ {printf "%d", $2/1024/1024}' /proc/meminfo 2>/dev/null || echo 0)"
  if [ "${mem_go:-0}" -gt 0 ]; then
    if [ -z "$QOS_OLLAMA_URL" ] && [ "$mem_go" -lt 16 ]; then
      warn "mémoire ${mem_go} Go : 16 Go conseillés avec le modèle d'IA sur ce serveur"
    else ok "mémoire ${mem_go} Go"; fi
  fi

  if [ -n "${QOS_LDAP_URL:-}" ]; then
    for v in QOS_LDAP_USERS_DN QOS_LDAP_BIND_DN; do
      [ -n "${!v:-}" ] && ok "$v renseigné" || ko "$v requis quand QOS_LDAP_URL est renseigné"
    done
    case "$QOS_LDAP_URL" in ldaps://*) ok "annuaire en LDAPS" ;; ldap://*) warn "annuaire en LDAP non chiffré" ;;
      *) ko "QOS_LDAP_URL doit commencer par ldap:// ou ldaps://" ;; esac
  fi

  echo
  if [ "$ERRORS" -gt 0 ]; then
    echo "$ERRORS prérequis manquant(s) : rien n'a été modifié. Corrigez puis relancez." >&2
    return 1
  fi
  echo "Prérequis réunis ($WARNINGS avertissement(s))."
}

# =============================================================== secrets ====
gen() { openssl rand -base64 24 | tr -d '/+=' | cut -c1-24; }

# Une ligne CLE=valeur dans .env : posée si absente, jamais remplacée (secrets),
# ou toujours remplacée (réglages qui suivent qualitos.conf).
env_keep() { grep -q "^$1=" "$DIR/.env" 2>/dev/null || printf '%s=%s\n' "$1" "$2" >> "$DIR/.env"; }
env_set() {
  local tmp; tmp="$(mktemp)"
  grep -v "^$1=" "$DIR/.env" > "$tmp" 2>/dev/null || true
  printf '%s=%s\n' "$1" "$2" >> "$tmp"
  cat "$tmp" > "$DIR/.env"; rm -f "$tmp"
}
env_get() { grep -m1 "^$1=" "$DIR/.env" | cut -d= -f2-; }

# ========================================================= installer ====
installer() {
  verifier || exit 1
  local tenant first_install=1
  tenant="$(qos_license_tenant "$LICENSE_FILE")"

  say "1/7 Dossier de l'installation — $DIR"
  mkdir -p "$DIR/realm" "$DIR/license" "$DIR/tls"
  chmod 700 "$DIR"
  [ -f "$DIR/.env" ] && first_install=0
  touch "$DIR/.env"; chmod 600 "$DIR/.env"

  # Secrets : générés une fois, jamais régénérés.
  env_keep POSTGRES_PASSWORD "$(gen)"
  env_keep NLQ_RO_PASSWORD "$(gen)"
  env_keep KEYCLOAK_ADMIN_PASSWORD "$(gen)"
  env_keep MINIO_ROOT_PASSWORD "$(gen)"
  env_keep STORAGE_S3_SECRET_KEY "$(gen)"
  env_keep QOS_ADMIN_INITIAL_PASSWORD "$(gen)"
  env_keep AI_CLIENT_SECRET ""
  env_keep KEYCLOAK_PROVISIONER_SECRET ""
  # Réglages : suivent qualitos.conf à chaque passage.
  env_set QOS_HOST "$QOS_HOST"
  env_set QOS_REGISTRY "$QOS_REGISTRY"
  env_set QOS_VERSION "${VERSION#v}"
  env_set QOS_KEYCLOAK_ADMIN_PORT "$QOS_KEYCLOAK_ADMIN_PORT"
  env_set QOS_BACKUP_DIR "${QOS_BACKUP_DIR:-./sauvegardes}"
  env_set QOS_IMG_POSTGRES "$IMG_POSTGRES"
  env_set QOS_IMG_KEYCLOAK "$IMG_KEYCLOAK"
  env_set QOS_IMG_QDRANT "$IMG_QDRANT"
  env_set QOS_IMG_OLLAMA "$IMG_OLLAMA"
  env_set QOS_IMG_MINIO "$IMG_MINIO"
  env_set QOS_IMG_MC "$IMG_MC"
  env_set QOS_IMG_NGINX "$IMG_NGINX"
  local ollama_url="${QOS_OLLAMA_URL:-http://ollama:11434}"
  env_set QOS_OLLAMA_BASE_URL "$ollama_url"
  env_set QOS_OLLAMA_MODEL "$QOS_OLLAMA_MODEL"
  env_set QOS_OLLAMA_HOSTNAME "$(printf '%s' "$ollama_url" | sed -E 's#^[a-z]+://##; s#[:/].*$##')"
  if [ -z "$QOS_OLLAMA_URL" ]; then env_set COMPOSE_PROFILES ollama; else env_set COMPOSE_PROFILES ""; fi
  ok "secrets et réglages (.env, lisible par root seulement)"

  say "2/7 Fichiers de l'installation"
  cp "$HERE/docker-compose.yml" "$DIR/docker-compose.yml"
  sed "s|__QOS_HOST__|$QOS_HOST|g" "$HERE/proxy.conf.template" > "$DIR/proxy.conf"
  # Scripts repris des manifestes Kubernetes : une seule source pour les deux modes.
  python3 "$HERE/extract-script.py" "$DEPS/10-postgres.yaml" '^kind: ConfigMap' 'init\.sh: \|' > "$DIR/postgres-init.sh"
  python3 "$HERE/extract-script.py" "$DEPS/50-minio.yaml" '^kind: Job' '^\s+- \|' > "$DIR/minio-init.sh"
  # shellcheck disable=SC2016  # les $ s'évaluent dans le conteneur de sauvegarde
  {
    printf '#!/bin/sh\n# Vidage des bases : le script du CronJob Kubernetes, chaque jour (ADR 0084).\n'
    printf '# « une-fois » : un seul vidage (avant une mise à jour).\n'
    printf 'export PGHOST=postgres POSTGRES_USER=qualitos POSTGRES_PASSWORD="$PGPASSWORD"\n'
    printf 'vider() {\n'
    python3 "$HERE/extract-script.py" "$DEPS/60-backup.yaml" '^kind: CronJob' '^\s+- \|' | sed 's/^/  /'
    printf '}\n'
    printf 'if [ "${1:-}" = une-fois ]; then (vider); exit $?; fi\n'
    printf 'while true; do (vider) || echo "ECHEC du vidage du $(date -u +%%F)"; sleep 86400; done\n'
  } > "$DIR/backup.sh"
  install -m 0644 "$TLS_CERT" "$DIR/tls/tls.crt"
  install -m 0600 "$TLS_KEY" "$DIR/tls/tls.key"
  install_license
  if [ ! -f "$DIR/realm/qualitos-realm.json" ]; then
    # Rendu UNE fois : Keycloak n'importe le realm qu'au premier démarrage.
    python3 "$ROOT/infra/keycloak/render_realm.py" "$ROOT/infra/keycloak/realm-export.json" \
      "$DIR/realm/qualitos-realm.json" "$QOS_HOST" "" "$(env_get QOS_ADMIN_INITIAL_PASSWORD)" \
      onprem "$tenant" "$QOS_ADMIN_EMAIL"
    # Lisible par l'utilisateur non privilégié du conteneur Keycloak ; sur le
    # serveur, c'est le dossier de l'installation (700) qui le protège.
    chmod 644 "$DIR/realm/qualitos-realm.json"
  fi
  ok "compose, proxy, scripts d'initialisation, certificat, licence, realm"

  if [ "$first_install" = 0 ] && dc ps --status running --services 2>/dev/null | grep -q '^postgres$'; then
    say "2bis/7 Vidage de sûreté avant mise à jour"
    if dc exec -T backup sh /backup.sh une-fois; then ok "vidage pris — un retour arrière reste possible"
    else warn "le vidage de sûreté a échoué : aucun retour arrière possible sur la base"; fi
  fi

  say "3/7 Images"
  if [ "${QOS_COMPOSE_PULL:-true}" = true ]; then dc pull --quiet; ok "images à jour"
  else ok "images déjà chargées (paquet hors ligne)"; fi

  say "4/7 Données et identité"
  dc up -d postgres keycloak minio qdrant
  wait_healthy keycloak 600
  dc up minio-init --exit-code-from minio-init >/dev/null && ok "stockage des pièces jointes prêt" \
    || warn "le stockage des pièces jointes n'a pas pu être préparé (dc logs minio-init)"

  say "5/7 Keycloak"
  configurer_keycloak "$tenant"

  say "6/7 Application"
  dc up -d --remove-orphans
  if [ -z "$QOS_OLLAMA_URL" ]; then
    dc exec -d ollama ollama pull "$QOS_OLLAMA_MODEL" >/dev/null 2>&1 || true
    dc exec -d ollama ollama pull bge-m3 >/dev/null 2>&1 || true
    ok "modèles d'IA : téléchargement en arrière-plan (dc logs -f ollama)"
  fi

  say "7/7 Terminé"
  dc ps
  cat <<EOF

Installation   : $(qos_license_customer "$LICENSE_FILE")
Application    : https://$QOS_HOST
Version        : ${VERSION#v}
Administrateur : admin ($QOS_ADMIN_EMAIL)
Mot de passe   : PROVISOIRE, à changer à la première connexion (second facteur exigé) :
    grep QOS_ADMIN_INITIAL_PASSWORD $DIR/.env

Vidages quotidiens : ${QOS_BACKUP_DIR:-$DIR/sauvegardes} — à recopier hors de ce serveur.
EOF
}

wait_healthy() {
  local svc="$1" timeout="$2" waited=0 cid status
  cid="$(dc ps -q "$svc")"
  while [ "$waited" -lt "$timeout" ]; do
    status="$(docker inspect --format '{{.State.Health.Status}}' "$cid" 2>/dev/null || echo inconnu)"
    if [ "$status" = healthy ]; then ok "$svc prêt"; return 0; fi
    sleep 5; waited=$((waited + 5))
  done
  echo "  $svc n'est pas prêt après ${timeout}s (dc logs $svc)" >&2
  exit 1
}

install_license() {
  # Remplacement atomique dans le DOSSIER monté : les services voient le nouveau
  # fichier sans redémarrage, jamais un fichier à moitié écrit.
  install -m 0644 "$LICENSE_FILE" "$DIR/license/.license.lic.tmp"
  mv -f "$DIR/license/.license.lic.tmp" "$DIR/license/license.lic"
}

# ------------------------------------------------- Keycloak, après démarrage --
# Les mêmes réglages que deploy.sh (Kubernetes) : anti-force-brute du realm
# master, politique de mot de passe, déconnexion, paliers d'authentification,
# compte de service des comptes, secrets des clients techniques, annuaire.
configurer_keycloak() {
  local tenant="$1"
  kc() { dc exec -T keycloak /opt/keycloak/bin/kcadm.sh "$@"; }
  first() { tr -d '\r' | head -1; }

  kc config credentials --server http://localhost:8080/auth --realm master \
    --user admin --password "$(env_get KEYCLOAK_ADMIN_PASSWORD)" >/dev/null
  kc update realms/master -s bruteForceProtected=true -s failureFactor=10 \
    -s waitIncrementSeconds=60 -s maxFailureWaitSeconds=900 >/dev/null && ok "realm master : anti-force-brute"
  kc update realms/qualitos \
    -s 'passwordPolicy=length(12) and upperCase(1) and lowerCase(1) and digits(1) and notUsername(undefined)' >/dev/null \
    && ok "realm qualitos : politique de mot de passe"

  local web_cid ai_cid prov_cid realm_id ldap_id
  web_cid="$(kc get clients -r qualitos -q clientId=qualitos-web --fields id --format csv --noquotes | first)"
  [ -n "$web_cid" ] && kc update "clients/$web_cid" -r qualitos \
    -s "attributes.\"post.logout.redirect.uris\"=https://$QOS_HOST/*" >/dev/null && ok "déconnexion autorisée"

  if KC_URL="http://127.0.0.1:$QOS_KEYCLOAK_ADMIN_PORT/auth" KC_REALM=qualitos KC_ADMIN=admin \
       KC_ADMIN_PASSWORD="$(env_get KEYCLOAK_ADMIN_PASSWORD)" "$ROOT/infra/keycloak/apply-step-up.sh" >/dev/null 2>&1; then
    ok "authentification par paliers"
  else
    warn "paliers d'authentification non posés : l'approbation d'un control plan répondra 403"
  fi

  ai_cid="$(kc get clients -r qualitos -q clientId=api-quality-engine-ai --fields id --format csv --noquotes | first)"
  [ -n "$ai_cid" ] && env_set AI_CLIENT_SECRET \
    "$(kc get "clients/$ai_cid/client-secret" -r qualitos --fields value --format csv --noquotes | first)"

  # Compte de service qui crée les comptes des membres invités : né ici s'il
  # manque, avec les seuls droits de gestion des utilisateurs du realm.
  prov_cid="$(kc get clients -r qualitos -q clientId=qualitos-provisioner --fields id --format csv --noquotes | first)"
  if [ -z "$prov_cid" ]; then
    kc create clients -r qualitos -s clientId=qualitos-provisioner -s enabled=true \
      -s publicClient=false -s serviceAccountsEnabled=true -s standardFlowEnabled=false \
      -s directAccessGrantsEnabled=false -s implicitFlowEnabled=false \
      -s 'attributes."access.token.lifespan"=300' >/dev/null && ok "client qualitos-provisioner créé"
    prov_cid="$(kc get clients -r qualitos -q clientId=qualitos-provisioner --fields id --format csv --noquotes | first)"
  fi
  if [ -n "$prov_cid" ]; then
    kc add-roles -r qualitos --uusername service-account-qualitos-provisioner --cclientid realm-management \
      --rolename manage-users --rolename view-users --rolename query-users >/dev/null \
      && ok "compte de service des comptes : droits en place"
    env_set KEYCLOAK_PROVISIONER_SECRET \
      "$(kc get "clients/$prov_cid/client-secret" -r qualitos --fields value --format csv --noquotes | first)"
  fi
  [ -n "$(env_get AI_CLIENT_SECRET)" ] && ok "secret du client IA récupéré" || warn "secret du client IA introuvable"
  [ -n "$(env_get KEYCLOAK_PROVISIONER_SECRET)" ] && ok "secret du compte de service récupéré" \
    || warn "secret du compte de service introuvable : l'invitation des membres sera indisponible"

  if [ -n "${QOS_LDAP_URL:-}" ]; then
    realm_id="$(kc get realms/qualitos --fields id --format csv --noquotes | first)"
    ldap_id="$(kc get components -r qualitos -q name=annuaire --fields id --format csv --noquotes | first)"
    if [ -z "$ldap_id" ]; then
      REALM_ID="$realm_id" python3 "$ONPREM/ldap-component.py" provider \
        | kc create components -r qualitos -f - >/dev/null && ok "annuaire relié" \
        || warn "l'annuaire n'a pas pu être relié"
      ldap_id="$(kc get components -r qualitos -q name=annuaire --fields id --format csv --noquotes | first)"
    else
      REALM_ID="$realm_id" python3 "$ONPREM/ldap-component.py" provider \
        | kc update "components/$ldap_id" -r qualitos -f - >/dev/null && ok "annuaire : réglages à jour"
    fi
    local mapper
    for mapper in tenant role; do
      if [ -n "$ldap_id" ] && [ -z "$(kc get components -r qualitos -q parent="$ldap_id" -q name="qualitos-$mapper" --fields id --format csv --noquotes | first)" ]; then
        LDAP_ID="$ldap_id" TENANT_ID="$tenant" python3 "$ONPREM/ldap-component.py" "$mapper" \
          | kc create components -r qualitos -f - >/dev/null && ok "annuaire : mappeur qualitos-$mapper"
      fi
    done
  fi
}

# ================================================================ main ====
case "$CMD" in
  verifier) verifier ;;
  installer) installer ;;
  licence)
    [ -f "$LICENSE_FILE" ] && qos_license_tenant "$LICENSE_FILE" >/dev/null \
      || { echo "licence introuvable ou illisible : $LICENSE_FILE" >&2; exit 1; }
    install_license
    echo "Licence de « $(qos_license_customer "$LICENSE_FILE") » installée."
    echo "Prise en compte en moins d'une minute, sans redémarrage (Administration › Licence)."
    ;;
  sauvegarder) dc exec -T backup sh /backup.sh une-fois ;;
  etat) dc ps ;;
esac
