package com.openlab.qualitos.quality.training.competencymatrix.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Le code catalogue d'une compétence créée depuis la matrice.
 *
 * <p>Le catalogue exige un code unique par client ({@code ^[a-z0-9][a-z0-9_-]{1,99}$}),
 * que l'atelier n'a pas à inventer : on le tire de l'intitulé — sans accents, en
 * minuscules, mots reliés par des tirets. « Sécurité / habilitations » devient
 * {@code securite-habilitations}. Un intitulé qui ne laisse aucun caractère
 * latin (écrit en japonais, par exemple) donne {@value #FALLBACK}.
 */
public final class SkillCode {

    static final String FALLBACK = "competence";
    /** Assez court pour qu'un suffixe de désambiguïsation tienne dans les 100 caractères. */
    static final int MAX_BASE = 80;

    private SkillCode() {}

    public static String from(String name) {
        String sansAccents = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = sansAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_BASE) {
            slug = slug.substring(0, MAX_BASE).replaceAll("-+$", "");
        }
        return slug.length() < 2 ? FALLBACK : slug;
    }

    /** Le n-ième candidat : le code lui-même, puis {@code code-2}, {@code code-3}… */
    public static String candidate(String base, int attempt) {
        return attempt <= 1 ? base : base + "-" + attempt;
    }
}
