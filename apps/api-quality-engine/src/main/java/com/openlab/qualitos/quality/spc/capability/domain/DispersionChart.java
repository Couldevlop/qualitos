package com.openlab.qualitos.quality.spc.capability.domain;

/**
 * La carte qui mesure la dispersion d'un sous-groupe, dans le couple
 * « carte des moyennes + carte de dispersion » d'une étude de capabilité.
 *
 * <p>Le choix n'est pas une préférence, c'est l'étendue du domaine de validité
 * des constantes :
 * <ul>
 *   <li>{@link #RANGE} — carte X̄ – R, la dispersion est l'étendue R = max − min.
 *       L'étendue n'utilise que DEUX des {@code n} mesures : au-delà d'une
 *       dizaine d'échantillons par sous-groupe, elle jette l'information du
 *       milieu et estime σ de moins en moins bien (efficacité relative qui
 *       s'effondre). C'est pourquoi les tables publiées de d2, A2, D3 et D4
 *       s'arrêtent là où leur usage cesse d'être recommandé.</li>
 *   <li>{@link #STDEV} — carte X̄ – S, la dispersion est l'écart-type
 *       d'échantillon s (diviseur {@code n − 1}). Il utilise TOUTES les mesures
 *       et reste efficace quand le sous-groupe grossit.</li>
 * </ul>
 *
 * <p>Voir {@code docs/adr/0105-les-cartes-de-controle-acceptent-toute-taille-d-echantillon.md}.
 */
public enum DispersionChart {

    /** Carte X̄ – R : σ̂ = R̄ / d2, limites X̿ ± A2 × R̄, carte R entre D3 × R̄ et D4 × R̄. */
    RANGE,

    /** Carte X̄ – S : σ̂ = s̄ / c4, limites X̿ ± A3 × s̄, carte S entre B3 × s̄ et B4 × s̄. */
    STDEV
}
