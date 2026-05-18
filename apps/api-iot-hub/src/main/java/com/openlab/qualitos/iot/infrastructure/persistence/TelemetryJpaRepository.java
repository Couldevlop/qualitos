package com.openlab.qualitos.iot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TelemetryJpaRepository extends JpaRepository<TelemetryEntity, UUID> {

  @Query("SELECT t FROM TelemetryEntity t WHERE t.tenantId = :tenantId AND t.deviceId = :deviceId" +
      " AND t.recordedAt >= :from AND t.recordedAt <= :to ORDER BY t.recordedAt DESC")
  List<TelemetryEntity> findRange(
      @Param("tenantId") UUID tenantId,
      @Param("deviceId") UUID deviceId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      org.springframework.data.domain.Pageable pageable);

  long countByTenantId(UUID tenantId);
}
