package com.openlab.qualitos.quality.kpi;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class KpiEntityCallbacksTest {

    @Test
    void kpiPrePersist_defaults() throws Exception {
        KpiDefinition d = new KpiDefinition();
        invoke(d, "prePersist");
        assertThat(d.getStatus()).isEqualTo(KpiStatus.DRAFT);
        assertThat(d.getDirection()).isEqualTo(KpiDirection.HIGHER_IS_BETTER);
        assertThat(d.getFrequency()).isEqualTo(KpiFrequency.MONTHLY);
        assertThat(d.getCreatedAt()).isNotNull();
    }

    @Test
    void kpiPrePersist_preservesValues() throws Exception {
        KpiDefinition d = new KpiDefinition();
        d.setStatus(KpiStatus.ACTIVE);
        d.setDirection(KpiDirection.LOWER_IS_BETTER);
        d.setFrequency(KpiFrequency.QUARTERLY);
        invoke(d, "prePersist");
        assertThat(d.getStatus()).isEqualTo(KpiStatus.ACTIVE);
        assertThat(d.getDirection()).isEqualTo(KpiDirection.LOWER_IS_BETTER);
        assertThat(d.getFrequency()).isEqualTo(KpiFrequency.QUARTERLY);
    }

    @Test
    void kpiPreUpdate_refreshes() throws Exception {
        KpiDefinition d = new KpiDefinition();
        d.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = d.getUpdatedAt();
        Thread.sleep(5);
        invoke(d, "preUpdate");
        assertThat(d.getUpdatedAt()).isAfter(before);
    }

    @Test
    void measurementPrePersist_defaultSourceManual() throws Exception {
        KpiMeasurement m = new KpiMeasurement();
        invoke(m, "prePersist");
        assertThat(m.getSource()).isEqualTo(MeasurementSource.MANUAL);
        assertThat(m.getCreatedAt()).isNotNull();
    }

    @Test
    void measurementPrePersist_preservesSource() throws Exception {
        KpiMeasurement m = new KpiMeasurement();
        m.setSource(MeasurementSource.IOT_AGGREGATED);
        invoke(m, "prePersist");
        assertThat(m.getSource()).isEqualTo(MeasurementSource.IOT_AGGREGATED);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
