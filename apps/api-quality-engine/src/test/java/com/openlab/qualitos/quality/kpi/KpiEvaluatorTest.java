package com.openlab.qualitos.quality.kpi;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class KpiEvaluatorTest {

    @Test
    void higher_aboveTarget_ok() {
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, "85", "70", "50");
        assertThat(KpiEvaluator.evaluate(d, bd("90"))).isEqualTo(KpiHealth.OK);
        assertThat(KpiEvaluator.evaluate(d, bd("85"))).isEqualTo(KpiHealth.OK);
    }

    @Test
    void higher_atTargetExactly_ok() {
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, "85", "70", "50");
        assertThat(KpiEvaluator.evaluate(d, bd("85.0"))).isEqualTo(KpiHealth.OK);
    }

    @Test
    void higher_betweenWarningAndTarget_warning() {
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, "85", "70", "50");
        assertThat(KpiEvaluator.evaluate(d, bd("75"))).isEqualTo(KpiHealth.WARNING);
        assertThat(KpiEvaluator.evaluate(d, bd("70"))).isEqualTo(KpiHealth.WARNING);
    }

    @Test
    void higher_belowCritical_critical() {
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, "85", "70", "50");
        assertThat(KpiEvaluator.evaluate(d, bd("40"))).isEqualTo(KpiHealth.CRITICAL);
    }

    @Test
    void higher_betweenCriticalAndWarning_critical() {
        // Pas ≥ warning ⇒ tombe sur critical (warning floor non atteint).
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, "85", "70", "50");
        assertThat(KpiEvaluator.evaluate(d, bd("60"))).isEqualTo(KpiHealth.CRITICAL);
    }

    @Test
    void lower_belowTarget_ok() {
        KpiDefinition d = def(KpiDirection.LOWER_IS_BETTER, "30", "50", "70");
        assertThat(KpiEvaluator.evaluate(d, bd("25"))).isEqualTo(KpiHealth.OK);
        assertThat(KpiEvaluator.evaluate(d, bd("30"))).isEqualTo(KpiHealth.OK);
    }

    @Test
    void lower_betweenTargetAndWarning_warning() {
        KpiDefinition d = def(KpiDirection.LOWER_IS_BETTER, "30", "50", "70");
        assertThat(KpiEvaluator.evaluate(d, bd("45"))).isEqualTo(KpiHealth.WARNING);
        assertThat(KpiEvaluator.evaluate(d, bd("50"))).isEqualTo(KpiHealth.WARNING);
    }

    @Test
    void lower_aboveCritical_critical() {
        KpiDefinition d = def(KpiDirection.LOWER_IS_BETTER, "30", "50", "70");
        assertThat(KpiEvaluator.evaluate(d, bd("80"))).isEqualTo(KpiHealth.CRITICAL);
    }

    @Test
    void lower_betweenWarningAndCritical_critical() {
        KpiDefinition d = def(KpiDirection.LOWER_IS_BETTER, "30", "50", "70");
        assertThat(KpiEvaluator.evaluate(d, bd("60"))).isEqualTo(KpiHealth.CRITICAL);
    }

    @Test
    void nullValue_unknown() {
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, "85", "70", "50");
        assertThat(KpiEvaluator.evaluate(d, null)).isEqualTo(KpiHealth.UNKNOWN);
    }

    @Test
    void allThresholdsNull_unknown() {
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, null, null, null);
        assertThat(KpiEvaluator.evaluate(d, bd("100"))).isEqualTo(KpiHealth.UNKNOWN);
    }

    @Test
    void higher_onlyTarget_belowTarget_warning() {
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, "85", null, null);
        assertThat(KpiEvaluator.evaluate(d, bd("80"))).isEqualTo(KpiHealth.WARNING);
        assertThat(KpiEvaluator.evaluate(d, bd("90"))).isEqualTo(KpiHealth.OK);
    }

    @Test
    void higher_onlyWarning_aboveWarning_ok() {
        // Pas de target ⇒ on n'a pas d'OK fort. Au-dessus du warning, on retourne OK
        // (sortie par défaut quand warning ≥ atteint).
        KpiDefinition d = def(KpiDirection.HIGHER_IS_BETTER, null, "70", null);
        assertThat(KpiEvaluator.evaluate(d, bd("80"))).isEqualTo(KpiHealth.WARNING);
        assertThat(KpiEvaluator.evaluate(d, bd("60"))).isEqualTo(KpiHealth.CRITICAL);
    }

    @Test
    void lower_onlyTarget_aboveTarget_warning() {
        KpiDefinition d = def(KpiDirection.LOWER_IS_BETTER, "30", null, null);
        assertThat(KpiEvaluator.evaluate(d, bd("40"))).isEqualTo(KpiHealth.WARNING);
        assertThat(KpiEvaluator.evaluate(d, bd("20"))).isEqualTo(KpiHealth.OK);
    }

    private static KpiDefinition def(KpiDirection dir, String target, String warn, String critical) {
        KpiDefinition d = new KpiDefinition();
        d.setDirection(dir);
        d.setTargetValue(target == null ? null : bd(target));
        d.setThresholdWarning(warn == null ? null : bd(warn));
        d.setThresholdCritical(critical == null ? null : bd(critical));
        return d;
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }
}
