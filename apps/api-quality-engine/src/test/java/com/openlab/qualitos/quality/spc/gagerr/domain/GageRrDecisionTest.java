package com.openlab.qualitos.quality.spc.gagerr.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** La règle de la cellule C38 : &lt; 10 % acceptable, 10 – 30 % sous conditions, &gt; 30 % non acceptable. */
class GageRrDecisionTest {

    @ParameterizedTest
    @CsvSource({
            "0, ACCEPTABLE",
            "0.0999, ACCEPTABLE",
            "0.10, CONDITIONAL",
            "0.21, CONDITIONAL",
            "0.30, CONDITIONAL",
            "0.3001, NOT_ACCEPTABLE",
            "1, NOT_ACCEPTABLE"})
    void lesSeuilsAiag(double ratio, GageRrDecision attendu) {
        assertThat(GageRrDecision.of(ratio)).isEqualTo(attendu);
    }
}
