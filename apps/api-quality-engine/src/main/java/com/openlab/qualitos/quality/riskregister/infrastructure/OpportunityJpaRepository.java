package com.openlab.qualitos.quality.riskregister.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityJpaRepository extends JpaRepository<OpportunityJpaEntity, UUID> {

    Optional<OpportunityJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<OpportunityJpaEntity> findByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
