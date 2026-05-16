package com.openlab.qualitos.quality.processoragreements.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessorAgreementRepository {

    ProcessorAgreement save(ProcessorAgreement agreement);

    Optional<ProcessorAgreement> findById(UUID id);

    List<ProcessorAgreement> findByTenant(UUID tenantId);

    List<ProcessorAgreement> findByTenantAndStatus(UUID tenantId, ProcessorAgreementStatus status);

    Optional<ProcessorAgreement> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    /** ACTIVE dont expirationDate ≤ now — scan d'expiration. */
    List<ProcessorAgreement> findExpirable(Instant now, int limit);

    void delete(UUID id);
}
