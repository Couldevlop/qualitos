package com.openlab.qualitos.keycloak.ldap;

import java.util.Objects;

/**
 * Représentation immuable et neutre d'une entrée utilisateur résolue depuis
 * l'annuaire, après application du mapping d'attributs configuré. Découple le
 * provider Keycloak du format JNDI/{@code javax.naming}.
 *
 * @param dn        Distinguished Name complet de l'entrée (utilisé pour le bind d'auth)
 * @param uuid      identifiant stable de l'entrée (entryUUID / objectGUID / ipaUniqueID)
 * @param username  nom d'utilisateur résolu (sAMAccountName / uid)
 * @param email     adresse e-mail (nullable)
 * @param firstName prénom (nullable)
 * @param lastName  nom (nullable)
 */
public record LdapUserEntry(
        String dn,
        String uuid,
        String username,
        String email,
        String firstName,
        String lastName) {

    public LdapUserEntry {
        Objects.requireNonNull(dn, "dn must not be null");
        Objects.requireNonNull(username, "username must not be null");
        // uuid peut être absent sur certains annuaires : on retombe sur le DN.
        if (uuid == null || uuid.isBlank()) {
            uuid = dn;
        }
    }
}
