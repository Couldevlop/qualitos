#!/usr/bin/env bash
# Valeurs Helm PROPRES AU SITE d'une installation on-premise (ADR 0083), écrites
# sur la sortie standard à partir de qualitos.conf (déjà chargé par deploy.sh).
#
# S'ajoutent à qualitos/values-onprem.yaml, qui porte ce qui vaut pour tout
# client. Ici : l'hôte et les URL qui en découlent, les certificats, le registre
# d'images, le modèle d'IA.
#
# Aucune annotation « snippet » d'ingress-nginx par défaut : les versions
# récentes les refusent, et le conteneur web pose déjà lui-même sa CSP et ses
# en-têtes de sécurité (apps/web/nginx.conf). Seul le WAF (QOS_WAF=true) en
# demande, pour ses exclusions — le contrôleur doit alors les autoriser.

set -euo pipefail

: "${QOS_HOST:?}" "${QOS_REGISTRY:?}"
: "${QOS_INGRESS_CLASS:=nginx}"
: "${QOS_TLS_MODE:=secret}"
: "${QOS_TLS_SECRET:=qualitos-tls}"
: "${QOS_CLUSTER_ISSUER:=}"
: "${QOS_PULL_SECRET:=}"
: "${QOS_WAF:=false}"
: "${QOS_INGRESS_SNIPPETS:=false}"
# Le WAF passe lui-même par des annotations snippet : il les suppose autorisées.
[ "$QOS_WAF" = true ] && QOS_INGRESS_SNIPPETS=true
: "${QOS_OLLAMA_URL:=}"
: "${QOS_OLLAMA_MODEL:=hf.co/OpenLLM-France/Lucie-7B-Instruct-v1.1-gguf:Q4_K_M}"

ORIGIN="https://$QOS_HOST"
ISSUER="$ORIGIN/auth/realms/qualitos"
OLLAMA_URL="${QOS_OLLAMA_URL:-http://ollama:11434}"
# L'hôte du modèle, seul autorisé en sortie par le service d'IA (allowlist).
OLLAMA_HOST="$(printf '%s' "$OLLAMA_URL" | sed -E 's#^[a-z]+://##; s#[:/].*$##')"

case "$QOS_TLS_MODE" in
  letsencrypt|issuer)
    [ -n "$QOS_CLUSTER_ISSUER" ] || { echo "QOS_CLUSTER_ISSUER est requis avec QOS_TLS_MODE=$QOS_TLS_MODE" >&2; exit 1; } ;;
  secret) ;;
  *) echo "QOS_TLS_MODE invalide : $QOS_TLS_MODE (letsencrypt | issuer | secret)" >&2; exit 1 ;;
esac

cat <<YAML
global:
  imageRegistry: "$QOS_REGISTRY"
  keycloak:
    url: "$ORIGIN/auth"
    realm: qualitos
YAML
if [ -n "$QOS_PULL_SECRET" ]; then
  cat <<YAML
  imagePullSecrets:
    - name: "$QOS_PULL_SECRET"
YAML
fi

cat <<YAML
ingress:
  enabled: true
  className: "$QOS_INGRESS_CLASS"
  snippets: $QOS_INGRESS_SNIPPETS
  host: "$QOS_HOST"
  tls:
    enabled: true
    secretName: "$QOS_TLS_SECRET"
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: 12m
YAML
if [ "$QOS_TLS_MODE" != secret ]; then
  echo "    cert-manager.io/cluster-issuer: \"$QOS_CLUSTER_ISSUER\""
fi
if [ "$QOS_WAF" = true ]; then
  cat <<'YAML'
    nginx.ingress.kubernetes.io/enable-modsecurity: "true"
    nginx.ingress.kubernetes.io/enable-owasp-core-rules: "true"
    nginx.ingress.kubernetes.io/modsecurity-snippet: |
      SecRuleEngine On
      SecRule REQUEST_URI "@rx ^/[a-z]{2}/assets/config\.json$" "id:1000100,phase:1,pass,nolog,ctl:ruleEngine=Off"
      SecRuleRemoveById 911100
YAML
fi

cat <<YAML
services:
  web:
    env:
      QOS_API_BASE_URL: "$ORIGIN"
      QOS_KEYCLOAK_ISSUER: "$ISSUER"
      QOS_KEYCLOAK_CLIENT_ID: qualitos-web
      QOS_CONNECT_SRC: "'self' $ORIGIN"
  api-quality-engine:
    env:
      STORAGE_S3_PUBLIC_ENDPOINT: "$ORIGIN"
  ai-service:
    env:
      KEYCLOAK_ISSUER: "$ISSUER"
      OLLAMA_BASE_URL: "$OLLAMA_URL"
      OLLAMA_MODEL: "$QOS_OLLAMA_MODEL"
      QOS_AI_ALLOWED_HOSTS: "$OLLAMA_HOST"
YAML
