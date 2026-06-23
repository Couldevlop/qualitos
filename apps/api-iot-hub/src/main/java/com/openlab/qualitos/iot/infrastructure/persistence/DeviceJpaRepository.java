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

  /** Tenant-agnostic lookup by code (MQTT ingestion path). May return more than
   * one row when the same code is used by different tenants — the adapter
   * fails-closed on ambiguity. */
  List<DeviceEntity> findByCode(String code);

  List<DeviceEntity> findAllByTenantId(UUID tenantId);

  long countByTenantId(UUID tenantId);

  @Modifying
  @Query("UPDATE DeviceEntity d SET d.lastSeenAt = :when WHERE d.tenantId = :tenantId AND d.id = :id")
  void touchLastSeen(@Param("tenantId") UUID tenantId, @Param("id") UUID id, @Param("when") Instant when);

  /**
   * Met à jour le Device Shadow (twin) via un UPDATE en masse (et non un
   * load-modify-save d'entité gérée). Indispensable côté ingestion multi-métriques :
   * un save() d'entité déclenche le contrôle « expected 1 row » d'Hibernate qui, en
   * cas de 0 ligne (course, état transitoire), lève une StaleStateException pendant
   * le flush et EMPOISONNE la session → la métrique suivante du même lot échouait.
   * Un bulk UPDATE renvoie simplement le nombre de lignes (0 ou 1), sans exception.
   */
  @Modifying
  @Query("UPDATE DeviceEntity d SET d.twinJson = :json WHERE d.tenantId = :tenantId AND d.id = :id")
  void updateTwinJson(@Param("tenantId") UUID tenantId, @Param("id") UUID id, @Param("json") String json);
}
