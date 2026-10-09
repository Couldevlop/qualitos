#!/usr/bin/env bash
# Bancs des scripts d'installation on-premise (ADR 0083). Bash pur, sans
# dépendance : un faux `kubectl` simule le cluster.
#
#   bash infra/onprem/tests/run-tests.sh
#
# Couvre : lecture de qualitos.conf (sans exécution de ce qu'il contient),
# images vers un registre miroir, lecture de la licence, valeurs du site,
# composants d'annuaire, rendu du realm on-premise, vérification des prérequis.

set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ONPREM="$(cd "$HERE/.." && pwd)"
ROOT="$(cd "$ONPREM/../.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

PASS=0
FAIL=0
check() {
  local name="$1"; shift
  if "$@"; then PASS=$((PASS + 1)); printf '  ok   %s\n' "$name"
  else FAIL=$((FAIL + 1)); printf '  ÉCHEC %s\n' "$name" >&2; fi
}
eq() { [ "$1" = "$2" ] || { printf '    attendu « %s », obtenu « %s »\n' "$2" "$1" >&2; return 1; }; }
contains() { case "$1" in *"$2"*) return 0 ;; *) printf '    « %s » absent de :\n%s\n' "$2" "$1" >&2; return 1 ;; esac; }
lacks() { case "$1" in *"$2"*) printf '    « %s » ne devrait pas apparaître\n' "$2" >&2; return 1 ;; *) return 0 ;; esac; }

PY="$(command -v python3 || command -v python)"

# shellcheck source=../lib.sh
. "$ONPREM/lib.sh"

# ---------------------------------------------------------------------------
echo "qualitos.conf"
cat > "$WORK/site.conf" <<'CONF'
# commentaire
QOS_HOST=qualitos.acme.local
QOS_NAMESPACE="qualitos"
QOS_ADMIN_EMAIL='dsi@acme.local'
QOS_REGISTRY=registry.acme.local/qualitos
QOS_STORAGE_CLASS=depuis-le-fichier
QOS_INJECTION=$(touch PWNED)
ceci n'est pas une affectation
CONF
(
  cd "$WORK" || exit 1
  export QOS_STORAGE_CLASS=depuis-l-environnement
  qos_load_conf "$WORK/site.conf" 2>"$WORK/warn.txt"
  check "lit les affectations" eq "$QOS_HOST" qualitos.acme.local
  check "retire les guillemets doubles" eq "$QOS_NAMESPACE" qualitos
  check "retire les guillemets simples" eq "$QOS_ADMIN_EMAIL" dsi@acme.local
  check "l'environnement l'emporte sur le fichier" eq "$QOS_STORAGE_CLASS" depuis-l-environnement
  check "n'exécute rien de ce que contient le fichier" test ! -e "$WORK/PWNED"
  check "garde la valeur littérale" eq "$QOS_INJECTION" '$(touch PWNED)'
  check "signale les lignes ignorées" contains "$(cat "$WORK/warn.txt")" "ceci n'est pas"
  export QOS_A=x; unset QOS_B
  check "qos_require accepte une variable posée" qos_require QOS_A
  check "qos_require refuse une variable vide" bash -c ". '$ONPREM/lib.sh'; qos_require QOS_B 2>/dev/null; [ \$? -ne 0 ]"
)
check "un fichier absent est refusé" bash -c ". '$ONPREM/lib.sh'; ! qos_load_conf '$WORK/absent.conf' 2>/dev/null"

# ---------------------------------------------------------------------------
echo "registre miroir"
check "image officielle → library/"        eq "$(qos_mirror_ref postgres:17-alpine reg.local)" reg.local/library/postgres:17-alpine
check "image d'un registre tiers"          eq "$(qos_mirror_ref quay.io/keycloak/keycloak:25.0 reg.local/)" reg.local/keycloak/keycloak:25.0
check "image d'organisation Docker Hub"    eq "$(qos_mirror_ref minio/minio:RELEASE.2025 reg.local)" reg.local/minio/minio:RELEASE.2025
check "registre avec port"                 eq "$(qos_mirror_ref localhost:5000/a/b:1 reg.local)" reg.local/a/b:1
check "sans miroir, rien ne change"        eq "$(qos_mirror_ref qdrant/qdrant:v1.11.0 '')" qdrant/qdrant:v1.11.0
MANIFEST="$(printf '      containers:\n        - name: pg\n          image: postgres:17-alpine # commentaire\n      initContainers:\n        - image: busybox:1.37\n' | qos_mirror_images reg.local)"
check "réécrit les lignes image:"          contains "$MANIFEST" "image: reg.local/library/postgres:17-alpine # commentaire"
check "réécrit les images en liste"        contains "$MANIFEST" "- image: reg.local/library/busybox:1.37"
check "laisse le reste intact"             contains "$MANIFEST" "- name: pg"

