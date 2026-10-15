package com.openlab.qualitos.core.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Forme de comparaison d'un texte pour la recherche : sans accents, en
 * minuscules, espaces resserrés.
 *
 * <p>Calculée côté application plutôt que par l'extension {@code unaccent} de
 * PostgreSQL : créer une extension exige des droits que le rôle des migrations
 * n'a pas partout (installation chez un client, ADR 0083), et la même forme sert
 * à la fois à la clé stockée et au terme cherché — les deux ne peuvent donc pas
 * diverger.
 */
public final class SearchText {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    /** Caractère d'échappement des motifs LIKE : ni lettre ni chiffre, rare dans un nom. */
    public static final char LIKE_ESCAPE = '!';

    private SearchText() {}

    /** « Élodie  MÜLLER » devient « elodie muller » ; {@code null} devient la chaîne vide. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutMarks = MARKS.matcher(decomposed).replaceAll("");
        return SPACES.matcher(withoutMarks.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
    }

    /**
     * Motif LIKE « contient » pour un terme déjà normalisé. Les jokers saisis par
     * l'utilisateur ({@code %}, {@code _}) sont neutralisés : chercher « 100% »
     * ne doit pas devenir « tout ce qui commence par 100 ».
     */
    public static String containsPattern(String normalizedTerm) {
        if (normalizedTerm.isEmpty()) {
            return "%";
        }
        StringBuilder sb = new StringBuilder(normalizedTerm.length() + 2).append('%');
        for (char c : normalizedTerm.toCharArray()) {
            if (c == LIKE_ESCAPE || c == '%' || c == '_') {
                sb.append(LIKE_ESCAPE);
            }
            sb.append(c);
        }
        return sb.append('%').toString();
    }
}
