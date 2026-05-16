package com.openlab.qualitos.quality.ropa.infrastructure;

import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessingActivityJpaRepository
        extends JpaRepository<ProcessingActivityJpaEntity, UUID> {

    Optional<ProcessingActivityJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<ProcessingActivityJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ProcessingActivityJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, ProcessingActivityStatus status, Pageable pageable);

    Optional<ProcessingActivityJpaEntity> findByTenantIdAndReference(
            UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
