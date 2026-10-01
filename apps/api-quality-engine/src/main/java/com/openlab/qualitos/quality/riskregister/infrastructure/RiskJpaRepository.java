package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskJpaRepository extends JpaRepository<RiskJpaEntity, UUID> {

    Optional<RiskJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<RiskJpaEntity> findByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    List<RiskJpaEntity> findByTenantIdAndOriginAndSourceId(UUID tenantId, RegisterOrigin origin, UUID sourceId);
}
