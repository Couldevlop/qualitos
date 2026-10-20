package com.openlab.qualitos.quality.spc.capability.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityThresholdsTest {

    @Test
    void lesSeuilsParDefautSontCeuxDeLaMaquette() {
        assertThat(CapabilityThresholds.DEFAULT.capable()).isEqualByComparingTo("1.67");
        assertThat(CapabilityThresholds.DEFAULT.watch()).isEqualByComparingTo("1.33");
    }

    @ParameterizedTest
    @CsvSource({"1.67,CAPABLE", "1.80,CAPABLE", "1.669,WATCH", "1.33,WATCH", "1.329,NOT_CAPABLE", "-0.5,NOT_CAPABLE"})
    void leCpkSeRangeSelonLesSeuils(double cpk, CapabilityStatus attendu) {
        assertThat(CapabilityThresholds.DEFAULT.classify(cpk)).isEqualTo(attendu);
    }

    @Test
    void seulLeStatutMaitriseVautProcedeMaitrise() {
        assertThat(CapabilityStatus.CAPABLE.controlled()).isTrue();
        assertThat(CapabilityStatus.WATCH.controlled()).isFalse();
        assertThat(CapabilityStatus.NOT_CAPABLE.controlled()).isFalse();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,1.33,capableThreshold",
            "1.67,null,watchThreshold",
            "1.675,1.33,capableThreshold",
            "1.67,1.333,capableThreshold",
            "1.67,0,watchThreshold",
            "1.67,-1,watchThreshold",
            "10.01,1.33,capableThreshold",
            "1.33,1.33,watchThreshold",
            "1.33,1.67,watchThreshold"}, nullValues = "null")
    void desSeuilsIncoherentsSontRefuses(BigDecimal capable, BigDecimal watch, String champ) {
        assertThatThrownBy(() -> new CapabilityThresholds(capable, watch))
                .isInstanceOf(SpcValidationException.class)
                .extracting("field").isEqualTo(champ);
    }

    @Test
    void leMaximumEstAdmis() {
        assertThat(new CapabilityThresholds(new BigDecimal("10.00"), new BigDecimal("2")).capable())
                .isEqualByComparingTo("10");
    }
}