# ---------------------------------------------------------------------------
echo "licence"
TENANT=0d6e7f80-91a2-4b3c-8d4e-5f6a7b8c9d0e
PAYLOAD="$(printf '{"version":1,"licenseId":"LIC-1","customer":"Hôpital Saint-Louis","tenantId":"%s","tier":"PRO"}' "$TENANT" \
  | base64 | tr -d '\n=' | tr '/+' '_-')"
printf '{\n  "format": "qualitos-license/1",\n  "payload": "%s",\n  "signature": "AAAA"\n}\n' "$PAYLOAD" > "$WORK/client.lic"
check "lit le client fixé par la licence"  eq "$(qos_license_tenant "$WORK/client.lic")" "$TENANT"
check "lit la raison sociale"              eq "$(qos_license_customer "$WORK/client.lic")" "Hôpital Saint-Louis"
printf 'pas une licence\n' > "$WORK/faux.lic"
check "refuse un fichier qui n'en est pas" bash -c ". '$ONPREM/lib.sh'; ! qos_license_tenant '$WORK/faux.lic'"

# ---------------------------------------------------------------------------
echo "valeurs du site"
SITE="$(QOS_HOST=q.acme.local QOS_REGISTRY=reg.local/qualitos QOS_TLS_MODE=secret bash "$ONPREM/site-values.sh")"
check "registre du site"                   contains "$SITE" 'imageRegistry: "reg.local/qualitos"'
check "émetteur des jetons sur l'hôte"     contains "$SITE" 'https://q.acme.local/auth/realms/qualitos'
check "Ollama du cluster par défaut"       contains "$SITE" 'OLLAMA_BASE_URL: "http://ollama:11434"'
check "seul l'hôte du modèle est autorisé" contains "$SITE" 'QOS_AI_ALLOWED_HOSTS: "ollama"'
check "aucune annotation snippet sans WAF" lacks "$SITE" "snippet:"
check "snippets désactivés par défaut"     contains "$SITE" "snippets: false"
check "snippets activés sur demande"       contains "$(QOS_HOST=h QOS_REGISTRY=r QOS_INGRESS_SNIPPETS=true bash "$ONPREM/site-values.sh")" "snippets: true"
check "pas d'émetteur en mode secret"      lacks "$SITE" "cluster-issuer"
SITE2="$(QOS_HOST=q.acme.local QOS_REGISTRY=r QOS_TLS_MODE=issuer QOS_CLUSTER_ISSUER=pki QOS_PULL_SECRET=acces \
  QOS_WAF=true QOS_OLLAMA_URL=https://ia.acme.local:11434 bash "$ONPREM/site-values.sh")"
check "émetteur de certificats du site"    contains "$SITE2" 'cert-manager.io/cluster-issuer: "pki"'
check "secret d'accès au registre"         contains "$SITE2" 'name: "acces"'
check "WAF et ses exclusions"              contains "$SITE2" "SecRuleRemoveById 911100"
check "le WAF suppose les snippets permis"  contains "$SITE2" "snippets: true"
check "Ollama du site"                     contains "$SITE2" 'QOS_AI_ALLOWED_HOSTS: "ia.acme.local"'
check "émetteur requis en mode issuer"     bash -c "! QOS_HOST=h QOS_REGISTRY=r QOS_TLS_MODE=issuer bash '$ONPREM/site-values.sh' >/dev/null 2>&1"
check "mode TLS inconnu refusé"            bash -c "! QOS_HOST=h QOS_REGISTRY=r QOS_TLS_MODE=auto bash '$ONPREM/site-values.sh' >/dev/null 2>&1"

