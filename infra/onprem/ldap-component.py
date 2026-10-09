#!/usr/bin/env python3
"""Composants Keycloak de l'annuaire d'entreprise d'une installation on-premise (ADR 0083).

    ldap-component.py provider   -> le fournisseur LDAP / Active Directory
    ldap-component.py tenant     -> mappeur : tenant_id du client de la licence
    ldap-component.py role       -> mappeur : rôle « user » pour tout compte de l'annuaire

Le JSON est écrit sur la sortie standard, pour `kcadm.sh create|update -f -`.
Les valeurs viennent de l'environnement (qualitos.conf chargé par deploy.sh) :
le mot de passe de liaison n'apparaît sur aucune ligne de commande.
"""
import json
import os
import sys


def env(name, default=""):
    return os.environ.get(name, default).strip()


def provider():
    ad = env("QOS_LDAP_VENDOR", "ad").lower() == "ad"
    config = {
        "vendor": ["ad" if ad else "other"],
        "connectionUrl": [env("QOS_LDAP_URL")],
        "usersDn": [env("QOS_LDAP_USERS_DN")],
        "authType": ["simple"],
        "bindDn": [env("QOS_LDAP_BIND_DN")],
        # Lecture seule : QualitOS ne modifie jamais l'annuaire du client.
        "editMode": ["READ_ONLY"],
        "importEnabled": ["true"],
        "syncRegistrations": ["false"],
        "searchScope": ["2"],  # sous-arbre
        "trustEmail": ["true"],
        "pagination": ["true"],
        "usernameLDAPAttribute": ["sAMAccountName" if ad else "uid"],
        "rdnLDAPAttribute": ["cn" if ad else "uid"],
        "uuidLDAPAttribute": ["objectGUID" if ad else "entryUUID"],
        "userObjectClasses": ["person, organizationalPerson, user" if ad
                              else "inetOrgPerson, organizationalPerson"],
        # Comptes désactivés dans l'annuaire : désactivés ici aussi.
        "enabled": ["true"],
        "priority": ["0"],
        "fullSyncPeriod": ["86400"],
        "changedSyncPeriod": ["3600"],
    }
    bind_password = env("QOS_LDAP_BIND_PASSWORD")
    if bind_password:
        config["bindCredential"] = [bind_password]
    missing = [k for k in ("connectionUrl", "usersDn") if not config[k][0]]
    if missing:
        sys.exit("annuaire : réglages manquants dans qualitos.conf : " + ", ".join(missing))
    return {
        "name": "annuaire",
        "providerId": "ldap",
        "providerType": "org.keycloak.storage.UserStorageProvider",
        "parentId": env("REALM_ID"),
        "config": config,
    }


def mapper(kind):
    parent = env("LDAP_ID")
    if not parent:
        sys.exit("LDAP_ID requis")
    if kind == "tenant":
        tenant = env("TENANT_ID")
        if not tenant:
            sys.exit("TENANT_ID requis")
        return {
            "name": "qualitos-tenant",
            "providerId": "hardcoded-attribute-mapper",
            "providerType": "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
            "parentId": parent,
            "config": {"user.model.attribute": ["tenant_id"], "attribute.value": [tenant]},
        }
    return {
        "name": "qualitos-role",
        "providerId": "hardcoded-ldap-role-mapper",
        "providerType": "org.keycloak.storage.ldap.mappers.LDAPStorageMapper",
        "parentId": parent,
        "config": {"role": ["user"]},
    }


def main(argv):
    if len(argv) != 2 or argv[1] not in ("provider", "tenant", "role"):
        sys.exit("usage: ldap-component.py provider|tenant|role")
    doc = provider() if argv[1] == "provider" else mapper(argv[1])
    sys.stdout.write(json.dumps(doc, ensure_ascii=False))


if __name__ == "__main__":
    main(sys.argv)
