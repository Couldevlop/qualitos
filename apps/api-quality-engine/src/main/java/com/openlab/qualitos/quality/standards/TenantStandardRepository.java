package com.openlab.qualitos.quality.standards;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantStandardRepository extends JpaRepository<TenantStandard, UUID> {

    Page<TenantStandard> findByTenantId(UUID tenantId, Pageable pageable);

    Page<TenantStandard> findByTenantIdAndStatus(UUID tenantId, AdoptionStatus status, Pageable pageable);

    Optional<TenantStandard> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndStandardId(UUID tenantId, UUID standardId);
}
