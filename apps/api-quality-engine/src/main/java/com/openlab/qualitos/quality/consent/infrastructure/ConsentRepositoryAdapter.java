package com.openlab.qualitos.quality.consent.infrastructure;

import com.openlab.qualitos.quality.consent.application.TenantProvider;
import com.openlab.qualitos.quality.consent.domain.Consent;
import com.openlab.qualitos.quality.consent.domain.ConsentRepository;
import com.openlab.qualitos.quality.consent.domain.ConsentStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConsentRepositoryAdapter implements ConsentRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final ConsentJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public ConsentRepositoryAdapter(ConsentJpaRepository jpa,
                                    @Qualifier("consentTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public Consent save(Consent consent) {
        // OWASP A01 — cross-tenant prevention.
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(consent.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        ConsentJpaEntity existing = consent.getId() != null
                ? jpa.findByIdAndTenantId(consent.getId(), currentTenant).orElse(null)
                : null;
        ConsentJpaEntity saved = jpa.save(ConsentMapper.toEntity(consent, existing));
        Consent out = ConsentMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<Consent> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(ConsentMapper::toDomain);
    }

    @Override
    public List<Consent> findByTenantAndSubjectHash(UUID tenantId, String hash) {
        return jpa.findByTenantIdAndSubjectIdentifierHash(tenantId, hash,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("grantedAt").descending()))
                .map(ConsentMapper::toDomain).getContent();
    }

    @Override
    public Optional<Consent> findLatestActiveByPurpose(UUID tenantId, String hash,
                                                       String purposeCode, Instant now) {
        List<ConsentJpaEntity> list = jpa.findActiveByPurpose(
                tenantId, hash, purposeCode, ConsentStatus.GRANTED, now,
                PageRequest.of(0, 1));
        return list.isEmpty() ? Optional.empty()
                : Optional.of(ConsentMapper.toDomain(list.get(0)));
    }

    @Override
    public List<Consent> findByTenantAndPurpose(UUID tenantId, String purposeCode) {
        return jpa.findByTenantIdAndPurposeCode(tenantId, purposeCode,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("grantedAt").descending()))
                .map(ConsentMapper::toDomain).getContent();
    }

    @Override
    public List<Consent> findExpirable(Instant now, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return jpa.findExpirable(ConsentStatus.GRANTED, now,
                        PageRequest.of(0, capped))
                .stream().map(ConsentMapper::toDomain).toList();
    }
}
