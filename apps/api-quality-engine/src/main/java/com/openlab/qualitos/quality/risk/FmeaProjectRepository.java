package com.openlab.qualitos.quality.risk;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FmeaProjectRepository extends JpaRepository<FmeaProject, UUID> {

    Optional<FmeaProject> findByTenantIdAndCode(UUID tenantId, String code);

    Page<FmeaProject> findByTenantId(UUID tenantId, Pageable pageable);

    Page<FmeaProject> findByTenantIdAndStatus(UUID tenantId, FmeaStatus status, Pageable pageable);

    Page<FmeaProject> findByTenantIdAndType(UUID tenantId, FmeaType type, Pageable pageable);

    Page<FmeaProject> findByTenantIdAndProductId(UUID tenantId, UUID productId, Pageable pageable);

    List<FmeaProject> findByTenantIdAndProductId(UUID tenantId, UUID productId);

    /**
     * Garde d'unicité côté service. L'index partiel {@code uk_pfmea_active_per_product}
     * dit la même chose en base ; H2 ne connaissant pas les index partiels, les tests
     * ne verraient jamais la seconde ceinture.
     */
    boolean existsByTenantIdAndProductIdAndTypeAndStatus(
            UUID tenantId, UUID productId, FmeaType type, FmeaStatus status);
}
