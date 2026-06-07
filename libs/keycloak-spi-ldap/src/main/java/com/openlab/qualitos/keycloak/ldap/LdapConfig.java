package com.openlab.qualitos.keycloak.ldap;

import org.keycloak.component.ComponentModel;

import java.util.Objects;

/**
 * Vue typée et validée de la configuration d'une instance du provider, lue depuis
 * le {@link ComponentModel} Keycloak (les valeurs saisies dans l'onglet User
 * Federation du realm).
 *
 * <p>Tous les noms d'attributs (username, email, prénom, nom) sont
 * <strong>configurables</strong> afin de couvrir les conventions différentes selon
 * le type d'annuaire :
 * <ul>
 *   <li>Active Directory : {@code sAMAccountName}, {@code mail}, {@code givenName}, {@code sn}</li>
 *   <li>OpenLDAP : {@code uid}, {@code mail}, {@code givenName}, {@code sn}</li>
 *   <li>FreeIPA : {@code uid}, {@code mail}, {@code givenName}, {@code sn}</li>
 * </ul>
 *
 * <p>Le {@code bindCredential} n'est jamais journalisé ni exposé via {@link #toString()}.
 */
public final class LdapConfig {

    // Clés de configuration (doivent matcher CustomLdapStorageProviderFactory#getConfigProperties).
    public static final String CONNECTION_URL = "connectionUrl";
    public static final String BIND_DN = "bindDn";
    public static final String BIND_CREDENTIAL = "bindCredential";
    public static final String USERS_DN = "usersDn";
    public static final String USER_OBJECT_CLASSES = "userObjectClasses";
    public static final String USERNAME_ATTR = "usernameAttribute";
    public static final String EMAIL_ATTR = "emailAttribute";
    public static final String FIRST_NAME_ATTR = "firstNameAttribute";
    public static final String LAST_NAME_ATTR = "lastNameAttribute";
    public static final String UUID_ATTR = "uuidAttribute";
    public static final String SEARCH_FILTER = "customSearchFilter";
    public static final String CACHE_TTL_SECONDS = "cacheTtlSeconds";
    public static final String CONNECT_TIMEOUT_MS = "connectTimeoutMs";
    public static final String READ_TIMEOUT_MS = "readTimeoutMs";

    // Valeurs par défaut (orientées OpenLDAP, l'annuaire le plus neutre).
    private static final String DEFAULT_USER_OBJECT_CLASSES = "inetOrgPerson,organizationalPerson,person";
    private static final String DEFAULT_USERNAME_ATTR = "uid";
    private static final String DEFAULT_EMAIL_ATTR = "mail";
    private static final String DEFAULT_FIRST_NAME_ATTR = "givenName";
    private static final String DEFAULT_LAST_NAME_ATTR = "sn";
    private static final String DEFAULT_UUID_ATTR = "entryUUID";
    private static final long DEFAULT_CACHE_TTL_SECONDS = 300L;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 5000;

