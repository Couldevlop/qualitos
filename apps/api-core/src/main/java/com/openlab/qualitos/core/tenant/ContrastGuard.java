package com.openlab.qualitos.core.tenant;

import java.util.regex.Pattern;

/**
 * Mesure le contraste de deux couleurs, et refuse celles qu'on ne pourrait pas
 * lire.
 *
 * <p>Un client choisit sa couleur d'accent à la création de son espace. Ce
 * choix ne regarde pas que lui : la couleur habille les boutons, les pastilles
 * et les liens de tous ses utilisateurs, et une couleur trop pâle rend
 * l'application inutilisable pour les personnes malvoyantes, sans qu'aucun
 * écran ne permette de revenir en arrière. Le contraste se vérifie donc ici,
 * au serveur, avant l'enregistrement — un contrôle côté écran serait une
 * politesse, pas une garantie.
 *
 * <p>Formule : luminance relative de WCAG 2.1 (§ « relative luminance »), puis
 * rapport {@code (L_clair + 0,05) / (L_sombre + 0,05)}. Le seuil est 4,5, celui
 * du niveau AA pour du texte de taille normale, conformément à
 * {@code CLAUDE.md} §15.1 (accessibilité WCAG 2.2 AA).
 */
public final class ContrastGuard {

    /** Seuil du niveau AA pour du texte de taille normale (WCAG 2.2, SC 1.4.3). */
    public static final double MINIMUM_RATIO = 4.5;

    /**
     * Forme longue uniquement. {@code #FFF} serait valide en CSS, mais accepter
     * deux écritures de la même couleur ferait diverger ce qui est comparé ici
     * de ce qui est stocké en base, où la colonne tient sept caractères.
     */
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private ContrastGuard() {
    }

    /**
     * Rapport de contraste entre deux couleurs, de 1 (identiques) à 21 (noir et
     * blanc). L'ordre des arguments est sans effet.
     *
     * @throws IllegalArgumentException si une couleur n'est pas une notation
     *                                  hexadécimale à six chiffres précédée d'un dièse
     */
    public static double ratio(String colorA, String colorB) {
        double a = relativeLuminance(colorA);
        double b = relativeLuminance(colorB);
        double lighter = Math.max(a, b);
        double darker = Math.min(a, b);
        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Laisse passer une paire lisible, refuse les autres.
     *
     * @throws IllegalArgumentException si le contraste est sous {@link #MINIMUM_RATIO}
     */
    public static void requireReadable(String foreground, String background) {
        double measured = ratio(foreground, background);
        if (measured < MINIMUM_RATIO) {
            throw new IllegalArgumentException(
                    "contraste insuffisant entre %s et %s : %.2f pour un minimum de %.1f (WCAG 2.2 AA)"
                            .formatted(foreground, background, measured, MINIMUM_RATIO));
        }
    }

    private static double relativeLuminance(String color) {
        if (color == null || !HEX.matcher(color).matches()) {
            throw new IllegalArgumentException(
                    "couleur attendue en notation hexadécimale à six chiffres (#RRGGBB), reçu : " + color);
        }
        double r = linearize(Integer.parseInt(color.substring(1, 3), 16));
        double g = linearize(Integer.parseInt(color.substring(3, 5), 16));
        double b = linearize(Integer.parseInt(color.substring(5, 7), 16));
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    /** Passage du canal 0-255 à sa valeur linéaire, seuil et exposant de WCAG 2.1. */
    private static double linearize(int channel) {
        double c = channel / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
