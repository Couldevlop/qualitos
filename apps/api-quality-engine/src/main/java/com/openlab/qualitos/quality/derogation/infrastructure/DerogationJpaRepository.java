package com.openlab.qualitos.quality.derogation.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/** Depot Spring Data des dossiers. Detail d'infrastructure : nul ne le voit du domaine. */
public interface DerogationJpaRepository extends JpaRepository<DerogationJpaEntity, UUID>,
        JpaSpecificationExecutor<DerogationJpaEntity> {

    Optional<DerogationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    long countByTenantIdAndReferenceStartingWith(UUID tenantId, String prefix);

    Page<DerogationJpaEntity> findByTenantIdAndManagerAccountIdAndStatus(
            UUID tenantId, UUID managerAccountId, String status, Pageable pageable);

    long countByTenantIdAndManagerAccountIdAndStatus(
            UUID tenantId, UUID managerAccountId, String status);
}