# ---------------------------------------------------------------------------
echo "annuaire"
AVEC_GUILLEMET='a"b'
LDAP="$(QOS_LDAP_URL=ldaps://ad.acme.local QOS_LDAP_USERS_DN=OU=Staff,DC=acme QOS_LDAP_BIND_DN=CN=svc QOS_LDAP_BIND_PASSWORD="$AVEC_GUILLEMET" \
  REALM_ID=r1 "$PY" "$ONPREM/ldap-component.py" provider)"
check "fournisseur Active Directory"       contains "$LDAP" '"usernameLDAPAttribute": ["sAMAccountName"]'
check "annuaire en lecture seule"          contains "$LDAP" '"editMode": ["READ_ONLY"]'
check "valeur correctement échappée"       contains "$LDAP" '"bindCredential": ["a\"b"]'
LDAP2="$(QOS_LDAP_VENDOR=other QOS_LDAP_URL=ldap://x QOS_LDAP_USERS_DN=ou=p REALM_ID=r "$PY" "$ONPREM/ldap-component.py" provider)"
check "fournisseur OpenLDAP"               contains "$LDAP2" '"uuidLDAPAttribute": ["entryUUID"]'
check "liaison anonyme sans mot de passe"  lacks "$LDAP2" bindCredential
check "réglages manquants refusés"         bash -c "! QOS_LDAP_URL=ldap://x '$PY' '$ONPREM/ldap-component.py' provider >/dev/null 2>&1"
check "mappeur du client"                  contains "$(LDAP_ID=l TENANT_ID="$TENANT" "$PY" "$ONPREM/ldap-component.py" tenant)" "\"attribute.value\": [\"$TENANT\"]"
check "mappeur du rôle user"               contains "$(LDAP_ID=l "$PY" "$ONPREM/ldap-component.py" role)" '"role": ["user"]'

# ---------------------------------------------------------------------------
# Un faux kubectl : le cluster du banc. Sa conduite se règle par FAKE_*.
mkdir -p "$WORK/bin"
cat > "$WORK/bin/kubectl" <<'KUBECTL'
#!/usr/bin/env bash
case "$*" in
  "get nodes") exit "${FAKE_NODES:-0}" ;;
  "get nodes --no-headers") echo "noeud-1 Ready" ;;
  *"allocatable.memory"*) echo "${FAKE_MEM_KI:-33554432}Ki" ;;
  "get ingressclass"*) exit "${FAKE_INGRESS:-0}" ;;
  "get storageclass"*) exit "${FAKE_STORAGE:-0}" ;;
  *"get secret qualitos-realm-accounts"*) exit 1 ;;
  *"get secret"*) exit "${FAKE_SECRET:-0}" ;;
  *"create configmap"*) for a in "$@"; do case "$a" in --from-file=*) cp "${a#*=*=}" "$FAKE_OUT/realm.json" ;; esac; done ;;
  *"apply -f -"*) cat >> "$FAKE_OUT/applied.yaml" ;;
  *"get configmap"*) exit 1 ;;
  *"get pod"*) echo "" ;;
  *"create secret generic qualitos-license"*) echo "license-secret" >> "$FAKE_OUT/actions.txt"; echo "kind: Secret" ;;
  *"create secret"*) echo "kind: Secret" ;;
esac
exit 0
KUBECTL
printf '#!/usr/bin/env bash\nexec "%s" "$@"\n' "$PY" > "$WORK/bin/python3"
for t in openssl curl; do printf '#!/usr/bin/env bash\nexit 0\n' > "$WORK/bin/$t"; done
cat > "$WORK/bin/helm" <<'HELM'
#!/usr/bin/env bash
case "$1" in
  status) exit 1 ;;
  upgrade)
    echo "$*" > "$FAKE_OUT/helm-args.txt"
    prev=""
    for a in "$@"; do
      [ "$prev" = "--values" ] && cat "$a" >> "$FAKE_OUT/helm-values.yaml"
      prev="$a"
    done ;;
esac
exit 0
HELM
chmod +x "$WORK/bin/"*
export FAKE_OUT="$WORK"

echo "realm on-premise"
REALM_OUT="$(PATH="$WORK/bin:$PATH" PYTHONIOENCODING=utf-8 QOS_HOST=q.acme.local QOS_EDITION=onprem \
  QOS_TENANT_ID="$TENANT" QOS_ADMIN_EMAIL=dsi@acme.local bash "$ROOT/infra/k8s/deps/render-realm.sh" qualitos 2>&1)"
