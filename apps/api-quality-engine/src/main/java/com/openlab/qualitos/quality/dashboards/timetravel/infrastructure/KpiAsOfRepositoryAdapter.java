package com.openlab.qualitos.quality.dashboards.timetravel.infrastructure;

import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfRepository;
import com.openlab.qualitos.quality.dashboards.timetravel.domain.KpiAsOfSnapshot;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class KpiAsOfRepositoryAdapter implements KpiAsOfRepository {

    private final KpiAsOfJpaRepository jpa;

    public KpiAsOfRepositoryAdapter(KpiAsOfJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<KpiAsOfSnapshot> snapshotAsOf(UUID tenantId, Instant asOf) {
        return jpa.snapshotAsOf(tenantId, asOf).stream()
                .map(this::toDomain)
                .toList();
    }

    private KpiAsOfSnapshot toDomain(KpiAsOfRow row) {
        if (row.getValue() == null) {
            return KpiAsOfSnapshot.absent(row.getKpiId(), row.getCode(),
                    row.getName(), row.getUnit());
        }
        return KpiAsOfSnapshot.withValue(row.getKpiId(), row.getCode(),
                row.getName(), row.getUnit(), row.getValue(), row.getMeasuredPeriodStart());
    }
}