    private final String connectionUrl;
    private final String bindDn;
    private final String bindCredential;
    private final String usersDn;
    private final String[] userObjectClasses;
    private final String usernameAttribute;
    private final String emailAttribute;
    private final String firstNameAttribute;
    private final String lastNameAttribute;
    private final String uuidAttribute;
    private final String customSearchFilter;
    private final long cacheTtlSeconds;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    LdapConfig(String connectionUrl, String bindDn, String bindCredential, String usersDn,
               String[] userObjectClasses, String usernameAttribute, String emailAttribute,
               String firstNameAttribute, String lastNameAttribute, String uuidAttribute,
               String customSearchFilter, long cacheTtlSeconds, int connectTimeoutMs, int readTimeoutMs) {
        this.connectionUrl = connectionUrl;
        this.bindDn = bindDn;
        this.bindCredential = bindCredential;
        this.usersDn = usersDn;
        this.userObjectClasses = userObjectClasses;
        this.usernameAttribute = usernameAttribute;
        this.emailAttribute = emailAttribute;
        this.firstNameAttribute = firstNameAttribute;
        this.lastNameAttribute = lastNameAttribute;
        this.uuidAttribute = uuidAttribute;
        this.customSearchFilter = customSearchFilter;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Construit la config depuis le {@link ComponentModel} Keycloak. Applique les
     * valeurs par défaut et valide les champs obligatoires.
     *
     * @throws IllegalStateException si une valeur obligatoire est absente.
     */
    public static LdapConfig fromComponentModel(ComponentModel model) {
        Objects.requireNonNull(model, "ComponentModel must not be null");
        var cfg = model.getConfig();
        String connectionUrl = first(cfg.getFirst(CONNECTION_URL));
        String usersDn = first(cfg.getFirst(USERS_DN));
        requireNonBlank(connectionUrl, CONNECTION_URL);
        requireNonBlank(usersDn, USERS_DN);

        return new LdapConfig(
                connectionUrl.trim(),
                trimToNull(cfg.getFirst(BIND_DN)),
                cfg.getFirst(BIND_CREDENTIAL), // jamais trim/log : secret
                usersDn.trim(),
                splitObjectClasses(orDefault(cfg.getFirst(USER_OBJECT_CLASSES), DEFAULT_USER_OBJECT_CLASSES)),
                orDefault(cfg.getFirst(USERNAME_ATTR), DEFAULT_USERNAME_ATTR),
                orDefault(cfg.getFirst(EMAIL_ATTR), DEFAULT_EMAIL_ATTR),
                orDefault(cfg.getFirst(FIRST_NAME_ATTR), DEFAULT_FIRST_NAME_ATTR),
                orDefault(cfg.getFirst(LAST_NAME_ATTR), DEFAULT_LAST_NAME_ATTR),
                orDefault(cfg.getFirst(UUID_ATTR), DEFAULT_UUID_ATTR),
                trimToNull(cfg.getFirst(SEARCH_FILTER)),
                parseLong(cfg.getFirst(CACHE_TTL_SECONDS), DEFAULT_CACHE_TTL_SECONDS),
                (int) parseLong(cfg.getFirst(CONNECT_TIMEOUT_MS), DEFAULT_CONNECT_TIMEOUT_MS),
                (int) parseLong(cfg.getFirst(READ_TIMEOUT_MS), DEFAULT_READ_TIMEOUT_MS)
        );
    }

    private static String[] splitObjectClasses(String raw) {
        String[] parts = raw.split(",");
        java.util.List<String> out = new java.util.ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out.toArray(new String[0]);
    }

    private static String orDefault(String value, String fallback) {
        String t = trimToNull(value);
        return t == null ? fallback : t;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private static String first(String value) {
        return value == null ? "" : value;
    }

    private static long parseLong(String value, long fallback) {
        String t = trimToNull(value);
        if (t == null) {
            return fallback;
        }
        try {
            long parsed = Long.parseLong(t);
            return parsed < 0 ? fallback : parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static void requireNonBlank(String value, String key) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Configuration LDAP invalide : '" + key + "' est obligatoire");
        }
    }

    /**
     * Construit le filtre de recherche LDAP pour un attribut donné.
     * Si un filtre personnalisé est fourni, il est combiné en AND avec la clause
     * d'égalité (le filtre custom restreint le périmètre, p.ex. {@code (memberOf=...)}).
     *
     * @param attribute nom de l'attribut LDAP (déjà résolu : usernameAttribute, etc.)
     * @param value     valeur recherchée (sera échappée)
     */
    public String buildFilter(String attribute, String value) {
        String escaped = LdapEscaper.escapeFilterValue(value);
        String base = "(" + attribute + "=" + escaped + ")";
        if (customSearchFilter == null) {
            return base;
        }
        String custom = customSearchFilter.startsWith("(") ? customSearchFilter : "(" + customSearchFilter + ")";
        return "(&" + base + custom + ")";
    }

    public String connectionUrl() { return connectionUrl; }
    public String bindDn() { return bindDn; }
    public String bindCredential() { return bindCredential; }
    public String usersDn() { return usersDn; }
    public String[] userObjectClasses() { return userObjectClasses.clone(); }
    public String usernameAttribute() { return usernameAttribute; }
    public String emailAttribute() { return emailAttribute; }
    public String firstNameAttribute() { return firstNameAttribute; }
    public String lastNameAttribute() { return lastNameAttribute; }
    public String uuidAttribute() { return uuidAttribute; }
    public String customSearchFilter() { return customSearchFilter; }
    public long cacheTtlSeconds() { return cacheTtlSeconds; }
    public int connectTimeoutMs() { return connectTimeoutMs; }
    public int readTimeoutMs() { return readTimeoutMs; }

    /** Le bindCredential est volontairement masqué pour ne jamais fuiter dans les logs. */
    @Override
    public String toString() {
        return "LdapConfig{connectionUrl=" + connectionUrl
                + ", bindDn=" + bindDn
                + ", bindCredential=" + (bindCredential == null || bindCredential.isEmpty() ? "<none>" : "***")
                + ", usersDn=" + usersDn
                + ", userObjectClasses=" + java.util.Arrays.toString(userObjectClasses)
                + ", usernameAttribute=" + usernameAttribute
                + ", emailAttribute=" + emailAttribute
                + ", cacheTtlSeconds=" + cacheTtlSeconds
                + ", connectTimeoutMs=" + connectTimeoutMs
                + ", readTimeoutMs=" + readTimeoutMs + '}';
    }
}