check "rendu sans erreur" test -f "$WORK/realm.json"
REALM_CHECK="$("$PY" - "$WORK/realm.json" "$TENANT" <<'PYCHK'
import io, json, sys
r = json.load(io.open(sys.argv[1], encoding="utf-8"))
humains = [u for u in r["users"] if not u.get("serviceAccountClientId")]
assert [u["username"] for u in humains] == ["admin"], humains
a = humains[0]
assert a["attributes"]["tenant_id"] == [sys.argv[2]]
assert a["realmRoles"] == ["admin_tenant"]
assert a["credentials"][0]["temporary"] is True
assert set(a["requiredActions"]) == {"CONFIGURE_TOTP", "UPDATE_PASSWORD"}
web = [c for c in r["clients"] if c["clientId"] == "qualitos-web"][0]
assert web["redirectUris"] == ["https://q.acme.local/*"]
print("ok")
PYCHK
)"
check "un seul administrateur, rattaché au client, mot de passe provisoire" eq "$REALM_CHECK" ok
check "aucun compte de démonstration annoncé" lacks "$REALM_OUT" "demo"
check "client requis en on-premise" bash -c "! PATH='$WORK/bin:$PATH' QOS_HOST=h QOS_EDITION=onprem bash '$ROOT/infra/k8s/deps/render-realm.sh' ns >/dev/null 2>&1"

# ---------------------------------------------------------------------------
echo "vérification des prérequis"
cp "$WORK/client.lic" "$WORK/licence.lic"
cat > "$WORK/ok.conf" <<CONF
QOS_HOST=localhost
QOS_NAMESPACE=qualitos
QOS_ADMIN_EMAIL=dsi@acme.local
QOS_LICENSE_FILE=./licence.lic
QOS_REGISTRY=reg.local/qualitos
QOS_TLS_MODE=secret
CONF
verifier() { PATH="$WORK/bin:$PATH" bash "$ONPREM/install.sh" verifier "$1" 2>&1; }
OUT="$(verifier "$WORK/ok.conf")"; RC=$?
check "site complet : prérequis réunis" eq "$RC" 0
check "le client de la licence est annoncé" contains "$OUT" "Hôpital Saint-Louis"
OUT="$(FAKE_INGRESS=1 FAKE_SECRET=1 verifier "$WORK/ok.conf")"; RC=$?
check "prérequis manquants : arrêt" eq "$RC" 1
check "dit quelle classe d'ingress manque" contains "$OUT" "classe d'ingress nginx absente"
check "dit comment poser le certificat" contains "$OUT" "create secret tls"
sed 's/^QOS_ADMIN_EMAIL=.*/QOS_ADMIN_EMAIL=pas-une-adresse/' "$WORK/ok.conf" > "$WORK/bad.conf"
OUT="$(verifier "$WORK/bad.conf")"; RC=$?
check "adresse e-mail invalide refusée" contains "$OUT" "n'est pas une adresse"
printf 'QOS_LDAP_URL=ad.acme.local\n' >> "$WORK/ok.conf"
OUT="$(verifier "$WORK/ok.conf")"
check "annuaire incomplet signalé" contains "$OUT" "QOS_LDAP_USERS_DN requis"
check "schéma d'annuaire refusé" contains "$OUT" "doit commencer par ldap://"

