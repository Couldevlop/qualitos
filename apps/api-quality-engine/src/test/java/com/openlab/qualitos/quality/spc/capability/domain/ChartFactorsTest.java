package com.openlab.qualitos.quality.spc.capability.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Le choix de la carte selon la taille d'échantillon, et ce qu'il refuse. */
class ChartFactorsTest {

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10})
    @DisplayName("jusqu'à dix mesures, la carte est X̄ – R et les facteurs sont ceux du classeur")
    void jusquADixLaCarteEstR(int n) {
        ChartFactors f = ChartFactors.forSubgroupSize(n);
        SpcConstants k = SpcConstants.forSubgroupSize(n);

        assertThat(f.chart()).isEqualTo(DispersionChart.RANGE);
        assertThat(f.subgroupSize()).isEqualTo(n);
        assertThat(f.sigmaDivisor()).isEqualTo(k.d2());
        assertThat(f.meanFactor()).isEqualTo(k.a2());
        assertThat(f.dispersionLower()).isEqualTo(k.d3());
        assertThat(f.dispersionUpper()).isEqualTo(k.d4());
    }

    @ParameterizedTest
    @ValueSource(ints = {11, 12, 15, 20, 25})
    @DisplayName("au-delà de dix, la carte est X̄ – S et les facteurs sont c4, A3, B3, B4")
    void audelaDeDixLaCarteEstS(int n) {
        ChartFactors f = ChartFactors.forSubgroupSize(n);
        StdevConstants k = StdevConstants.forSubgroupSize(n);

        assertThat(f.chart()).isEqualTo(DispersionChart.STDEV);
        assertThat(f.sigmaDivisor()).isEqualTo(k.c4());
        assertThat(f.meanFactor()).isEqualTo(k.a3());
        assertThat(f.dispersionLower()).isEqualTo(k.b3());
        assertThat(f.dispersionUpper()).isEqualTo(k.b4());
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, 0, 1, 26, 40})
    @DisplayName("hors du domaine des deux cartes, la taille est refusée — aucun chiffre n'est produit")
    void horsDomaine(int n) {
        assertThatThrownBy(() -> ChartFactors.forSubgroupSize(n))
                .isInstanceOf(SpcValidationException.class)
                .extracting("field").isEqualTo("subgroupSize");
        assertThatThrownBy(() -> ChartFactors.forSubgroupSize(n))
                .hasMessageContaining("2")
                .hasMessageContaining("25");
    }

    @Test
    @DisplayName("la dispersion est l'étendue sur une carte R, l'écart-type d'échantillon sur une carte S")
    void dispersionSelonLaCarte() {
        double[] cinq = {10, 12, 11, 13, 14};
        assertThat(ChartFactors.forSubgroupSize(5).dispersionOf(cinq)).isCloseTo(4, within(1e-12));

        // Onze mesures de moyenne 20 : cinq à 21, cinq à 19, une à 20.
        // Σ(x − x̄)² = 10, donc s = √(10 / 10) = 1 exactement.
        double[] onze = {21, 21, 21, 21, 21, 19, 19, 19, 19, 19, 20};
        assertThat(ChartFactors.forSubgroupSize(11).dispersionOf(onze)).isCloseTo(1, within(1e-12));
    }

    @Test
    @DisplayName("un sous-groupe sans variation a une dispersion nulle, sur les deux cartes")
    void dispersionNulle() {
        assertThat(ChartFactors.forSubgroupSize(3).dispersionOf(new double[] {7, 7, 7})).isZero();
        double[] plats = new double[12];
        java.util.Arrays.fill(plats, 7);
        assertThat(ChartFactors.forSubgroupSize(12).dispersionOf(plats)).isZero();
    }

    @Test
    @DisplayName("les bornes publiées sont celles des deux tables")
    void bornes() {
        assertThat(ChartFactors.MIN_SUBGROUP_SIZE).isEqualTo(2);
        assertThat(ChartFactors.RANGE_CHART_MAX).isEqualTo(10);
        assertThat(ChartFactors.MAX_SUBGROUP_SIZE).isEqualTo(25);
    }
}
