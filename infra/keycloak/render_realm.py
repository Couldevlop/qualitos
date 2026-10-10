#!/usr/bin/env python3
"""Rend le realm Keycloak d'un environnement exposé (ADR 0083).

    render_realm.py <source> <sortie> <hôte> <mdp superadmin> <mdp admin> <édition> <client> <e-mail admin>

Partagé par infra/k8s/deps/render-realm.sh (Kubernetes) et le mode léger
docker-compose : une seule règle pour les deux, sans quoi elles divergeraient.
"""
import io, json, sys

src, out, host, superadmin_pwd, admin_pwd, edition, tenant_id, admin_email = sys.argv[1:9]
realm = json.load(io.open(src, encoding="utf-8"))

origin = "https://%s" % host

# Keycloak borne nom et description d'un client à 255 caractères : au-delà,
# l'import échoue et Keycloak ne démarre pas sur une installation neuve. On le
# dit ici, avant le déploiement, plutôt qu'au démarrage chez le client.
for client in realm.get("clients") or []:
    for field in ("name", "description"):
        if len(client.get(field) or "") > 255:
            sys.exit("realm : %s du client %s dépasse 255 caractères (%d)"
                     % (field, client.get("clientId"), len(client[field])))

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
