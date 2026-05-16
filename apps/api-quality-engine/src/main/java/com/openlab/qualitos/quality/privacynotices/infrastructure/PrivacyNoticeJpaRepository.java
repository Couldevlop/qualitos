package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivacyNoticeJpaRepository
        extends JpaRepository<PrivacyNoticeJpaEntity, UUID> {

    Optional<PrivacyNoticeJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<PrivacyNoticeJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<PrivacyNoticeJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, PrivacyNoticeStatus status, Pageable pageable);

    Optional<PrivacyNoticeJpaEntity> findByTenantIdAndReferenceAndLanguageAndStatus(
            UUID tenantId, String reference, String language, PrivacyNoticeStatus status);

    boolean existsByTenantIdAndReferenceAndVersionAndLanguage(
            UUID tenantId, String reference, String version, String language);

    List<PrivacyNoticeJpaEntity> findByTenantIdAndReferenceOrderByCreatedAtDesc(
            UUID tenantId, String reference);
}
