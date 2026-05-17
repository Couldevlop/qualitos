package com.openlab.qualitos.iot.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceJpaRepository extends JpaRepository<DeviceEntity, UUID> {

  Optional<DeviceEntity> findByTenantIdAndId(UUID tenantId, UUID id);

  Optional<DeviceEntity> findByTenantIdAndCode(UUID tenantId, String code);

  List<DeviceEntity> findAllByTenantId(UUID tenantId);

  long countByTenantId(UUID tenantId);

  @Modifying
  @Query("UPDATE DeviceEntity d SET d.lastSeenAt = :when WHERE d.tenantId = :tenantId AND d.id = :id")
  void touchLastSeen(@Param("tenantId") UUID tenantId, @Param("id") UUID id, @Param("when") Instant when);
}
