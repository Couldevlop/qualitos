package com.openlab.qualitos.quality.nonconformity.subcategory;

import com.openlab.qualitos.quality.nonconformity.NcCategory;

/**
 * Une sous-catégorie de non-conformité : la nature précise de l'écart, sous sa
 * catégorie.
 *
 * <p>C'est une DONNÉE, pas une énumération du code. Les natures d'écart ne sont
 * pas les mêmes dans une usine, un CHU, une banque ou une exploitation agricole,
 * et les coder en dur reviendrait à inscrire un secteur dans le cœur de la
 * plateforme (§18.2 #9). Le client reçoit un catalogue de départ couvrant tous
 * les secteurs où la qualité intervient, qu'il remplace par le sien quand le
 * sien existe.
 *
 * @param code     identifiant stable, porté par les NC ({@code PRD-DIM}, {@code SEC-EIG}…)
 * @param category la catégorie dont cette nature dépend
 * @param label    ce que l'utilisateur lit
 */
public record NcSubcategory(String code, NcCategory category, String label) {

    public static final int CODE_MAX = 30;
    public static final int LABEL_MAX = 120;

    public NcSubcategory {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("a subcategory needs a code");
        }
        if (category == null) {
            throw new IllegalArgumentException("a subcategory belongs to a category");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("a subcategory needs a label");
        }
        code = code.strip();
        label = label.strip();
        if (code.length() > CODE_MAX) {
            throw new IllegalArgumentException("subcategory code is at most " + CODE_MAX + " characters");
        }
        if (label.length() > LABEL_MAX) {
            throw new IllegalArgumentException("subcategory label is at most " + LABEL_MAX + " characters");
        }
    }
}
