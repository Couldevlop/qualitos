package com.openlab.qualitos.quality.dashboards.timetravel.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KpiAsOfSnapshotTest {

    static final UUID KPI = UUID.randomUUID();
    static final Instant T = Instant.parse("2026-03-15T00:00:00Z");

    @Test
    void withValue_isPresent() {
        KpiAsOfSnapshot s = KpiAsOfSnapshot.withValue(
                KPI, "fpy", "First Pass Yield", "%", new BigDecimal("94.2"), T);
        assertThat(s.isPresent()).isTrue();
        assertThat(s.getValue()).isEqualByComparingTo("94.2");
        assertThat(s.getMeasuredPeriodStart()).isEqualTo(T);
        assertThat(s.getCode()).isEqualTo("fpy");
        assertThat(s.getName()).isEqualTo("First Pass Yield");
        assertThat(s.getUnit()).isEqualTo("%");
        assertThat(s.getKpiId()).isEqualTo(KPI);
    }

    @Test
    void absent_isNotPresent() {
        KpiAsOfSnapshot s = KpiAsOfSnapshot.absent(KPI, "fpy", "First Pass Yield", "%");
        assertThat(s.isPresent()).isFalse();
        assertThat(s.getValue()).isNull();
        assertThat(s.getMeasuredPeriodStart()).isNull();
    }

    @Test
    void withValue_requiresValue() {
        assertThatThrownBy(() -> KpiAsOfSnapshot.withValue(KPI, "c", "n", "u", null, T))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_requiresMandatoryFields() {
        assertThatThrownBy(() -> new KpiAsOfSnapshot(null, "c", "n", "u", null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new KpiAsOfSnapshot(KPI, null, "n", "u", null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new KpiAsOfSnapshot(KPI, "c", null, "u", null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void unit_isOptional() {
        KpiAsOfSnapshot s = KpiAsOfSnapshot.absent(KPI, "c", "n", null);
        assertThat(s.getUnit()).isNull();
    }
}
