package com.openlab.qualitos.keycloak.ldap;

/**
 * Échappement des valeurs injectées dans un filtre de recherche LDAP (RFC 4515)
 * pour empêcher les injections de filtre LDAP (OWASP A03 — Injection).
 *
 * <p>Chaque caractère spécial est remplacé par sa séquence {@code \XX} hexadécimale.
 */
final class LdapEscaper {

    /**
     * Nom d'attribut LDAP accepté : descripteur (RFC 4512 {@code descr} : une
     * lettre puis lettres, chiffres et tirets), ou OID numérique, chacun suivi
     * d'options {@code ;lang-fr} facultatives.
     *
     * <p>Un nom d'attribut ne peut PAS être échappé comme une valeur : la RFC 4515
     * ne prévoit d'échappement que pour l'{@code assertionValue}. La seule défense
     * est donc de refuser tout ce qui n'est pas un nom d'attribut.
     */
    private static final java.util.regex.Pattern ATTRIBUTE_DESCRIPTION =
            java.util.regex.Pattern.compile(
                    "^(?:[A-Za-z][A-Za-z0-9-]*|[0-9]+(?:[.][0-9]+)+)(?:;[A-Za-z0-9-]+)*$");

    private LdapEscaper() {
    }

    /**
     * Valide un nom d'attribut destiné à l'{@code attributeDescription} d'un filtre.
     *
     * @param attribute nom brut
     * @return le nom tel quel, s'il est un nom d'attribut LDAP
     * @throws IllegalArgumentException si ce n'en est pas un — ce qui permettrait
     *         de fermer la clause et d'en ouvrir une autre, p.ex. {@code cn)(uid=*}
     */
    static String requireAttributeDescription(String attribute) {
        if (attribute == null || !ATTRIBUTE_DESCRIPTION.matcher(attribute).matches()) {
            throw new IllegalArgumentException("nom d'attribut LDAP refusé");
        }
        return attribute;
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
