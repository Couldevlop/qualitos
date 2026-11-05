package com.openlab.qualitos.core.tenant;

/**
 * Les thèmes livrés, et le fond sur lequel chacun pose les couleurs du client.
 *
 * <p>Une liste fermée, volontairement. Un éditeur de jetons libre laisserait un
 * client composer un espace illisible ou hors WCAG, et multiplierait par autant
 * la surface d'écran à éprouver. Quatre jeux cohérents couvrent les besoins
 * exprimés : la densité du poste de travail, l'aération d'une salle qualité, le
 * sombre, et le contraste renforcé.
 *
 * <p>Le fond compte : c'est contre lui que se mesure la lisibilité de la couleur
 * d'accent choisie. Mesurer toujours sur blanc interdirait les thèmes sombres,
 * dont les accents sont clairs par construction.
 */
public enum TenantTheme {

    /** Clair, dense : le poste de travail d'un service qualité. */
    DENSE_LIGHT("#FFFFFF"),

    /** Clair, aéré : le défaut. */
    AIRY_LIGHT("#FFFFFF"),

    /** Sombre. */
    DARK("#11181B"),

    /** Contraste renforcé, pour les environnements difficiles (atelier, extérieur). */
    HIGH_CONTRAST("#FFFFFF");

    private final String background;

    TenantTheme(String background) {
        this.background = background;
    }

    /** Le fond du thème, en notation longue, contre lequel se mesure le contraste. */
    public String background() {
        return background;
    }

    /**
     * Lit un code de thème, et refuse ce qui n'en est pas un.
     *
     * @throws IllegalArgumentException si le code n'est pas l'un des quatre thèmes
     */
    public static TenantTheme of(String code) {
        if (code == null || code.isBlank()) {
            return AIRY_LIGHT;
        }
        for (TenantTheme theme : values()) {
            if (theme.name().equals(code)) {
                return theme;
            }
        }
        throw new IllegalArgumentException("thème inconnu : " + code);
    }
}
