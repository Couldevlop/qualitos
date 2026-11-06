package com.openlab.qualitos.quality.spc.capability.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Le banc des constantes de la carte X̄ – S : les valeurs CALCULÉES sont
 * confrontées aux valeurs PUBLIÉES.
 *
 * <p>Source des valeurs attendues : D. C. Montgomery, <i>Introduction to
 * Statistical Quality Control</i>, annexe VI « Factors for Constructing
 * Variables Control Charts » (colonnes c4, A3, B3, B4), elle-même reprise de
 * l'ASTM STP-15D. La table est imprimée à trois décimales pour A3, B3 et B4, à
 * quatre pour c4 : la tolérance du banc est celle de l'impression.
 */
class StdevConstantsTest {

    /** La table publiée s'arrête à trois décimales : on ne peut pas exiger mieux d'elle. */
    private static final double TABLE = 1e-3;

    @ParameterizedTest(name = "n = {0} : c4 = {1}, A3 = {2}, B3 = {3}, B4 = {4}")
    @DisplayName("les constantes calculées sont celles de la table publiée")
    @CsvSource({
            " 2, 0.7979, 2.659, 0.000, 3.267",
            " 3, 0.8862, 1.954, 0.000, 2.568",
            " 4, 0.9213, 1.628, 0.000, 2.266",
            " 5, 0.9400, 1.427, 0.000, 2.089",
            " 6, 0.9515, 1.287, 0.030, 1.970",
            " 7, 0.9594, 1.182, 0.118, 1.882",
            " 8, 0.9650, 1.099, 0.185, 1.815",
            " 9, 0.9693, 1.032, 0.239, 1.761",
            "10, 0.9727, 0.975, 0.284, 1.716",
            "11, 0.9754, 0.927, 0.321, 1.679",
            "12, 0.9776, 0.886, 0.354, 1.646",
            "15, 0.9823, 0.789, 0.428, 1.572",
            "20, 0.9869, 0.680, 0.510, 1.490",
            "25, 0.9896, 0.606, 0.565, 1.435"})
    void tablePubliee(int n, double c4, double a3, double b3, double b4) {
        StdevConstants k = StdevConstants.forSubgroupSize(n);

        assertThat(k.c4()).isCloseTo(c4, within(TABLE));
        assertThat(k.a3()).isCloseTo(a3, within(TABLE));
        assertThat(k.b3()).isCloseTo(b3, within(TABLE));
        assertThat(k.b4()).isCloseTo(b4, within(TABLE));
    }

    @Test
    @DisplayName("c4 croît vers 1 : s̄ est de moins en moins biaisé quand le sous-groupe grossit")
    void c4CroitVersUn() {
        double precedent = 0;
        for (int n = StdevConstants.MIN_SUBGROUP_SIZE; n <= StdevConstants.MAX_SUBGROUP_SIZE; n++) {
            double c4 = StdevConstants.forSubgroupSize(n).c4();
            assertThat(c4).isGreaterThan(precedent).isLessThan(1.0);
            precedent = c4;
        }
    }

    @Test
    @DisplayName("B3 reste nul tant que la limite basse de la carte S serait négative")
    void b3NulAvantSix() {
        for (int n = 2; n <= 5; n++) {
            assertThat(StdevConstants.forSubgroupSize(n).b3()).isZero();
        }
        assertThat(StdevConstants.forSubgroupSize(6).b3()).isPositive();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 26, 100})
    @DisplayName("hors table, la constante est refusée sur le champ de la taille")
    void horsTable(int n) {
        assertThatThrownBy(() -> StdevConstants.forSubgroupSize(n))
                .isInstanceOf(SpcValidationException.class)
                .extracting("field").isEqualTo("subgroupSize");
    }

    @Test
    @DisplayName("Γ(k/2) se déroule depuis Γ(1) = 1 et Γ(1/2) = √π")
    void gammaDemiEntiers() {
        assertThat(StdevConstants.gammaHalf(1)).isCloseTo(Math.sqrt(Math.PI), within(1e-12));
        assertThat(StdevConstants.gammaHalf(2)).isCloseTo(1.0, within(1e-12));
        assertThat(StdevConstants.gammaHalf(3)).isCloseTo(Math.sqrt(Math.PI) / 2, within(1e-12));
        // Γ(5) = 4! = 24 et Γ(11/2) = 4,5 × 3,5 × 2,5 × 1,5 × 0,5 × √π.
        assertThat(StdevConstants.gammaHalf(10)).isCloseTo(24.0, within(1e-9));
        assertThat(StdevConstants.gammaHalf(11)).isCloseTo(52.34277778, within(1e-6));
        assertThat(StdevConstants.c4(2)).isCloseTo(0.797884561, within(1e-9));
    }
}
