package com.openlab.qualitos.quality.dashboards.timetravel.application;

import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfRepository;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeTravelServiceTest {

    @Mock KpiAsOfRepository repo;
    @Mock TenantProvider tenantProvider;

    static final Instant NOW = Instant.parse("2026-06-20T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID KPI = UUID.randomUUID();

    TimeTravelService service;

    @BeforeEach
    void setup() {
        service = new TimeTravelService(repo, tenantProvider, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
    }

    @Test
    void snapshotAsOf_mapsPresentValues_andNotEmpty() {
        Instant asOf = Instant.parse("2026-03-15T00:00:00Z");
        when(repo.snapshotAsOf(TENANT, asOf)).thenReturn(List.of(
                KpiAsOfSnapshot.withValue(KPI, "fpy", "First Pass Yield", "%",
                        new BigDecimal("94.2"), Instant.parse("2026-03-01T00:00:00Z"))));

        var view = service.snapshotAsOf(asOf);

        assertThat(view.asOf()).isEqualTo(asOf);
        assertThat(view.empty()).isFalse();
        assertThat(view.kpis()).hasSize(1);
        assertThat(view.kpis().get(0).value()).isEqualByComparingTo("94.2");
        assertThat(view.kpis().get(0).present()).isTrue();
    }

    @Test
    void snapshotAsOf_allAbsent_isEmptyState() {
        Instant asOf = Instant.parse("2020-01-01T00:00:00Z");
        when(repo.snapshotAsOf(TENANT, asOf)).thenReturn(List.of(
                KpiAsOfSnapshot.absent(KPI, "fpy", "First Pass Yield", "%")));

        var view = service.snapshotAsOf(asOf);

        assertThat(view.empty()).isTrue();
        assertThat(view.kpis()).hasSize(1);
        assertThat(view.kpis().get(0).present()).isFalse();
    }

    @Test
    void snapshotAsOf_noKpis_isEmptyState() {
        Instant asOf = Instant.parse("2026-01-01T00:00:00Z");
        when(repo.snapshotAsOf(TENANT, asOf)).thenReturn(List.of());
        var view = service.snapshotAsOf(asOf);
        assertThat(view.empty()).isTrue();
        assertThat(view.kpis()).isEmpty();
    }

    @Test
    void snapshotAsOf_futureDate_isClampedToNow() {
        Instant future = NOW.plusSeconds(86400);
        when(repo.snapshotAsOf(eq(TENANT), eq(NOW))).thenReturn(List.of());

        var view = service.snapshotAsOf(future);

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(repo).snapshotAsOf(eq(TENANT), captor.capture());
        assertThat(captor.getValue()).isEqualTo(NOW);
        assertThat(view.asOf()).isEqualTo(NOW);
    }

    @Test
    void snapshotAsOf_nullDate_throws() {
        assertThatThrownBy(() -> service.snapshotAsOf(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
