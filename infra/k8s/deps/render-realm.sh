#!/usr/bin/env bash
# Génère le realm Keycloak d'un environnement à partir de infra/keycloak/realm-export.json,
# puis le publie en ConfigMap. Rien de sensible n'est versionné : les mots de passe
# sont fournis en variables d'environnement et n'existent que dans le cluster.
#
# Deux écarts entre le realm de développement et un environnement exposé :
#
#   1. Les URI de redirection pointent sur http://localhost:4200/*. Telles quelles,
#      Keycloak refuse la redirection après authentification et la connexion échoue.
#   2. Les mots de passe du fichier valent le nom du compte (superadmin/superadmin,
#      admin/admin). Acceptable en local, inacceptable sur un domaine public : ce
#      sont des comptes super_admin et admin_tenant. Ils sont donc remplacés.
#      Le compte `demo` conserve volontairement `demo/demo` — c'est un compte de
#      démonstration, sans privilège d'administration (quality_manager + user).
#
# Usage :
#   export QOS_HOST=qualitos.openlabconsulting.com
#   export QOS_SUPERADMIN_PASSWORD=... QOS_ADMIN_PASSWORD=...
#   ./render-realm.sh <namespace>
#
# Les mots de passe non fournis sont générés et affichés UNE FOIS en fin
# d'exécution : à consigner immédiatement dans le gestionnaire de secrets.
#
# Édition on-premise (ADR 0083) — QOS_EDITION=onprem, avec QOS_TENANT_ID (le
# client que fixe la licence) et QOS_ADMIN_EMAIL : le realm ne garde AUCUN compte
# de démonstration ni de super-administrateur (la console éditeur n'existe pas
# chez un client). Il porte un seul administrateur, rattaché au client de la
# licence, avec un mot de passe provisoire à changer et un second facteur à
# enrôler dès la première connexion.

set -euo pipefail

NS="${1:?usage: render-realm.sh <namespace>}"
HOST="${QOS_HOST:?QOS_HOST est requis (ex. qualitos.openlabconsulting.com)}"
SRC="$(cd "$(dirname "$0")/../../.." && pwd)/infra/keycloak/realm-export.json"

[ -f "$SRC" ] || { echo "realm introuvable : $SRC" >&2; exit 1; }

gen() { openssl rand -base64 18 | tr -d '/+=' | cut -c1-20; }
EDITION="${QOS_EDITION:-saas}"
TENANT_ID="${QOS_TENANT_ID:-}"
ADMIN_EMAIL="${QOS_ADMIN_EMAIL:-}"
if [ "$EDITION" = onprem ]; then
  [ -n "$TENANT_ID" ] || { echo "QOS_TENANT_ID est requis en on-premise (lu dans la licence)" >&2; exit 1; }
  [ -n "$ADMIN_EMAIL" ] || { echo "QOS_ADMIN_EMAIL est requis en on-premise" >&2; exit 1; }
fi
SUPERADMIN_PWD="${QOS_SUPERADMIN_PASSWORD:-$(gen)}"
ADMIN_PWD="${QOS_ADMIN_PASSWORD:-$(gen)}"

OUT="$(mktemp)"
trap 'rm -f "$OUT"' EXIT

python3 - "$SRC" "$OUT" "$HOST" "$SUPERADMIN_PWD" "$ADMIN_PWD" "$EDITION" "$TENANT_ID" "$ADMIN_EMAIL" <<'PY'
import io, json, sys

src, out, host, superadmin_pwd, admin_pwd, edition, tenant_id, admin_email = sys.argv[1:9]
realm = json.load(io.open(src, encoding="utf-8"))

origin = "https://%s" % host

for client in realm.get("clients") or []:
    if client.get("clientId") == "qualitos-web":
        # On REMPLACE au lieu d'ajouter : laisser localhost dans la liste d'un
        # environnement exposé ouvrirait une redirection vers une machine tierce
        # si un poste de développement écoutait sur ce port.
        client["redirectUris"] = ["%s/*" % origin]
        client["webOrigins"] = [origin]
        client["rootUrl"] = origin
        client["baseUrl"] = "/"
        # L'URI de POST-DÉCONNEXION est un réglage distinct des URI de
        # redirection, et Keycloak ne retombe PAS sur celles-ci : attribut absent
        # ou vide = aucune redirection autorisée après déconnexion, et l'écran
        # « Invalid redirect uri » remplace le retour à l'application. Oublier
        # cette ligne laissait donc une déconnexion cassée sur tout domaine
        # exposé — la connexion, elle, fonctionnait, ce qui rendait la panne
        # d'autant plus tardive à découvrir.
        client.setdefault("attributes", {})["post.logout.redirect.uris"] = "%s/*" % origin

if edition == "onprem":
    # Seuls les comptes de service des clients techniques restent ; un seul
    # administrateur humain, rattaché au client de la licence.
    realm["users"] = [u for u in realm.get("users") or [] if u.get("serviceAccountClientId")]
    realm["users"].append({
        "username": "admin",
        "email": admin_email,
        "enabled": True,
        "emailVerified": True,
        "attributes": {"tenant_id": [tenant_id]},
        "realmRoles": ["admin_tenant"],
        "credentials": [{"type": "password", "value": admin_pwd, "temporary": True}],
        "requiredActions": ["CONFIGURE_TOTP", "UPDATE_PASSWORD"],
    })

