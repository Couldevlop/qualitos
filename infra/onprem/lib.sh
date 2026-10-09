# shellcheck shell=bash
# Fonctions partagées des installations on-premise (ADR 0083) : lecture de la
# configuration du site, images vers un registre miroir, lecture de la licence.
# Sourcé par infra/k8s/deploy.sh et infra/onprem/install.sh — jamais exécuté seul.

# Charge qualitos.conf. Le fichier est une suite d'affectations VAR=valeur : on
# le lit ligne à ligne plutôt que de le `source`, pour qu'aucune commande glissée
# dedans ne s'exécute avec les droits de l'installateur.
qos_load_conf() {
  local conf="$1" line key value
  [ -f "$conf" ] || { echo "configuration introuvable : $conf" >&2; return 1; }
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    case "$line" in ''|'#'*) continue ;; esac
    if [[ "$line" =~ ^[[:space:]]*(QOS_[A-Z0-9_]+)=(.*)$ ]]; then
      key="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      # Guillemets facultatifs autour de la valeur.
      value="${value%\"}"; value="${value#\"}"
      value="${value%\'}"; value="${value#\'}"
      # Une variable déjà posée dans l'environnement l'emporte : c'est ainsi qu'on
      # passe un mot de passe sans l'écrire dans le fichier.
      if [ -z "${!key:-}" ]; then
        printf -v "$key" '%s' "$value"
        export "${key?}"
      fi
    else
      echo "ligne ignorée dans $conf (attendu QOS_NOM=valeur) : $line" >&2
    fi
  done < "$conf"
}

# Vérifie qu'une variable de configuration est renseignée.
qos_require() {
  local var
  for var in "$@"; do
    [ -n "${!var:-}" ] || { echo "configuration incomplète : $var est requis" >&2; return 1; }
  done
}

# Référence d'une image tierce dans le registre miroir du site.
#   postgres:17-alpine               -> <miroir>/library/postgres:17-alpine
#   quay.io/keycloak/keycloak:25.0   -> <miroir>/keycloak/keycloak:25.0
#   minio/minio:RELEASE.2025-…       -> <miroir>/minio/minio:RELEASE.2025-…
# La MÊME règle sert à remplir le miroir (paquet hors ligne) et à le lire (ici) :
# deux règles finiraient par diverger, et une image introuvable bloque un site isolé.
qos_mirror_ref() {
  local ref="$1" mirror="$2" first rest
  [ -n "$mirror" ] || { printf '%s' "$ref"; return; }
  first="${ref%%/*}"
  if [ "$first" != "$ref" ] && { [[ "$first" == *.* ]] || [[ "$first" == *:* ]] || [ "$first" = localhost ]; }; then
    rest="${ref#*/}"
  else
    rest="$ref"
  fi
  [[ "$rest" == */* ]] || rest="library/$rest"
  printf '%s/%s' "${mirror%/}" "$rest"
}

# Réécrit les lignes `image:` d'un manifeste vers le registre miroir (stdin -> stdout).
qos_mirror_images() {
  local mirror="$1" line indent ref
  while IFS= read -r line || [ -n "$line" ]; do
    if [ -n "$mirror" ] && [[ "$line" =~ ^([[:space:]]*-?[[:space:]]*image:[[:space:]]*)([^[:space:]#]+)(.*)$ ]]; then
      indent="${BASH_REMATCH[1]}"
      ref="${BASH_REMATCH[2]}"
      printf '%s%s%s\n' "$indent" "$(qos_mirror_ref "$ref" "$mirror")" "${BASH_REMATCH[3]}"
    else
      printf '%s\n' "$line"
    fi
  done
}

# Le client (tenant) que fixe une licence, lu dans son contenu signé. On ne
# VÉRIFIE pas la signature ici — l'application le fait, avec les clés épinglées ;
# l'installateur n'a besoin que de l'identifiant pour rattacher l'administrateur.
qos_license_tenant() {
  local file="$1" payload
  payload="$(tr -d '\n\r' < "$file" | grep -o '"payload"[[:space:]]*:[[:space:]]*"[^"]*"' \
    | sed 's/.*"\([^"]*\)"$/\1/' | tr '_-' '/+')"
  [ -n "$payload" ] || return 1
  while [ $(( ${#payload} % 4 )) -ne 0 ]; do payload="${payload}="; done
  printf '%s' "$payload" | base64 -d 2>/dev/null \
    | grep -o '"tenantId"[[:space:]]*:[[:space:]]*"[0-9a-fA-F-]\{36\}"' | grep -o '[0-9a-fA-F-]\{36\}'
}

# Le client (raison sociale) d'une licence, pour l'afficher à l'installation.
qos_license_customer() {
  local file="$1" payload
  payload="$(tr -d '\n\r' < "$file" | grep -o '"payload"[[:space:]]*:[[:space:]]*"[^"]*"' \
    | sed 's/.*"\([^"]*\)"$/\1/' | tr '_-' '/+')"
  [ -n "$payload" ] || return 1
  while [ $(( ${#payload} % 4 )) -ne 0 ]; do payload="${payload}="; done
  printf '%s' "$payload" | base64 -d 2>/dev/null | grep -o '"customer"[[:space:]]*:[[:space:]]*"[^"]*"' \
    | sed 's/.*"\([^"]*\)"$/\1/'
}
