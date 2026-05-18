package com.openlab.qualitos.quality.aieudb.infrastructure;

import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EudbRegistrationJpaRepository
        extends JpaRepository<EudbRegistrationJpaEntity, UUID> {

    Optional<EudbRegistrationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<EudbRegistrationJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<EudbRegistrationJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, EudbRegistrationStatus status, Pageable pageable);

    Page<EudbRegistrationJpaEntity> findByTenantIdAndAiSystemId(
            UUID tenantId, UUID aiSystemId, Pageable pageable);

    Optional<EudbRegistrationJpaEntity> findByTenantIdAndReference(
            UUID tenantId, String reference);

    Optional<EudbRegistrationJpaEntity> findByTenantIdAndEudbId(UUID tenantId, String eudbId);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
