package com.openlab.qualitos.quality.spc.gagerr.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** La loi F contre des valeurs de référence (mpmath, bêta incomplète régularisée). */
class FDistributionTest {

    @ParameterizedTest
    @CsvSource({
            "1.0, 2, 4, 0.4444444444444444",
            "3.5, 9, 18, 0.011372425817733659",
            "0.5, 18, 60, 0.948025652318549",
            "10, 1, 1, 0.19498222904213663",
            "2.2, 5, 40, 0.07328224987216389",
            "1.1180366774523711, 18, 60, 0.3583858414026355"})
    void survieDeReference(double f, double d1, double d2, double attendu) {
        assertThat(FDistribution.survival(f, d1, d2)).isCloseTo(attendu, within(1e-12));
    }

    @Test
    void bornes() {
        assertThat(FDistribution.survival(0, 2, 3)).isEqualTo(1.0);
        assertThat(FDistribution.survival(-1, 2, 3)).isEqualTo(1.0);
        assertThat(FDistribution.survival(Double.POSITIVE_INFINITY, 2, 3)).isZero();
        assertThat(FDistribution.regularizedBeta(0, 2, 3)).isZero();
        assertThat(FDistribution.regularizedBeta(1, 2, 3)).isEqualTo(1.0);
    }

    @Test
    void desDegresDeLiberteInvalidesSontRefuses() {
        assertThatThrownBy(() -> FDistribution.survival(1, 0, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FDistribution.survival(1, 2, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FDistribution.survival(Double.NaN, 2, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lnGammaDesDeuxCotesDeUnDemi() {
        assertThat(FDistribution.lnGamma(5)).isCloseTo(Math.log(24), within(1e-12));
        assertThat(FDistribution.lnGamma(0.5)).isCloseTo(Math.log(Math.sqrt(Math.PI)), within(1e-12));
        // Γ(0,25) = 3,6256099082219083 : la formule des compléments.
        assertThat(FDistribution.lnGamma(0.25)).isCloseTo(Math.log(3.6256099082219083), within(1e-12));
    }
}
