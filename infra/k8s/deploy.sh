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
#   4. dépendances d'état (PostgreSQL, Qdrant, Ollama, Keycloak)
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

say "Environnement $ENV — namespace $NS, hôte $HOST, version $VERSION"

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

say "3/6 Realm Keycloak"
if kubectl -n "$NS" get configmap qualitos-keycloak-realm >/dev/null 2>&1; then
  echo "  realm déjà publié, inchangé (l'import Keycloak n'a lieu qu'au premier démarrage)"
else
  QOS_HOST="$HOST" "$DEPS/render-realm.sh" "$NS"
fi

say "4/6 Dépendances d'état"
kubectl -n "$NS" apply -f "$DEPS/10-postgres.yaml" \
                       -f "$DEPS/30-qdrant.yaml" \
                       -f "$DEPS/40-ollama-external.yaml"
sed "s/__QOS_HOST__/$HOST/g" "$DEPS/20-keycloak.yaml" | kubectl -n "$NS" apply -f -

kubectl -n "$NS" rollout status deploy/postgres --timeout=180s
kubectl -n "$NS" rollout status deploy/qdrant   --timeout=180s
kubectl -n "$NS" rollout status deploy/keycloak --timeout=420s

say "5/6 Secrets applicatifs"
# Les services Spring lisent DB_USER et DB_PASSWORD (cf. application.yml). On les
# dérive du secret PostgreSQL plutôt que de les saisir deux fois : deux sources
# pour le même mot de passe finissent toujours par diverger.
PG_USER="$(kubectl -n "$NS" get secret qualitos-postgres -o jsonpath='{.data.POSTGRES_USER}' | base64 -d)"
PG_PWD="$(kubectl -n "$NS" get secret qualitos-postgres -o jsonpath='{.data.POSTGRES_PASSWORD}' | base64 -d)"
NLQ_PWD="$(kubectl -n "$NS" get secret qualitos-postgres -o jsonpath='{.data.NLQ_RO_PASSWORD}' | base64 -d)"

for svc in api-core api-quality-engine api-iot-hub; do
  kubectl -n "$NS" create secret generic "qualitos-$svc" \
    --from-literal=DB_USER="$PG_USER" \
    --from-literal=DB_PASSWORD="$PG_PWD" \
    --dry-run=client -o yaml | kubectl apply -f - >/dev/null
  echo "  secret qualitos-$svc : à jour"
done

kubectl -n "$NS" create secret generic qualitos-ai-service \
  --from-literal=NLQ_READONLY_DSN="postgresql://qualitos_nlq_ro:${NLQ_PWD}@postgres:5432/qualitos_quality" \
  --dry-run=client -o yaml | kubectl apply -f - >/dev/null
echo "  secret qualitos-ai-service : à jour"

say "6/6 Chart applicatif"
helm upgrade --install "qualitos-$ENV" "$CHART" \
  --namespace "$NS" \
  --values "$VALUES" \
  --set "global.imageTag=$VERSION" \
  --wait --timeout 10m

say "Terminé"
kubectl -n "$NS" get pods
cat <<EOF

Environnement  : $ENV
Namespace      : $NS
Version        : $VERSION
Application    : https://$HOST
Keycloak       : https://$HOST/auth

Promotion en production, une fois cette version validée :

    ./infra/k8s/deploy.sh prod $VERSION

EOF
