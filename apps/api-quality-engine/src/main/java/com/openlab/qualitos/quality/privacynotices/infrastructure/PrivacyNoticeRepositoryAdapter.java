package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.privacynotices.application.TenantProvider;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeRepository;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PrivacyNoticeRepositoryAdapter implements PrivacyNoticeRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final PrivacyNoticeJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public PrivacyNoticeRepositoryAdapter(
            PrivacyNoticeJpaRepository jpa,
            @Qualifier("privacyNoticesTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public PrivacyNotice save(PrivacyNotice notice) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(notice.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        PrivacyNoticeJpaEntity existing = notice.getId() != null
                ? jpa.findByIdAndTenantId(notice.getId(), currentTenant).orElse(null)
                : null;
        PrivacyNoticeJpaEntity saved = jpa.save(PrivacyNoticeMapper.toEntity(notice, existing));
        PrivacyNotice out = PrivacyNoticeMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<PrivacyNotice> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(PrivacyNoticeMapper::toDomain);
    }

    @Override
    public List<PrivacyNotice> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(PrivacyNoticeMapper::toDomain).getContent();
    }

    @Override
    public List<PrivacyNotice> findByTenantAndStatus(UUID tenantId, PrivacyNoticeStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(PrivacyNoticeMapper::toDomain).getContent();
    }

    @Override
    public Optional<PrivacyNotice> findPublishedByReferenceAndLanguage(
            UUID tenantId, String reference, String language) {
        return jpa.findByTenantIdAndReferenceAndLanguageAndStatus(
                tenantId, reference, language, PrivacyNoticeStatus.PUBLISHED)
                .map(PrivacyNoticeMapper::toDomain);
    }

    @Override
    public boolean existsByTenantAndReferenceAndVersionAndLanguage(
            UUID tenantId, String reference, String version, String language) {
        return jpa.existsByTenantIdAndReferenceAndVersionAndLanguage(
                tenantId, reference, version, language);
    }

    @Override
    public List<PrivacyNotice> findVersionsByReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReferenceOrderByCreatedAtDesc(tenantId, reference)
                .stream().map(PrivacyNoticeMapper::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
