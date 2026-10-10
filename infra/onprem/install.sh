#!/usr/bin/env bash
# Installation (et mise à jour) de QualitOS chez un client — édition on-premise.
#
#   ./infra/onprem/install.sh qualitos.conf <version>   installer ou mettre à jour
#   ./infra/onprem/install.sh licence qualitos.conf     remplacer la licence seulement
#   ./infra/onprem/install.sh verifier qualitos.conf    vérifier les prérequis, sans rien changer
#
# L'installation VÉRIFIE d'abord tout ce qui ferait échouer le déploiement au
# milieu — outils, cluster, classe d'ingress et de stockage, certificat, licence,
# annuaire — et s'arrête avant d'avoir touché au cluster si quelque chose manque.
# Une installation qui échoue à mi-chemin chez un client coûte une journée ;
# une liste de prérequis manquants, cinq minutes.
#
# Rejouable : relancer avec une nouvelle version met à jour, avec un vidage de
# sûreté de la base juste avant (voir deploy.sh). ADR 0082, 0083.
#
# Site sans Internet : QOS_BUNDLE=<dossier du paquet hors ligne> (ADR 0085). Le
# paquet est vérifié, ses images poussées vers QOS_REGISTRY et QOS_MIRROR_REGISTRY
# depuis ce poste (docker connecté au registre du site), ses modèles d'IA posés.

set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
# shellcheck source=lib.sh
. "$HERE/lib.sh"

usage() {
  sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//' >&2
  exit 2
}

ok()   { printf '  \033[32m✓\033[0m %s\n' "$*"; }
warn() { printf '  \033[33m!\033[0m %s\n' "$*" >&2; WARNINGS=$((WARNINGS + 1)); }
ko()   { printf '  \033[31m✗\033[0m %s\n' "$*" >&2; ERRORS=$((ERRORS + 1)); }
ERRORS=0
WARNINGS=0

MODE=install
case "${1:-}" in
  licence|license) MODE=licence; CONF="${2:-}"; VERSION="" ;;
  verifier|check)  MODE=verifier; CONF="${2:-}"; VERSION="" ;;
  '') usage ;;
  *) CONF="$1"; VERSION="${2:-}"; [ -n "$VERSION" ] || usage ;;
esac
[ -n "${CONF:-}" ] || usage

qos_load_conf "$CONF"
: "${QOS_NAMESPACE:=qualitos}"
: "${QOS_INGRESS_CLASS:=nginx}"
: "${QOS_STORAGE_CLASS:=local-path}"
: "${QOS_TLS_MODE:=secret}"
: "${QOS_TLS_SECRET:=qualitos-tls}"

