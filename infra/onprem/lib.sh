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

# ---------------------------------------------------------------------------
# Paquet hors ligne (ADR 0085). Un dossier :
#   images.txt       une ligne par image : « app <ref> » ou « tiers <ref> »
#   images/*.tar.gz  les images (docker save | gzip), dans l'ordre d'images.txt
#   models/          facultatif : ollama-models.tar.gz, les modèles d'IA
#   SHA256SUMS       l'empreinte de CHAQUE fichier du paquet
#   SHA256SUMS.sig   signature Ed25519 de SHA256SUMS par l'éditeur (base64)
# Rien n'est chargé avant que la signature ET toutes les empreintes soient
# vérifiées : un paquet altéré en route ne doit rien poser chez le client.

# Vérifie un paquet contre la clé publique épinglée. Code de retour non nul et
# message sur stderr au premier défaut.
qos_bundle_verify() {
  local dir="$1" pub="$2" raw line path
  [ -f "$dir/SHA256SUMS" ] && [ -f "$dir/SHA256SUMS.sig" ] \
    || { echo "paquet incomplet : SHA256SUMS ou sa signature manque ($dir)" >&2; return 1; }
  raw="$(mktemp)"
  if ! openssl base64 -d -A -in "$dir/SHA256SUMS.sig" -out "$raw" 2>/dev/null \
     || ! openssl pkeyutl -verify -pubin -inkey "$pub" -rawin -in "$dir/SHA256SUMS" -sigfile "$raw" >/dev/null 2>&1; then
    rm -f "$raw"
    echo "signature du paquet INVALIDE : il ne vient pas de l'éditeur, ou a été modifié" >&2
    return 1
  fi
  rm -f "$raw"
  # Des chemins relatifs, sans remontée : une liste signée ne doit pas pouvoir
  # désigner un fichier hors du paquet.
  while IFS= read -r line || [ -n "$line" ]; do
    path="${line#*  }"
    case "$path" in /*|*..*|'') echo "chemin refusé dans SHA256SUMS : $path" >&2; return 1 ;; esac
  done < "$dir/SHA256SUMS"
  (cd "$dir" && sha256sum --quiet --strict -c SHA256SUMS) >/dev/null 2>&1 \
    || { echo "empreinte invalide : un fichier du paquet est altéré ou manque" >&2; return 1; }
  # Tout fichier du paquet doit être signé : un fichier ajouté après coup
  # (modèles, image) serait sinon chargé sans que rien ne le couvre.
  local extra
  extra="$(cd "$dir" && find . -type f ! -name 'SHA256SUMS' ! -name 'SHA256SUMS.sig' | sed 's#^\./##' | LC_ALL=C sort \
    | LC_ALL=C comm -23 - <(sed 's/^[0-9a-f]*  //' SHA256SUMS | LC_ALL=C sort))"
  [ -z "$extra" ] || { printf 'fichier non couvert par la signature : %s\n' "$extra" >&2; return 1; }
  grep -q '  images.txt$' "$dir/SHA256SUMS" && grep -q '  VERSION$' "$dir/SHA256SUMS" \
    || { echo "images.txt ou VERSION n'est pas couvert par la signature" >&2; return 1; }
}

# Référence qu'une image du paquet doit porter chez le client : le registre du
# site pour les images de QualitOS, le miroir (même règle qu'au déploiement)
# pour les images tierces.
qos_bundle_target() {
  local kind="$1" ref="$2" name
  if [ "$kind" = app ]; then
    name="${ref##*/}"
    printf '%s/%s' "${QOS_REGISTRY%/}" "$name"
  else
    qos_mirror_ref "$ref" "${QOS_MIRROR_REGISTRY:-}"
  fi
}

# Charge les images d'un paquet VÉRIFIÉ dans le Docker local et les nomme comme
# le site les attend ; avec « push », les pousse aussi vers le registre du site.
qos_bundle_load() {
  local dir="$1" mode="${2:-}" kind ref file target i=0
  while read -r kind ref; do
    [ -n "$kind" ] || continue
    i=$((i + 1))
    file="$(printf '%s/images/%03d.tar.gz' "$dir" "$i")"
    [ -f "$file" ] || { echo "image manquante dans le paquet : $ref" >&2; return 1; }
    gzip -dc "$file" | docker load >/dev/null || { echo "chargement refusé : $ref" >&2; return 1; }
    target="$(qos_bundle_target "$kind" "$ref")"
    [ "$target" = "$ref" ] || docker tag "$ref" "$target"
    if [ "$mode" = push ]; then docker push --quiet "$target" >/dev/null || { echo "envoi refusé : $target" >&2; return 1; }; fi
    printf '  %s\n' "$target"
  done < "$dir/images.txt"
}
