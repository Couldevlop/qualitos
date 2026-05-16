package com.openlab.qualitos.quality.dmaic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test pur math : Cp/Cpk + stats descriptives.
 * Verifie les formules Six Sigma standard.
 */
class CapabilityCalculatorTest {

    private final CapabilityCalculator calc = new CapabilityCalculator();

    @Test
    void empty_returnsIndeterminate() {
        DmaicDto.CapabilityResponse r = calc.compute(List.of(), 10.0, 20.0, 15.0);
        assertThat(r.sampleSize()).isZero();
        assertThat(r.cp()).isNull();
        assertThat(r.cpk()).isNull();
        assertThat(r.interpretation()).isEqualTo("Indeterminé");
        assertThat(r.warnings()).isNotEmpty();
    }

    @Test
    void null_returnsIndeterminate() {
        DmaicDto.CapabilityResponse r = calc.compute(null, 10.0, 20.0, 15.0);
        assertThat(r.sampleSize()).isZero();
    }

    @Test
    void smallSample_addsWarning() {
        List<Double> vals = List.of(15.0, 15.1, 14.9, 15.2, 14.8);
        DmaicDto.CapabilityResponse r = calc.compute(vals, 10.0, 20.0, 15.0);
        assertThat(r.warnings()).anyMatch(w -> w.contains("Echantillon faible"));
        assertThat(r.sampleSize()).isEqualTo(5);
    }

    @Test
    void zeroStdDev_warnsNoCp() {
        // 30 mesures identiques → stdDev=0
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < 30; i++) vals.add(15.0);
        DmaicDto.CapabilityResponse r = calc.compute(vals, 10.0, 20.0, 15.0);
        assertThat(r.stdDev()).isZero();
        assertThat(r.cp()).isNull();
        assertThat(r.cpk()).isNull();
        assertThat(r.warnings()).anyMatch(w -> w.contains("Ecart-type nul"));
    }

    @Test
    void centeredProcess_cp_cpk_equal() {
        // Process centre sur 15, sigma ~1 → Cp = Cpk ~ 1.67
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < 50; i++) vals.add(15.0 + ((i % 3) - 1) * 1.0);
        DmaicDto.CapabilityResponse r = calc.compute(vals, 10.0, 20.0, 15.0);
        assertThat(r.sampleSize()).isEqualTo(50);
        assertThat(r.mean()).isCloseTo(15.0, org.assertj.core.api.Assertions.within(0.05));
        assertThat(r.cp()).isNotNull();
        assertThat(r.cpk()).isNotNull();
        // Process centre : Cp ≈ Cpk
        assertThat(Math.abs(r.cp() - r.cpk())).isLessThan(0.05);
    }

    @Test
    void offCenterProcess_cpk_lessThanCp() {
        // Mean = 17 (decale vers USL=20), Cp identique mais Cpk reduit
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < 50; i++) vals.add(17.0 + ((i % 3) - 1) * 1.0);
        DmaicDto.CapabilityResponse r = calc.compute(vals, 10.0, 20.0, 15.0);
        assertThat(r.cp()).isNotNull();
        assertThat(r.cpk()).isLessThan(r.cp());
        assertThat(r.cpu()).isLessThan(r.cpl());  // mean plus proche USL
    }

    @Test
    void onlyUpperLimit_cpk_equals_cpu() {
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < 50; i++) vals.add(15.0 + ((i % 3) - 1) * 1.0);
        DmaicDto.CapabilityResponse r = calc.compute(vals, null, 20.0, null);
        assertThat(r.cp()).isNull();  // pas de borne inferieure
        assertThat(r.cpu()).isNotNull();
        assertThat(r.cpl()).isNull();
        assertThat(r.cpk()).isEqualTo(r.cpu());
    }

    @Test
    void onlyLowerLimit_cpk_equals_cpl() {
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < 50; i++) vals.add(15.0 + ((i % 3) - 1) * 1.0);
        DmaicDto.CapabilityResponse r = calc.compute(vals, 10.0, null, null);
        assertThat(r.cp()).isNull();
        assertThat(r.cpl()).isNotNull();
        assertThat(r.cpu()).isNull();
        assertThat(r.cpk()).isEqualTo(r.cpl());
    }

    @Test
    void interpretCpk_categories() {
        assertThat(CapabilityCalculator.interpret(null)).isEqualTo("Indeterminé");
        assertThat(CapabilityCalculator.interpret(-0.5)).contains("hors specs");
        assertThat(CapabilityCalculator.interpret(0.8)).contains("non capable");
        assertThat(CapabilityCalculator.interpret(1.1)).contains("marginalement");
        assertThat(CapabilityCalculator.interpret(1.5)).contains("capable");
        assertThat(CapabilityCalculator.interpret(1.8)).contains("très capable");
        assertThat(CapabilityCalculator.interpret(2.2)).contains("Six Sigma");
    }

    @Test
    void sigmaLevel_isCpk_times3() {
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < 50; i++) vals.add(15.0 + ((i % 3) - 1) * 1.0);
        DmaicDto.CapabilityResponse r = calc.compute(vals, 10.0, 20.0, 15.0);
        assertThat(r.sigmaLevel()).isCloseTo(r.cpk() * 3d, org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    void minMax_computedCorrectly() {
        List<Double> vals = List.of(12.0, 15.0, 18.0, 14.0, 16.0,
                                    13.0, 17.0, 15.5, 14.5, 16.5);
        DmaicDto.CapabilityResponse r = calc.compute(vals, 10.0, 20.0, 15.0);
        assertThat(r.min()).isEqualTo(12.0);
        assertThat(r.max()).isEqualTo(18.0);
    }
}
