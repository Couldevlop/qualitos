package com.openlab.qualitos.quality.dpia.infrastructure;

import com.openlab.qualitos.quality.dpia.domain.DpiaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DpiaJpaRepository extends JpaRepository<DpiaJpaEntity, UUID> {

    Optional<DpiaJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<DpiaJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<DpiaJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, DpiaStatus status, Pageable pageable);

    Optional<DpiaJpaEntity> findByTenantIdAndReference(UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
