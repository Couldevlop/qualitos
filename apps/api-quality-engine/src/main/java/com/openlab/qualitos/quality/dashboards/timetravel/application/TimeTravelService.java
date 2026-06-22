package com.openlab.qualitos.quality.dashboards.timetravel.application;

import com.openlab.qualitos.quality.dashboards.application.TenantProvider;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfRepository;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfSnapshot;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use case — dashboard time-travel (CLAUDE.md §7.3).
 *
 * <p>Returns the real as-of state of the tenant's KPIs at a chosen instant by
 * reading {@code kpi_measurements} (no front simulation). tenantId comes from the
 * JWT (§18.2 #2). Future dates are clamped to "now"; a future or pre-history date
 * simply yields an empty/partly-empty snapshot, which the UI renders as a tidy
 * empty state.</p>
 */
public class TimeTravelService {

    private final KpiAsOfRepository repo;
    private final TenantProvider tenantProvider;
    private final Clock clock;

    public TimeTravelService(KpiAsOfRepository repo,
                             TenantProvider tenantProvider,
                             Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.clock = clock;
    }

    public TimeTravelDto.DashboardSnapshotView snapshotAsOf(Instant requestedAsOf) {
        if (requestedAsOf == null) {
            throw new IllegalArgumentException("asOf required");
        }
        UUID tenantId = tenantProvider.requireTenantId();
        Instant now = Instant.now(clock);
        // Time travel is about the PAST: never resolve beyond "now".
        Instant asOf = requestedAsOf.isAfter(now) ? now : requestedAsOf;

        List<KpiAsOfSnapshot> snapshots = repo.snapshotAsOf(tenantId, asOf);
        List<TimeTravelDto.KpiSnapshotView> views = snapshots.stream()
                .map(TimeTravelDto.KpiSnapshotView::of)
                .toList();
        boolean empty = views.stream().noneMatch(TimeTravelDto.KpiSnapshotView::present);
        return new TimeTravelDto.DashboardSnapshotView(asOf, empty, views);
    }
}
