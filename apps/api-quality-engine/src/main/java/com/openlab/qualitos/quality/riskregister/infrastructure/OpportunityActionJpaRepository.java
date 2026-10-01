package com.openlab.qualitos.quality.riskregister.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpportunityActionJpaRepository extends JpaRepository<OpportunityActionJpaEntity, UUID> {

    Optional<OpportunityActionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<OpportunityActionJpaEntity> findByTenantIdAndOpportunityId(UUID tenantId, UUID opportunityId);

    @Query("select coalesce(max(a.number), 0) from OpportunityActionJpaEntity a where a.tenantId = :tenantId")
    int maxNumber(@Param("tenantId") UUID tenantId);
}
