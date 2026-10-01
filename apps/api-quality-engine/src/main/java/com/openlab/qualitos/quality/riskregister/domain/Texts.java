package com.openlab.qualitos.quality.riskregister.domain;

/** Les deux règles de texte du registre : obligatoire ou facultatif, borné, rogné. */
final class Texts {

    private Texts() {}

    static String required(String field, String value, int max, String missing) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            throw new RegisterValidationException(field, missing);
        }
        return bounded(field, v, max);
    }

    /** Un texte vide vaut absence : on stocke {@code null}, pas une chaîne blanche. */
    static String optional(String field, String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return bounded(field, value.strip(), max);
    }

    private static String bounded(String field, String v, int max) {
        if (v.length() > max) {
            throw new RegisterValidationException(field, "Ce champ dépasse " + max + " caractères.");
        }
        return v;
    }
}
