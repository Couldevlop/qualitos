package com.openlab.qualitos.quality.iot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IotThresholdRepository extends JpaRepository<IotThreshold, UUID> {

    Page<IotThreshold> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<IotThreshold> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Seuils actifs applicables à une mesure : ceux ciblant explicitement le device
     * ET les seuils tenant-larges (device_id NULL), pour la métrique donnée.
     */
    @Query("""
            SELECT t FROM IotThreshold t
            WHERE t.tenantId = :tenantId
              AND t.enabled = true
              AND t.metric = :metric
              AND (t.deviceId IS NULL OR t.deviceId = :deviceId)
            """)
    List<IotThreshold> findApplicable(@Param("tenantId") UUID tenantId,
                                      @Param("deviceId") UUID deviceId,
                                      @Param("metric") String metric);
}
