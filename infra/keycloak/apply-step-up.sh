#!/usr/bin/env bash
# Installe l'authentification par paliers sur un realm Keycloak DÉJÀ EN SERVICE.
#
# Pourquoi ce script existe : `--import-realm` n'importe le realm qu'au premier
# démarrage sur une base vide. Sur une instance déjà initialisée — préproduction,
# production — modifier `realm-export.json` ne change rien.
#
# Ce qu'il fait :
#
#   1. Pose la carte des paliers : `silver` = mot de passe, `gold` = mot de passe
#      + code à usage unique. C'est elle qui fait sortir le palier atteint dans
#      la revendication `acr` du jeton.
#   2. Construit un flux de connexion NEUF, `browser-mfa-loa`, à côté de
#      l'existant, et bascule le realm dessus. Rien n'est touché dans le flux en
#      place : revenir en arrière, c'est reposer `browserFlow` sur son ancienne
#      valeur, que le script affiche avant de basculer.
#
# Le point à comprendre, vérifié sur Keycloak 25 : sans sous-flux déclarant le
# palier 1, Keycloak traite une connexion qui ne demande aucun palier comme une
# demande du palier LE PLUS ÉLEVÉ, et impose le code à usage unique à TOUS les
# utilisateurs. Déclarer le palier 1 est ce qui garde la connexion ordinaire
# ordinaire.
#
# Après ce script :
#   connexion ordinaire        → mot de passe        → acr = silver
#   demande `acr_values=gold`  → mot de passe + code → acr = gold
#
# Le moteur qualité n'accepte d'approuver un control plan, ou d'accepter une
# proposition de révision, que sur un jeton `gold` ; le front redemande ce palier
# quand le serveur répond 403 « step-up-required ».
#
# Usage :
#   export KC_URL=https://auth.example.com KC_REALM=qualitos
#   export KC_ADMIN=admin KC_ADMIN_PASSWORD=...
#   ./apply-step-up.sh
#
# Idempotent : le flux est détruit puis reconstruit à chaque passage. Les rôles
# qui portent déjà un code obligatoire à la connexion (super_admin, admin_tenant)
# le conservent.

set -euo pipefail

KC_URL="${KC_URL:-http://localhost:8080}"
KC_REALM="${KC_REALM:-qualitos}"
KC_ADMIN="${KC_ADMIN:-admin}"
KC_ADMIN_PASSWORD="${KC_ADMIN_PASSWORD:?KC_ADMIN_PASSWORD requis}"
FLOW="${FLOW:-browser-mfa-loa}"
FORMS="$FLOW-forms"
LOA1="$FLOW-loa1"
LOA2="$FLOW-loa2"
OTP_SUPERADMIN="$FLOW-otp-superadmin"
OTP_ADMIN="$FLOW-otp-admin"
MAX_AGE="${STEP_UP_MAX_AGE:-3600}"
PY="${PYTHON:-python3}"

say() { printf '%s\n' "$*" >&2; }

TOKEN="$(curl -sS -X POST "$KC_URL/realms/master/protocol/openid-connect/token" \
  -d client_id=admin-cli -d "username=$KC_ADMIN" \
  -d "password=$KC_ADMIN_PASSWORD" -d grant_type=password \
  | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')"
[ -n "$TOKEN" ] || { say "Authentification administrateur refusée."; exit 1; }

api() {
  local method="$1" path="$2"; shift 2
  curl -sS -X "$method" -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" "$KC_URL/admin/realms/$KC_REALM$path" "$@"
}

subflow() {  # flux parent, alias, description
  api POST "/authentication/flows/$1/executions/flow" \
    -d "{\"alias\":\"$2\",\"type\":\"basic-flow\",\"description\":\"$3\",\"provider\":\"registration-page-form\"}" \
    -o /dev/null -w "   $2 (HTTP %{http_code})\n"
}

execution() {  # flux, provider
  api POST "/authentication/flows/$1/executions/execution" \
    -d "{\"provider\":\"$2\"}" -o /dev/null
}

