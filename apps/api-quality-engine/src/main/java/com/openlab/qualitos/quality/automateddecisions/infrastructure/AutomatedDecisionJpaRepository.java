package com.openlab.qualitos.quality.automateddecisions.infrastructure;

import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AutomatedDecisionJpaRepository
        extends JpaRepository<AutomatedDecisionJpaEntity, UUID> {

    Optional<AutomatedDecisionJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<AutomatedDecisionJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AutomatedDecisionJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, AutomatedDecisionStatus status, Pageable pageable);

    Optional<AutomatedDecisionJpaEntity> findByTenantIdAndReference(
            UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
