package com.openlab.qualitos.quality.consent.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository {

    Consent save(Consent consent);

    Optional<Consent> findById(UUID id);

    /** Tous les consentements d'un sujet (par hash) — Art. 15 lookup. */
    List<Consent> findByTenantAndSubjectHash(UUID tenantId, String subjectIdentifierHash);

    /** Dernier consentement actif (status=GRANTED, non expiré) pour ce sujet & cette finalité. */
    Optional<Consent> findLatestActiveByPurpose(UUID tenantId,
                                                String subjectIdentifierHash,
                                                String purposeCode,
                                                Instant now);

    List<Consent> findByTenantAndPurpose(UUID tenantId, String purposeCode);

    /** Consentements GRANTED dont expiresAt est dépassé — scan d'expiration. */
    List<Consent> findExpirable(Instant now, int limit);
}
