package com.openlab.qualitos.quality.nonconformity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NonConformityRepository extends JpaRepository<NonConformity, UUID> {

    Page<NonConformity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<NonConformity> findByTenantIdAndStatus(UUID tenantId, NcStatus status, Pageable pageable);

    Page<NonConformity> findByTenantIdAndSeverity(UUID tenantId, NcSeverity severity, Pageable pageable);

    Page<NonConformity> findByTenantIdAndCategory(UUID tenantId, NcCategory category, Pageable pageable);

    Page<NonConformity> findByTenantIdAndStatusAndSeverity(
            UUID tenantId, NcStatus status, NcSeverity severity, Pageable pageable);

    Page<NonConformity> findByTenantIdAndStatusAndCategory(
            UUID tenantId, NcStatus status, NcCategory category, Pageable pageable);

    Page<NonConformity> findByTenantIdAndSeverityAndCategory(
            UUID tenantId, NcSeverity severity, NcCategory category, Pageable pageable);

    Page<NonConformity> findByTenantIdAndStatusAndSeverityAndCategory(
            UUID tenantId, NcStatus status, NcSeverity severity, NcCategory category, Pageable pageable);

    Optional<NonConformity> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Vrai si la référence générée est déjà prise pour ce tenant (collision improbable). */
    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    /** Numérotation séquentielle annuelle, par tenant. */
    long countByTenantIdAndReferenceStartingWith(UUID tenantId, String prefix);
}
