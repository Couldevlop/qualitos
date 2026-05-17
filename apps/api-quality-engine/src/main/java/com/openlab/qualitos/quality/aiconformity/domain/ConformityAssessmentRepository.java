package com.openlab.qualitos.quality.aiconformity.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConformityAssessmentRepository {

    ConformityAssessment save(ConformityAssessment assessment);

    Optional<ConformityAssessment> findById(UUID id);

    List<ConformityAssessment> findByTenant(UUID tenantId);

    List<ConformityAssessment> findByTenantAndStatus(
            UUID tenantId, ConformityAssessmentStatus status);

    List<ConformityAssessment> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId);

    /** Certifications dont validUntil ≤ now. */
    List<ConformityAssessment> findExpiringCertificates(UUID tenantId, Instant now, int limit);

    Optional<ConformityAssessment> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
