package com.openlab.qualitos.quality.smi.domain;

import java.util.Collection;
import java.util.Locale;

/**
 * Ce qu'un filtre « une norme » retient de chaque source.
 *
 * <p>Aucune table de correspondance codée en dur : le code d'une norme du
 * Standards Hub ({@code iso-45001}) se lit tel quel dans les exigences du
 * registre ({@code ISO_45001_6_1}) et dans le texte libre d'un audit planifié
 * (« ISO 45001:2018 ») une fois ramené à ses lettres et chiffres. Une norme
 * ajoutée au catalogue est donc filtrable sans toucher à ce code.
 */
public final class StandardScope {

    private final String code;
    private final String requirementPrefix;
    private final String compact;

    private StandardScope(String code) {
        this.code = code;
        this.requirementPrefix = code.toUpperCase(Locale.ROOT).replace('-', '_') + "_";
        this.compact = compact(code);
    }

    /** {@code null} ou blanc : aucun filtre, toutes les normes. */
    public static StandardScope of(String code) {
        return code == null || code.isBlank() ? null : new StandardScope(code.strip().toLowerCase(Locale.ROOT));
    }

    public String code() {
        return code;
    }

    /** Un risque relève de la norme s'il en couvre au moins une exigence. */
    public boolean coversAny(Collection<String> requirementCodes) {
        return requirementCodes != null
                && requirementCodes.stream().anyMatch(r -> r != null && r.startsWith(requirementPrefix));
    }

    /** Un audit relève de la norme si son référentiel la nomme : « ISO 9001:2015 » nomme {@code iso-9001}. */
    public boolean namedIn(String freeText) {
        return freeText != null && compact(freeText).contains(compact);
    }

    private static String compact(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
