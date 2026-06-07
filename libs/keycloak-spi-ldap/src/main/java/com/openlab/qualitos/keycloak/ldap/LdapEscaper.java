package com.openlab.qualitos.keycloak.ldap;

/**
 * Échappement des valeurs injectées dans un filtre de recherche LDAP (RFC 4515)
 * pour empêcher les injections de filtre LDAP (OWASP A03 — Injection).
 *
 * <p>Chaque caractère spécial est remplacé par sa séquence {@code \XX} hexadécimale.
 */
final class LdapEscaper {

    private LdapEscaper() {
    }

    /**
     * Échappe une valeur destinée à un assertionValue d'un filtre LDAP.
     *
     * @param value valeur brute (non nulle ; {@code null} est traité comme chaîne vide)
     * @return valeur échappée, sûre à concaténer dans un filtre
     */
    static String escapeFilterValue(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\5c");
                case '*' -> sb.append("\\2a");
                case '(' -> sb.append("\\28");
                case ')' -> sb.append("\\29");
                case '\0' -> sb.append("\\00");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
