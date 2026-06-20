package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA — exécutions d'audit blanc. Toutes les méthodes filtrent par
 * tenant (issu du JWT côté adapter) pour empêcher toute lecture cross-tenant.
 */
public interface MockAuditRunJpaRepository extends JpaRepository<MockAuditRunJpaEntity, UUID> {

    Optional<MockAuditRunJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<MockAuditRunJpaEntity> findByTenantIdAndAdoptionId(
            UUID tenantId, UUID adoptionId, Pageable pageable);
}
