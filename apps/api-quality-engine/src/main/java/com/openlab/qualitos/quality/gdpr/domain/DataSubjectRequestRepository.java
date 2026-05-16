package com.openlab.qualitos.quality.gdpr.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataSubjectRequestRepository {

    DataSubjectRequest save(DataSubjectRequest request);

    Optional<DataSubjectRequest> findById(UUID id);

    /** Recherche par hash → permet à un opérateur de retrouver les demandes
     *  d'une même personne sans stocker la PII. */
    List<DataSubjectRequest> findByTenantIdAndSubjectIdentifierHash(
            UUID tenantId, String subjectIdentifierHash);

    List<DataSubjectRequest> findByTenantId(UUID tenantId);

    List<DataSubjectRequest> findByTenantIdAndStatus(UUID tenantId, SubjectRequestStatus status);

    /** Non-terminales dont la deadline est passée — scheduler reporting. */
    List<DataSubjectRequest> findOverdue(Instant now, int limit);
}