# --- 0. Ce sur quoi on revient en cas de retour arrière ----------------------
REALM_JSON="$(api GET "")"
CURRENT_FLOW="$(printf '%s' "$REALM_JSON" | sed -n 's/.*"browserFlow":"\([^"]*\)".*/\1/p')"
SAVED_FLOW="$(printf '%s' "$REALM_JSON" \
  | sed -n 's/.*"qualitos.previous-browser-flow":"\([^"]*\)".*/\1/p' | head -1 || true)"

# Le flux d'origine est mémorisé dans un attribut du realm dès le premier
# passage : au second, `browserFlow` vaut déjà le flux qu'on reconstruit, et il
# ne serait plus possible de dire sur quoi revenir.
if [ "$CURRENT_FLOW" != "$FLOW" ]; then
  SAVED_FLOW="$CURRENT_FLOW"
  api PUT "" -d "{\"attributes\":{\"qualitos.previous-browser-flow\":\"$SAVED_FLOW\"}}" -o /dev/null
fi
[ -n "$SAVED_FLOW" ] || SAVED_FLOW="browser"

say "Flux de connexion actuel : $CURRENT_FLOW"
say "Retour arrière : reposer browserFlow sur $SAVED_FLOW"
say ""

# --- 1. La carte des paliers ------------------------------------------------
say "→ carte des paliers (acr.loa.map)"
api PUT "" -d "{\"attributes\":{\"acr.loa.map\":\"{\\\"silver\\\":1,\\\"gold\\\":2}\"}}" \
  -o /dev/null -w '   HTTP %{http_code}\n'

# --- 2. Le flux neuf --------------------------------------------------------
# Keycloak supprime un flux par IDENTIFIANT, pas par alias : supprimer par alias
# échoue en silence, le POST suivant répond 409, et le flux du passage précédent
# reste en place — avec, à la clé, un formulaire de mot de passe en double.
say "→ reconstruction du flux $FLOW"
# `|| true` sur la recherche : avec `set -o pipefail`, un grep qui ne trouve rien
# — le cas normal au premier passage — ferait échouer l'affectation et sortir.
FLOW_ID="$(api GET "/authentication/flows" | tr '{' '\n' \
  | grep "\"alias\":\"$FLOW\"" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p' | head -1 || true)"
if [ -n "$FLOW_ID" ]; then
  # Keycloak refuse de supprimer le flux de connexion EN USAGE (HTTP 500). On
  # repose donc d'abord le realm sur son flux d'origine ; sinon la suppression
  # échoue, les créations suivantes répondent 409, et le flux du passage
  # précédent reste en place — avec un mot de passe demandé deux fois.
  if [ "$CURRENT_FLOW" = "$FLOW" ]; then
    api PUT "" -d "{\"browserFlow\":\"$SAVED_FLOW\"}" -o /dev/null \
      -w "   realm reposé sur $SAVED_FLOW (HTTP %{http_code})\n"
  fi
  api DELETE "/authentication/flows/$FLOW_ID" -o /dev/null \
    -w '   ancien flux supprimé (HTTP %{http_code})\n'
fi

api POST "/authentication/flows" \
  -d "{\"alias\":\"$FLOW\",\"description\":\"Connexion par paliers silver/gold\",\"providerId\":\"basic-flow\",\"topLevel\":true,\"builtIn\":false}" \
  -o /dev/null -w '   flux racine (HTTP %{http_code})\n'

# Les descriptions restent en ASCII : elles partent dans un corps JSON envoye
# par curl, et un accent mal encode par le terminal fait repondre 400.
execution "$FLOW" "auth-cookie"
subflow "$FLOW" "$FORMS" "Formulaires"

subflow "$FORMS" "$LOA1" "Palier 1 - mot de passe"
subflow "$FORMS" "$OTP_SUPERADMIN" "Code obligatoire pour super_admin"
subflow "$FORMS" "$OTP_ADMIN" "Code obligatoire pour admin_tenant"
subflow "$FORMS" "$LOA2" "Palier 2 - code a usage unique"

execution "$LOA1" "conditional-level-of-authentication"
execution "$LOA1" "auth-username-password-form"
execution "$OTP_SUPERADMIN" "conditional-user-role"
execution "$OTP_SUPERADMIN" "auth-otp-form"
execution "$OTP_ADMIN" "conditional-user-role"
execution "$OTP_ADMIN" "auth-otp-form"
execution "$LOA2" "conditional-level-of-authentication"
execution "$LOA2" "auth-otp-form"

# --- 3. Exigences, conditions, bascule --------------------------------------
# Cette partie relit les exécutions pour retrouver les identifiants que Keycloak
# vient d'attribuer : l'API de création ne les rend pas.
say "→ exigences, conditions et bascule"
"$PY" - "$KC_URL" "$KC_REALM" "$TOKEN" "$FLOW" "$FORMS" "$LOA1" "$LOA2" \
        "$OTP_SUPERADMIN" "$OTP_ADMIN" "$MAX_AGE" <<'PYEOF'
import json, sys, urllib.request

url, realm, token, flow, forms, loa1, loa2, otp_sa, otp_admin, max_age = sys.argv[1:11]
base = f"{url}/admin/realms/{realm}"


def call(method, path, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    request = urllib.request.Request(f"{base}{path}", data=data, method=method,
                                     headers={"Authorization": f"Bearer {token}",
                                              "Content-Type": "application/json"})
    with urllib.request.urlopen(request) as response:
        body = response.read()
    return json.loads(body) if body else None


def executions(name):
    return call("GET", f"/authentication/flows/{name}/executions")


def require(name, matches, requirement):
    for execution in executions(name):
        if matches(execution):
            execution["requirement"] = requirement
            call("PUT", f"/authentication/flows/{name}/executions", execution)
            return
    raise SystemExit(f"exécution introuvable dans {name}")


def configure(name, provider, alias, config):
    for execution in executions(name):
        if execution.get("providerId") == provider:
            call("POST", f"/authentication/executions/{execution['id']}/config",
                 {"alias": alias, "config": config})
            return
    raise SystemExit(f"{provider} introuvable dans {name}")


def by_provider(provider):
    return lambda execution: execution.get("providerId") == provider


def by_flow(alias):
    return lambda execution: execution.get("displayName") == alias


require(flow, by_provider("auth-cookie"), "ALTERNATIVE")
require(flow, by_flow(forms), "ALTERNATIVE")

for sub in (loa1, otp_sa, otp_admin, loa2):
    require(forms, by_flow(sub), "CONDITIONAL")
    for execution in executions(sub):
        require(sub, by_provider(execution["providerId"]), "REQUIRED")

configure(loa1, "conditional-level-of-authentication", "cfg-loa1",
          {"loa-condition-level": "1", "loa-max-age": max_age})
configure(loa2, "conditional-level-of-authentication", "cfg-loa2",
          {"loa-condition-level": "2", "loa-max-age": max_age})
configure(otp_sa, "conditional-user-role", "cfg-otp-superadmin",
          {"condUserRole": "super_admin"})
configure(otp_admin, "conditional-user-role", "cfg-otp-admin",
          {"condUserRole": "admin_tenant"})

call("PUT", "", {"browserFlow": flow})
print("   flux posé, realm basculé dessus")
PYEOF

# --- 4. Contrôle ------------------------------------------------------------
say ""
say "→ paliers annoncés par le realm"
curl -sS "$KC_URL/realms/$KC_REALM/.well-known/openid-configuration" \
  | "$PY" -c 'import json,sys; print("   acr_values_supported =", json.load(sys.stdin).get("acr_values_supported"))'

say ""
say "À vérifier avant de considérer la bascule faite :"
say "  - une connexion ordinaire ne demande PAS de code et rend acr = silver ;"
say "  - une connexion avec acr_values=gold demande le code et rend acr = gold."
say "Si le premier point échoue, le palier 1 est mal posé et Keycloak traite"
say "toute connexion comme une demande du palier maximal."
