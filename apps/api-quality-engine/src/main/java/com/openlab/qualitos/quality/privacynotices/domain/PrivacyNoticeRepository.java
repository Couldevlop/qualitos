package com.openlab.qualitos.quality.privacynotices.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivacyNoticeRepository {

    PrivacyNotice save(PrivacyNotice notice);

    Optional<PrivacyNotice> findById(UUID id);

    List<PrivacyNotice> findByTenant(UUID tenantId);

    List<PrivacyNotice> findByTenantAndStatus(UUID tenantId, PrivacyNoticeStatus status);

    /** Notice PUBLISHED pour cette (tenant, reference, language) — au plus une. */
    Optional<PrivacyNotice> findPublishedByReferenceAndLanguage(
            UUID tenantId, String reference, String language);

    boolean existsByTenantAndReferenceAndVersionAndLanguage(
            UUID tenantId, String reference, String version, String language);

    List<PrivacyNotice> findVersionsByReference(UUID tenantId, String reference);

    void delete(UUID id);
}