BUNDLE=""
case "${QOS_BUNDLE:-}" in
  '') ;;
  /*) BUNDLE="$QOS_BUNDLE" ;;
  *)  BUNDLE="$(cd "$(dirname "$CONF")" && pwd)/$QOS_BUNDLE" ;;
esac

case "${QOS_LICENSE_FILE:-}" in
  '') LICENSE_FILE="" ;;
  /*) LICENSE_FILE="$QOS_LICENSE_FILE" ;;
  *)  LICENSE_FILE="$(cd "$(dirname "$CONF")" && pwd)/$QOS_LICENSE_FILE" ;;
esac

# --- Renouvellement de licence : le secret, et rien d'autre ---------------------
if [ "$MODE" = licence ]; then
  [ -f "$LICENSE_FILE" ] || { echo "licence introuvable : $LICENSE_FILE" >&2; exit 1; }
  qos_license_tenant "$LICENSE_FILE" >/dev/null || { echo "ce fichier n'est pas une licence QualitOS" >&2; exit 1; }
  kubectl -n "$QOS_NAMESPACE" create secret generic qualitos-license \
    --from-file=license.lic="$LICENSE_FILE" --dry-run=client -o yaml | kubectl apply -f - >/dev/null
  echo "Licence de « $(qos_license_customer "$LICENSE_FILE") » installée."
  echo "Elle est prise en compte en moins d'une minute, sans redémarrage (page Administration › Licence)."
  exit 0
fi

echo
echo "Vérification des prérequis — ${QOS_HOST:-?}"

# --- Configuration --------------------------------------------------------------
for v in QOS_HOST QOS_NAMESPACE QOS_ADMIN_EMAIL QOS_LICENSE_FILE QOS_REGISTRY; do
  if [ -n "${!v:-}" ]; then ok "$v renseigné"; else ko "$v manquant dans $CONF"; fi
done
if [[ "${QOS_ADMIN_EMAIL:-}" =~ ^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$ ]]; then :; else
  ko "QOS_ADMIN_EMAIL n'est pas une adresse e-mail : ${QOS_ADMIN_EMAIL:-}"
fi
if [[ "$QOS_NAMESPACE" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ ]]; then :; else
  ko "QOS_NAMESPACE invalide pour Kubernetes : $QOS_NAMESPACE"
fi

# --- Outils ---------------------------------------------------------------------
for tool in kubectl helm openssl python3 base64 curl; do
  if command -v "$tool" >/dev/null 2>&1; then ok "outil $tool"; else ko "outil $tool introuvable"; fi
done

# --- Licence --------------------------------------------------------------------
if [ -n "$LICENSE_FILE" ] && [ -f "$LICENSE_FILE" ]; then
  TENANT_ID="$(qos_license_tenant "$LICENSE_FILE" || true)"
  if [ -n "$TENANT_ID" ]; then
    ok "licence de « $(qos_license_customer "$LICENSE_FILE") » (client $TENANT_ID)"
  else
    ko "$LICENSE_FILE n'est pas une licence QualitOS lisible"
  fi
else
  ko "licence introuvable : ${LICENSE_FILE:-(non renseignée)}"
fi

# --- Cluster --------------------------------------------------------------------
if command -v kubectl >/dev/null 2>&1 && kubectl get nodes >/dev/null 2>&1; then
  ok "cluster joignable ($(kubectl get nodes --no-headers 2>/dev/null | wc -l | tr -d ' ') nœud(s))"
  CLUSTER=1
else
  ko "cluster injoignable : kubectl n'est pas configuré sur ce poste (KUBECONFIG ?)"
  CLUSTER=0
fi

if [ "$CLUSTER" = 1 ]; then
  if kubectl get ingressclass "$QOS_INGRESS_CLASS" >/dev/null 2>&1; then
    ok "classe d'ingress $QOS_INGRESS_CLASS"
  else
    ko "classe d'ingress $QOS_INGRESS_CLASS absente (installer ingress-nginx, ou régler QOS_INGRESS_CLASS)"
  fi
  if kubectl get storageclass "$QOS_STORAGE_CLASS" >/dev/null 2>&1; then
    ok "classe de stockage $QOS_STORAGE_CLASS"
  else
    ko "classe de stockage $QOS_STORAGE_CLASS absente (kubectl get storageclass)"
  fi

  case "$QOS_TLS_MODE" in
    secret)
      if kubectl -n "$QOS_NAMESPACE" get secret "$QOS_TLS_SECRET" >/dev/null 2>&1; then
        ok "certificat TLS (secret $QOS_TLS_SECRET)"
      else
        ko "certificat TLS absent : kubectl create namespace $QOS_NAMESPACE ; kubectl -n $QOS_NAMESPACE create secret tls $QOS_TLS_SECRET --cert=cert.pem --key=cle.pem"
      fi ;;
    letsencrypt|issuer)
      if [ -z "${QOS_CLUSTER_ISSUER:-}" ]; then
        ko "QOS_CLUSTER_ISSUER requis avec QOS_TLS_MODE=$QOS_TLS_MODE"
      elif kubectl get clusterissuer "$QOS_CLUSTER_ISSUER" >/dev/null 2>&1; then
        ok "émetteur de certificats $QOS_CLUSTER_ISSUER"
      else
        ko "émetteur $QOS_CLUSTER_ISSUER absent (cert-manager installé ? kubectl get clusterissuer)"
      fi ;;
    *) ko "QOS_TLS_MODE invalide : $QOS_TLS_MODE (letsencrypt | issuer | secret)" ;;
  esac

  if [ -n "${QOS_PULL_SECRET:-}" ]; then
    if kubectl -n "$QOS_NAMESPACE" get secret "$QOS_PULL_SECRET" >/dev/null 2>&1; then
      ok "accès au registre (secret $QOS_PULL_SECRET)"
    else
      ko "secret d'accès au registre $QOS_PULL_SECRET absent du namespace $QOS_NAMESPACE"
    fi
  fi

  # Mémoire : la pile complète, modèle d'IA local compris, demande ~16 Go.
  MEM_KI="$(kubectl get nodes -o jsonpath='{range .items[*]}{.status.allocatable.memory}{"\n"}{end}' 2>/dev/null \
    | sed -n 's/Ki$//p' | awk '{s+=$1} END {print s+0}')"
  if [ "${MEM_KI:-0}" -gt 0 ]; then
    MEM_GO=$((MEM_KI / 1024 / 1024))
    if [ -z "${QOS_OLLAMA_URL:-}" ] && [ "$MEM_GO" -lt 16 ]; then
      warn "mémoire allouable ${MEM_GO} Go : 16 Go conseillés avec le modèle d'IA dans le cluster"
    else
      ok "mémoire allouable ${MEM_GO} Go"
    fi
  fi
fi

# --- Réseau ---------------------------------------------------------------------
if getent hosts "${QOS_HOST:-}" >/dev/null 2>&1; then
  ok "le nom $QOS_HOST résout"
else
  warn "le nom ${QOS_HOST:-} ne résout pas depuis ce poste : l'enregistrement DNS doit exister avant la première connexion"
fi
if [ -n "${QOS_OLLAMA_URL:-}" ]; then
  if curl -sf -m 5 -o /dev/null "${QOS_OLLAMA_URL%/}/api/version"; then
    ok "modèle d'IA du site joignable ($QOS_OLLAMA_URL)"
  else
    warn "modèle d'IA du site injoignable depuis ce poste ($QOS_OLLAMA_URL) — à vérifier depuis le cluster"
  fi
fi

# --- Annuaire -------------------------------------------------------------------
if [ -n "${QOS_LDAP_URL:-}" ]; then
  for v in QOS_LDAP_USERS_DN QOS_LDAP_BIND_DN; do
    if [ -n "${!v:-}" ]; then ok "$v renseigné"; else ko "$v requis quand QOS_LDAP_URL est renseigné"; fi
  done
  if [ -z "${QOS_LDAP_BIND_PASSWORD:-}" ]; then
    warn "QOS_LDAP_BIND_PASSWORD vide : liaison anonyme à l'annuaire"
  fi
  case "$QOS_LDAP_URL" in
    ldaps://*) ok "annuaire en LDAPS" ;;
    ldap://*)  warn "annuaire en LDAP non chiffré : préférer ldaps://" ;;
    *)         ko "QOS_LDAP_URL doit commencer par ldap:// ou ldaps://" ;;
  esac
fi

if [ -n "$BUNDLE" ]; then
  if qos_bundle_verify "$BUNDLE" "$HERE/bundle/editeur-paquet.pub"; then
    ok "paquet hors ligne $(cat "$BUNDLE/VERSION") : signature et empreintes vérifiées"
  else
    ko "paquet hors ligne refusé : $BUNDLE"
  fi
  if [ -n "$VERSION" ] && [ "$(cat "$BUNDLE/VERSION" 2>/dev/null)" != "${VERSION#v}" ]; then
    ko "le paquet porte la version $(cat "$BUNDLE/VERSION" 2>/dev/null), pas ${VERSION#v}"
  fi
  [ -n "${QOS_MIRROR_REGISTRY:-}" ] || ko "QOS_MIRROR_REGISTRY requis avec un paquet hors ligne (images tierces)"
  command -v docker >/dev/null 2>&1 && ok "outil docker (envoi des images)" || ko "outil docker introuvable : requis pour pousser les images du paquet"
fi

echo
if [ "$ERRORS" -gt 0 ]; then
  echo "$ERRORS prérequis manquant(s) : rien n'a été modifié. Corrigez puis relancez." >&2
  exit 1
fi
echo "Prérequis réunis ($WARNINGS avertissement(s))."

if [ "$MODE" = verifier ]; then
  exit 0
fi

if [ -n "$BUNDLE" ]; then
  echo
  echo "Envoi des images du paquet vers le registre du site"
  qos_bundle_load "$BUNDLE" push
fi

"$ROOT/infra/k8s/deploy.sh" onprem "$VERSION" "$CONF"

# Les modèles d'IA du paquet, dans l'Ollama du cluster : le téléchargement en
# ligne (job ollama-models) ne peut pas aboutir sur un site isolé.
if [ -n "$BUNDLE" ] && [ -z "${QOS_OLLAMA_URL:-}" ]; then
  kubectl -n "$QOS_NAMESPACE" delete job ollama-models --ignore-not-found >/dev/null
  if [ -f "$BUNDLE/models/ollama-models.tar.gz" ]; then
    kubectl -n "$QOS_NAMESPACE" exec -i deploy/ollama -- tar --no-same-owner -C /models -xzf - \
      < "$BUNDLE/models/ollama-models.tar.gz" && echo "  modèles d'IA posés depuis le paquet"
  else
    echo "  ATTENTION : paquet sans modèles d'IA (--modeles) : fonctions d'IA indisponibles" >&2
  fi
fi
