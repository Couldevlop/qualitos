#!/usr/bin/env python3
"""Déclare l'attribut `tenant_id` dans le profil utilisateur du realm.

Pourquoi : depuis Keycloak 24, le profil utilisateur est DÉCLARATIF. Un
attribut qu'il ne déclare pas est « non géré » et, sans politique pour ces
attributs, Keycloak le SUPPRIME à l'écriture. Créer un membre par l'API
d'administration avec `attributes.tenant_id` rendait donc un compte sans
client : son jeton ne portait pas `tenant_id`, et l'invitation échouait.

L'attribut est déclaré éditable et visible par l'ADMINISTRATION seulement
(API d'administration, console) : jamais par l'utilisateur lui-même, qui
pourrait sinon se rattacher à un autre client depuis son compte.

Deux usages :

    user_profile.py --check  <realm-export.json>
        Vérifie que le realm versionné déclare l'attribut comme attendu
        (intégration continue). Code de sortie 1 sinon.

    user_profile.py --apply
        Pose l'attribut sur un realm EN SERVICE (l'import du realm n'a lieu
        que sur une base vide). Lit KC_URL, KC_REALM, KC_ADMIN,
        KC_ADMIN_PASSWORD. Idempotent : n'écrit que si le profil diffère.
        À joindre par un tunnel vers le service, jamais par l'URL publique :
        le WAF de l'ingress refuse les écritures d'administration.
"""
import copy
import io
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

COMPONENT = "org.keycloak.userprofile.UserProfileProvider"
CONFIG_KEY = "kc.user.profile.config"

TENANT_ATTRIBUTE = {
    "name": "tenant_id",
    "displayName": "Client (tenant)",
    "validations": {
        "pattern": {
            "pattern": "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            "error-message": "tenant_id doit être un identifiant de client (UUID).",
        }
    },
    "permissions": {"view": ["admin"], "edit": ["admin"]},
    "multivalued": False,
}

# Les quatre attributs que Keycloak déclare par défaut : un profil qui en
# perdrait un ferait disparaître le champ correspondant des comptes.
BUILT_IN = ("username", "email", "firstName", "lastName")


def merge(profile):
    """Rend (profil, modifié) : le profil avec `tenant_id` tel qu'attendu."""
    result = copy.deepcopy(profile) if profile else {"attributes": []}
    attributes = result.setdefault("attributes", [])
    for i, attribute in enumerate(attributes):
        if attribute.get("name") == TENANT_ATTRIBUTE["name"]:
            if attribute == TENANT_ATTRIBUTE:
                return result, False
            attributes[i] = copy.deepcopy(TENANT_ATTRIBUTE)
            return result, True
    attributes.append(copy.deepcopy(TENANT_ATTRIBUTE))
    return result, True


def realm_profile(realm):
    """Le profil déclaré dans un realm exporté, ou None."""
    for component in (realm.get("components") or {}).get(COMPONENT) or []:
        values = (component.get("config") or {}).get(CONFIG_KEY) or []
        if values:
            return json.loads(values[0])
    return None


def check(path):
    realm = json.load(io.open(path, encoding="utf-8"))
    profile = realm_profile(realm)
    if profile is None:
        print("%s : aucun profil utilisateur déclaré (%s)" % (path, COMPONENT))
        return 1
    names = [a.get("name") for a in profile.get("attributes") or []]
    missing = [n for n in BUILT_IN if n not in names]
    if missing:
        print("%s : profil utilisateur sans %s" % (path, ", ".join(missing)))
        return 1
    _, changed = merge(profile)
    if changed:
        print("%s : tenant_id absent ou différent de la déclaration attendue" % path)
        return 1
    print("%s : tenant_id déclaré (admin seulement)" % path)
    return 0


def _call(method, url, token=None, payload=None, form=None):
    headers = {}
    data = None
    if token:
        headers["Authorization"] = "Bearer " + token
    if payload is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(payload).encode("utf-8")
    if form is not None:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        data = urllib.parse.urlencode(form).encode("utf-8")
    request = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request) as response:
            body = response.read()
    except urllib.error.HTTPError as failure:
        # Le corps porte la raison (droit manquant, profil refusé) : sans lui,
        # un 400 sec envoie chercher au mauvais endroit.
        detail = failure.read().decode("utf-8", "replace")[:400]
        raise SystemExit("%s %s -> HTTP %s %s\n  %s" % (method, url, failure.code, failure.reason, detail))
    return json.loads(body) if body else None


def apply():
    url = os.environ.get("KC_URL", "http://localhost:8080").rstrip("/")
    realm = os.environ.get("KC_REALM", "qualitos")
    admin = os.environ.get("KC_ADMIN", "admin")
    password = os.environ.get("KC_ADMIN_PASSWORD")
    if not password:
        raise SystemExit("KC_ADMIN_PASSWORD requis")
    token = _call("POST", url + "/realms/master/protocol/openid-connect/token",
                  form={"client_id": "admin-cli", "username": admin, "password": password,
                        "grant_type": "password"})["access_token"]
    endpoint = "%s/admin/realms/%s/users/profile" % (url, realm)
    current = _call("GET", endpoint, token=token)
    wanted, changed = merge(current)
    if not changed:
        print("   profil utilisateur : tenant_id déjà déclaré, rien à faire")
        return 0
    _call("PUT", endpoint, token=token, payload=wanted)
    print("   profil utilisateur : tenant_id déclaré (admin seulement)")
    return 0


def main(argv):
    if len(argv) == 3 and argv[1] == "--check":
        return check(argv[2])
    if len(argv) == 2 and argv[1] == "--apply":
        return apply()
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
