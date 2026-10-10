#!/usr/bin/env bash
# Construit le paquet hors ligne d'une version (ADR 0085) — chez l'éditeur, avec
# Internet. Le site l'installe ensuite sans aucun accès extérieur.
#
#   QOS_BUNDLE_KEY=/chemin/paquet-signature.key \
#     ./infra/onprem/bundle/build-bundle.sh <version> <dossier de sortie> [--modeles]
#
# Contenu : les images de QualitOS et toutes les images tierces des manifestes,
# les scripts d'installation de cette version, et avec --modeles les modèles
# d'IA (plusieurs gigaoctets). Le tout est listé dans SHA256SUMS, signé Ed25519.
# La clé privée ne quitte jamais le poste de l'éditeur ; la clé publique est
# épinglée dans le dépôt (editeur-paquet.pub) et vérifiée avant de signer.

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../.." && pwd)"
# shellcheck source=../lib.sh
. "$HERE/../lib.sh"

VERSION="${1:-}"; OUT="${2:-}"; MODELS="${3:-}"
[ -n "$VERSION" ] && [ -n "$OUT" ] || { sed -n '2,7p' "$0" | sed 's/^# \{0,1\}//' >&2; exit 2; }
VERSION="${VERSION#v}"
: "${QOS_BUNDLE_KEY:?QOS_BUNDLE_KEY : chemin de la clé privée de signature des paquets}"
: "${QOS_SOURCE_REGISTRY:=ghcr.io/couldevlop/qualitos}"
: "${QOS_OLLAMA_MODEL:=hf.co/OpenLLM-France/Lucie-7B-Instruct-v1.1-gguf:Q4_K_M}"
APPS="api-core api-quality-engine api-iot-hub ai-service web"

# Signer avec une autre clé que celle épinglée produirait un paquet que tous les
# sites refuseraient : on le voit ici, pas chez le client.
[ "$(openssl pkey -in "$QOS_BUNDLE_KEY" -pubout 2>/dev/null)" = "$(cat "$HERE/editeur-paquet.pub")" ] \
  || { echo "la clé de signature ne correspond pas à editeur-paquet.pub" >&2; exit 1; }
[ ! -e "$OUT" ] || { echo "$OUT existe déjà : choisir un dossier neuf" >&2; exit 1; }
mkdir -p "$OUT/images"

echo "== images"
{
  for app in $APPS; do echo "app $QOS_SOURCE_REGISTRY/$app:$VERSION"; done
  # Les images tierces : celles des manifestes et du mode léger, une fois chacune.
  { grep -hoE '^[[:space:]]*-?[[:space:]]*image:[[:space:]]*[^[:space:]#]+' "$ROOT"/infra/k8s/deps/*.yaml \
      | sed -E 's/.*image:[[:space:]]*//'
    echo "nginx:1.27-alpine"; } | sort -u | sed 's/^/tiers /'
} > "$OUT/images.txt"
i=0
while read -r _ ref; do
  i=$((i + 1))
  echo "  $ref"
  docker pull --quiet "$ref" >/dev/null
  docker save "$ref" | gzip -6 > "$(printf '%s/images/%03d.tar.gz' "$OUT" "$i")"
done < "$OUT/images.txt"

if [ "$MODELS" = --modeles ]; then
  echo "== modèles d'IA"
  mkdir -p "$OUT/models"
  ollama_img="$(awk '$2 ~ /ollama\/ollama/ {print $2; exit}' "$OUT/images.txt")"
  vol="qualitos-paquet-modeles-$$"
  docker volume create "$vol" >/dev/null
  trap 'docker rm -f "$vol" >/dev/null 2>&1; docker volume rm "$vol" >/dev/null 2>&1' EXIT
  docker run -d --name "$vol" -e OLLAMA_MODELS=/models -v "$vol":/models "$ollama_img" >/dev/null
  sleep 3
  docker exec "$vol" ollama pull "$QOS_OLLAMA_MODEL"
  docker exec "$vol" ollama pull bge-m3
  docker exec "$vol" tar -C /models -czf - . > "$OUT/models/ollama-models.tar.gz"
fi

echo "== scripts d'installation"
git -C "$ROOT" archive --format=tar HEAD infra docs/runbooks docs/adr | tar -x -C "$OUT" --one-top-level=installation
echo "$VERSION" > "$OUT/VERSION"

echo "== empreintes et signature"
(cd "$OUT" && find . -type f ! -name 'SHA256SUMS*' | sed 's#^\./##' | LC_ALL=C sort | xargs -d '\n' sha256sum > SHA256SUMS)
openssl pkeyutl -sign -inkey "$QOS_BUNDLE_KEY" -rawin -in "$OUT/SHA256SUMS" | openssl base64 -A > "$OUT/SHA256SUMS.sig"
qos_bundle_verify "$OUT" "$HERE/editeur-paquet.pub"
echo "Paquet $VERSION prêt et vérifié : $OUT ($(du -sh "$OUT" | cut -f1))"
