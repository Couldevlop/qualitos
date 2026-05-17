package com.openlab.qualitos.quality.aiconformity.infrastructure;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConformityAssessmentJpaRepository
        extends JpaRepository<ConformityAssessmentJpaEntity, UUID> {

    Optional<ConformityAssessmentJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<ConformityAssessmentJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ConformityAssessmentJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, ConformityAssessmentStatus status, Pageable pageable);

    Page<ConformityAssessmentJpaEntity> findByTenantIdAndAiSystemId(
            UUID tenantId, UUID aiSystemId, Pageable pageable);

    Optional<ConformityAssessmentJpaEntity> findByTenantIdAndReference(
            UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    /**
     * Certifications dont validUntil ≤ now (donc périmées en pratique).
     * Index partiel idx_aica_certified_valid_until accélère le scan.
     */
    @Query("""
            SELECT c FROM ConformityAssessmentJpaEntity c
             WHERE c.tenantId = :tenantId
               AND c.status = com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus.CERTIFIED
               AND c.validUntil <= :now
             ORDER BY c.validUntil ASC
            """)
    List<ConformityAssessmentJpaEntity> findExpiringCertificates(
            @Param("tenantId") UUID tenantId,
            @Param("now") Instant now,
            Pageable pageable);
}
