package com.openlab.qualitos.quality.iot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface IotTelemetryEventRepository extends JpaRepository<IotTelemetryEvent, UUID> {

    Page<IotTelemetryEvent> findByTenantIdAndDeviceIdOrderByRecordedAtDesc(
            UUID tenantId, UUID deviceId, Pageable pageable);

    Page<IotTelemetryEvent> findByTenantIdAndDeviceIdAndMetricAndRecordedAtBetweenOrderByRecordedAtAsc(
            UUID tenantId, UUID deviceId, String metric, Instant from, Instant to, Pageable pageable);

    long deleteByRecordedAtBefore(Instant cutoff);
}