# ---------------------------------------------------------------------------
echo "déploiement on-premise de bout en bout (faux cluster)"
rm -f "$WORK/applied.yaml" "$WORK/helm-args.txt" "$WORK/helm-values.yaml" "$WORK/actions.txt" "$WORK/realm.json"
cat > "$WORK/deploy.conf" <<CONF
QOS_HOST=q.acme.local
QOS_NAMESPACE=client-qualite
QOS_ADMIN_EMAIL=dsi@acme.local
QOS_LICENSE_FILE=./licence.lic
QOS_REGISTRY=reg.acme.local/qualitos
QOS_MIRROR_REGISTRY=reg.acme.local/miroir
QOS_STORAGE_CLASS=ceph-rbd
QOS_TLS_MODE=secret
QOS_OLLAMA_MODEL=mistral:7b
CONF
DEPLOY_OUT="$(PATH="$WORK/bin:$PATH" PYTHONIOENCODING=utf-8 bash "$ROOT/infra/k8s/deploy.sh" onprem v1.2.3 "$WORK/deploy.conf" 2>&1)"; RC=$?
check "le déploiement va au bout" eq "$RC" 0
[ "$RC" = 0 ] || printf '%s\n' "$DEPLOY_OUT" | tail -15 >&2
APPLIED="$(cat "$WORK/applied.yaml" 2>/dev/null)"
check "namespace du site"                  contains "$APPLIED" "name: client-qualite"
check "images tierces depuis le miroir"    contains "$APPLIED" "image: reg.acme.local/miroir/library/postgres:17-alpine"
check "Keycloak depuis le miroir"          contains "$APPLIED" "image: reg.acme.local/miroir/keycloak/keycloak:"
check "aucune image publique restante"     lacks "$APPLIED" "image: postgres:"
check "classe de stockage du site"         contains "$APPLIED" "storageClassName: ceph-rbd"
check "plus de local-path"                 lacks "$APPLIED" "storageClassName: local-path"
check "Ollama dans le cluster"             contains "$APPLIED" "name: ollama-models"
check "modèle du site"                     contains "$APPLIED" "ollama pull 'mistral:7b'"
check "pas l'Ollama de l'hôte SaaS"        lacks "$APPLIED" "10.42.0.1"
check "hôte de Keycloak"                   contains "$APPLIED" "https://q.acme.local/auth"
check "licence posée en secret"            contains "$(cat "$WORK/actions.txt" 2>/dev/null)" "license-secret"
check "realm on-premise rendu"             test -f "$WORK/realm.json"
HELM_ARGS="$(cat "$WORK/helm-args.txt" 2>/dev/null)"
check "release « qualitos » au tag demandé" contains "$HELM_ARGS" "upgrade --install qualitos "
check "tag sans le v"                      contains "$HELM_ARGS" "global.imageTag=1.2.3"
check "valeurs on-premise"                 contains "$HELM_ARGS" "values-onprem.yaml"
HELM_VALUES="$(cat "$WORK/helm-values.yaml" 2>/dev/null)"
check "valeurs du site transmises"         contains "$HELM_VALUES" 'imageRegistry: "reg.acme.local/qualitos"'
check "édition on-premise"                 contains "$HELM_VALUES" "QUALITOS_EDITION: onprem"
check "message final : où se connecter"    contains "$DEPLOY_OUT" "https://q.acme.local"
check "pas de promotion en production"     lacks "$DEPLOY_OUT" "Promotion en production"

# ---------------------------------------------------------------------------
echo "préproduction SaaS : rien ne change"
rm -f "$WORK/applied.yaml" "$WORK/helm-args.txt" "$WORK/helm-values.yaml" "$WORK/actions.txt"
SAAS_OUT="$(env -u QOS_STORAGE_CLASS -u QOS_MIRROR_REGISTRY PATH="$WORK/bin:$PATH" PYTHONIOENCODING=utf-8 \
  bash "$ROOT/infra/k8s/deploy.sh" preprod v1.2.3 2>&1)"; RC=$?
check "la préproduction se déploie"        eq "$RC" 0
APPLIED="$(cat "$WORK/applied.yaml" 2>/dev/null)"
check "images publiques, sans miroir"      contains "$APPLIED" "image: postgres:17-alpine"
check "stockage local-path"                contains "$APPLIED" "storageClassName: local-path"
check "Ollama de l'hôte"                   contains "$APPLIED" "10.42.0.1"
check "pas d'Ollama dans le cluster"       lacks "$APPLIED" "name: ollama-models"
check "pas de licence"                     lacks "$(cat "$WORK/actions.txt" 2>/dev/null)" "license-secret"
HELM_ARGS="$(cat "$WORK/helm-args.txt" 2>/dev/null)"
check "release qualitos-preprod"           contains "$HELM_ARGS" "upgrade --install qualitos-preprod "
check "valeurs de préproduction seules"    contains "$HELM_ARGS" "values-preprod.yaml"
check "pas de valeurs on-premise"          lacks "$HELM_ARGS" "values-onprem"
check "promotion en production annoncée"   contains "$SAAS_OUT" "Promotion en production"

echo
echo "$PASS réussi(s), $FAIL échec(s)"
[ "$FAIL" -eq 0 ]