passwords = {} if edition == "onprem" else {"superadmin": superadmin_pwd, "admin": admin_pwd}
for user in realm.get("users") or []:
    pwd = passwords.get(user.get("username"))
    if pwd:
        user["credentials"] = [
            {"type": "password", "value": pwd, "temporary": False}
        ]

# Refuse tout échange non chiffré ailleurs qu'en boucle locale. Le TLS est
# terminé par l'ingress, mais ce réglage empêche Keycloak d'accepter une session
# en clair si quelqu'un l'atteignait directement dans le cluster.
realm["sslRequired"] = "external"

io.open(out, "w", encoding="utf-8").write(json.dumps(realm, ensure_ascii=False, indent=2))
print("realm rendu : %d utilisateurs, %d clients, hôte %s"
      % (len(realm.get("users") or []), len(realm.get("clients") or []), host))
PY

kubectl -n "$NS" create configmap qualitos-keycloak-realm \
  --from-file=qualitos-realm.json="$OUT" \
  --dry-run=client -o yaml | kubectl apply -f -

# Les deux mots de passe générés sont AUSSI déposés dans un secret du namespace.
#
# Pourquoi : les afficher une seule fois était une invitation à les perdre, et
# c'est exactement ce qui est arrivé — la préproduction a tourné des semaines
# avec deux comptes d'administration dont personne ne connaissait plus le mot de
# passe, seul `demo` restant utilisable. Keycloak ne stocke que des empreintes :
# un mot de passe non consigné n'est pas « oublié », il est perdu.
#
# `--dry-run=client | apply` seulement si le secret n'existe PAS encore : le
# rendu du realm peut être rejoué, et réécrire le secret avec des mots de passe
# fraîchement générés le désaccorderait de ce que Keycloak connaît déjà. Même
# règle que les secrets d'infrastructure de deploy.sh — générés une fois, jamais
# régénérés.
if kubectl -n "$NS" get secret qualitos-realm-accounts >/dev/null 2>&1; then
  ACCOUNTS_NOTE="secret qualitos-realm-accounts déjà en place, inchangé"
elif [ "$EDITION" = onprem ]; then
  kubectl -n "$NS" create secret generic qualitos-realm-accounts     --from-literal=ADMIN_USERNAME=admin --from-literal=ADMIN_PASSWORD="$ADMIN_PWD" >/dev/null
  ACCOUNTS_NOTE="secret qualitos-realm-accounts créé"
else
  kubectl -n "$NS" create secret generic qualitos-realm-accounts     --from-literal=SUPERADMIN_USERNAME=superadmin     --from-literal=SUPERADMIN_PASSWORD="$SUPERADMIN_PWD"     --from-literal=ADMIN_USERNAME=admin     --from-literal=ADMIN_PASSWORD="$ADMIN_PWD" >/dev/null
  ACCOUNTS_NOTE="secret qualitos-realm-accounts créé"
fi

if [ "$EDITION" = onprem ]; then
  cat <<EOF

ConfigMap qualitos-keycloak-realm appliquée dans le namespace ${NS}.
${ACCOUNTS_NOTE}.

Administrateur du client (${ADMIN_EMAIL}) :
  identifiant  : admin
  mot de passe : ${ADMIN_PWD}   (PROVISOIRE — à changer à la première connexion)

La première connexion demande aussi l'enrôlement d'un second facteur (TOTP).
Pour relire ce mot de passe provisoire tant qu'il n'a pas été changé :

  kubectl -n ${NS} get secret qualitos-realm-accounts -o jsonpath='{.data.ADMIN_PASSWORD}' | base64 -d
EOF
  exit 0
fi

cat <<EOF

ConfigMap qualitos-keycloak-realm appliquée dans le namespace ${NS}.
${ACCOUNTS_NOTE}.

Comptes du realm :
  superadmin / ${SUPERADMIN_PWD}      (super_admin)
  admin      / ${ADMIN_PWD}      (admin_tenant)
  demo       / demo                    (quality_manager, user) — compte de démonstration

Ces mots de passe ne sont écrits dans AUCUN fichier du dépôt. Pour les relire
plus tard :

  kubectl -n ${NS} get secret qualitos-realm-accounts     -o jsonpath='{.data.SUPERADMIN_PASSWORD}' | base64 -d

Les deux comptes portent l'action obligatoire CONFIGURE_TOTP : la première
connexion PAR LE NAVIGATEUR demandera l'enrôlement d'un code à usage unique, et
le flux « password » leur reste refusé tant qu'il n'est pas fait.

Rappel : le realm n'est importé qu'au PREMIER démarrage de Keycloak sur une base
vide. Sur une instance déjà initialisée, ce rendu ne change rien — il faut alors
passer par la console d'administration ou kcadm.
EOF
