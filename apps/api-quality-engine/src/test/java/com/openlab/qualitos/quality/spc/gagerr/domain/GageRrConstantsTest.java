package com.openlab.qualitos.quality.spc.gagerr.domain;

import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** La table de l'onglet « Constantes », et le domaine du plan qu'elle fixe. */
class GageRrConstantsTest {

    @Test
    void laTableDuClasseurA515Sigma() {
        GageRrConstants c = GageRrConstants.forPlan(2, 2, 2, 5.15);
        assertThat(c.k1()).isCloseTo(4.56, within(1e-12));
        assertThat(c.k2()).isCloseTo(3.65, within(1e-12));
        assertThat(c.k3()).isCloseTo(3.65, within(1e-12));
        assertThat(c.d4()).isEqualTo(3.27);
        double[] k3 = {3.65, 2.70, 2.30, 2.08, 1.93, 1.82, 1.74, 1.67, 1.62};
        for (int n = 2; n <= 10; n++) {
            assertThat(GageRrConstants.forPlan(3, n, 3, 5.15).k3()).isCloseTo(k3[n - 2], within(1e-12));
        }
    }

    @Test
    void a6SigmaK1K2K3SontMisesALEchelleMaisPasD4() {
        GageRrConstants c = GageRrConstants.forPlan(3, 10, 3, 6);
        assertThat(c.k1()).isCloseTo(3.05 * 6 / 5.15, within(1e-12));
        assertThat(c.k2()).isCloseTo(2.70 * 6 / 5.15, within(1e-12));
        assertThat(c.k3()).isCloseTo(1.62 * 6 / 5.15, within(1e-12));
        assertThat(c.d4()).isEqualTo(2.58);
    }

    @Test
    void unPlanHorsTableEstRefuseChampParChamp() {
        assertThatThrownBy(() -> GageRrConstants.forPlan(4, 10, 3, 5.15))
                .isInstanceOf(SpcValidationException.class).extracting("field").isEqualTo("operatorCount");
        assertThatThrownBy(() -> GageRrConstants.forPlan(3, 11, 3, 5.15))
                .isInstanceOf(SpcValidationException.class).extracting("field").isEqualTo("partCount");
        assertThatThrownBy(() -> GageRrConstants.forPlan(3, 10, 1, 5.15))
                .isInstanceOf(SpcValidationException.class).extracting("field").isEqualTo("trialCount");
        assertThatThrownBy(() -> GageRrConstants.forPlan(3, 10, 3, 5.5))
                .isInstanceOf(SpcValidationException.class).extracting("field").isEqualTo("sigmaMultiplier");
    }

    @Test
    void leMultiplicateurVaut515Ou6() {
        assertThat(GageRrConstants.requireMultiplier(5.15)).isEqualTo(5.15);
        assertThat(GageRrConstants.requireMultiplier(6.0)).isEqualTo(6.0);
        assertThatThrownBy(() -> GageRrConstants.requireMultiplier(3))
                .isInstanceOf(SpcValidationException.class);
    }
}
