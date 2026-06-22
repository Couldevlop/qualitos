package com.openlab.qualitos.quality.dashboards.timetravel.infrastructure;

import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiAsOfRepositoryAdapterTest {

    @Mock KpiAsOfJpaRepository jpa;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID KPI = UUID.randomUUID();
    static final Instant ASOF = Instant.parse("2026-03-15T00:00:00Z");

    KpiAsOfRepositoryAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new KpiAsOfRepositoryAdapter(jpa);
    }

    @Test
    void mapsPresentRow_toPresentSnapshot() {
        when(jpa.snapshotAsOf(TENANT, ASOF)).thenReturn(List.of(
                row(KPI, "fpy", "First Pass Yield", "%",
                        new BigDecimal("94.2"), Instant.parse("2026-03-01T00:00:00Z"))));

        List<KpiAsOfSnapshot> out = adapter.snapshotAsOf(TENANT, ASOF);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).isPresent()).isTrue();
        assertThat(out.get(0).getValue()).isEqualByComparingTo("94.2");
        assertThat(out.get(0).getCode()).isEqualTo("fpy");
    }

    @Test
    void mapsNullValueRow_toAbsentSnapshot() {
        when(jpa.snapshotAsOf(TENANT, ASOF)).thenReturn(List.of(
                row(KPI, "fpy", "First Pass Yield", "%", null, null)));

        List<KpiAsOfSnapshot> out = adapter.snapshotAsOf(TENANT, ASOF);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).isPresent()).isFalse();
        assertThat(out.get(0).getValue()).isNull();
    }

    @Test
    void emptyResult_isEmptyList() {
        when(jpa.snapshotAsOf(TENANT, ASOF)).thenReturn(List.of());
        assertThat(adapter.snapshotAsOf(TENANT, ASOF)).isEmpty();
    }

    private static KpiAsOfRow row(UUID kpiId, String code, String name, String unit,
                                  BigDecimal value, Instant period) {
        return new KpiAsOfRow() {
            @Override public UUID getKpiId() { return kpiId; }
            @Override public String getCode() { return code; }
            @Override public String getName() { return name; }
            @Override public String getUnit() { return unit; }
            @Override public BigDecimal getValue() { return value; }
            @Override public Instant getMeasuredPeriodStart() { return period; }
        };
    }
}
