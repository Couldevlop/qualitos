#!/usr/bin/env bash
# Déploiement du site de présentation (qualitos.fr).
#
#   ./infra/k8s/site/deploy.sh main-1b762961abcd
#
# IDEMPOTENT : le rejouer sur un site déjà en place ne recrée rien et ne touche
# pas au secret du relais de courriel.
#
# Il ne touche NI au namespace de la préproduction, NI à son ingress : la
# démonstration et le site public partagent une machine, et rien d'autre.
set -euo pipefail

TAG="${1:-}"
if [ -z "$TAG" ]; then
  echo "usage: $0 <tag-image>   (ex. main-1b762961abcd)" >&2
  exit 2
fi

ICI="$(cd "$(dirname "$0")" && pwd)"
NS=qualitos-site

log() { printf '%s  %s\n' "$(date -Is)" "$*"; }

# Le secret du relais est créé À LA MAIN, une fois, sur le nœud — il n'est pas
# versionné et ne doit pas l'être (§18.2.3). On vérifie seulement qu'il existe :
# sans lui, le service de contact démarrerait puis refuserait tout envoi, et le
# formulaire répondrait « merci » sans que rien ne parte.
kubectl get namespace "$NS" >/dev/null 2>&1 || kubectl create namespace "$NS"

if ! kubectl -n "$NS" get secret site-contact-smtp >/dev/null 2>&1; then
  cat >&2 <<'MANQUE'
ERREUR : le secret site-contact-smtp est absent du namespace qualitos-site.

Le créer une fois, sur le nœud, avec les identifiants de la boîte
contact@qualitos.fr (cf. docs/runbooks/site-qualitos-fr.md) :

  kubectl -n qualitos-site create secret generic site-contact-smtp \
    --from-literal=SMTP_HOTE=ssl0.ovh.net \
    --from-literal=SMTP_PORT=465 \
    --from-literal=SMTP_UTILISATEUR=contact@qualitos.fr \
    --from-literal=SMTP_MOT_DE_PASSE='<mot de passe de la boîte>' \
    --from-literal=SMTP_EXPEDITEUR=contact@qualitos.fr \
    --from-literal=SMTP_DESTINATAIRE=contact@qualitos.fr
MANQUE
  exit 1
fi

log "application des manifestes au tag $TAG"
sed "s|__TAG__|$TAG|g" "$ICI/site.yaml" | kubectl apply -f -

log "attente du déploiement"
kubectl -n "$NS" rollout status deployment/site         --timeout=180s
kubectl -n "$NS" rollout status deployment/site-contact --timeout=180s

log "site déployé en $TAG"
