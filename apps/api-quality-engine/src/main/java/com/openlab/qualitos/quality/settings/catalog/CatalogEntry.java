package com.openlab.qualitos.quality.settings.catalog;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Une entrée de liste déroulante : le code que la donnée porte, et le mot que
 * l'utilisateur lit.
 *
 * <p>Le <b>code</b> est l'identité durable. Il est enregistré sur les écarts,
 * les audits, les fournisseurs, et ne doit jamais changer de sens : retirer une
 * entrée du catalogue ne reclasse pas l'historique, qui garde son code et
 * l'affiche tel quel. Le <b>libellé</b>, lui, appartient au client : il dit
 * « Bloquant » là où la plateforme dit « CRITICAL », sans rien déplacer.
 *
 * @param code   identifiant stable, en majuscules ({@code INTERNAL}, {@code ZONE-A1})
 * @param label  ce que l'utilisateur lit, dans la langue du client
 * @param active faux pour une entrée retirée des nouvelles saisies mais conservée
 *               pour l'historique
 */
public record CatalogEntry(String code, String label, boolean active) {

    public static final int CODE_MAX = 40;
    public static final int LABEL_MAX = 120;

    /**
     * Majuscules, chiffres, tiret et souligné. Pas d'espace ni d'accent : un code
     * voyage dans des URL, des exports et des fichiers de reprise, et un code
     * accentué finit par exister en deux versions qui ne se rejoignent plus.
     */
    private static final Pattern CODE = Pattern.compile("[A-Z0-9][A-Z0-9_-]{0,39}");

    public CatalogEntry {
        if (code == null || code.isBlank()) {
            throw new CatalogValidationException("An entry needs a code");
        }
        if (label == null || label.isBlank()) {
            throw new CatalogValidationException("An entry needs a label");
        }
        code = code.strip().toUpperCase(Locale.ROOT);
        label = label.strip();
        if (code.length() > CODE_MAX) {
            throw new CatalogValidationException("An entry code is at most " + CODE_MAX + " characters");
        }
        if (!CODE.matcher(code).matches()) {
            throw new CatalogValidationException(
                    "Invalid entry code '" + code + "': use capitals, digits, hyphen or underscore");
        }
        if (label.length() > LABEL_MAX) {
            throw new CatalogValidationException("An entry label is at most " + LABEL_MAX + " characters");
        }
    }

    /** Une entrée active, forme de loin la plus courante. */
    public static CatalogEntry of(String code, String label) {
        return new CatalogEntry(code, label, true);
    }
}
