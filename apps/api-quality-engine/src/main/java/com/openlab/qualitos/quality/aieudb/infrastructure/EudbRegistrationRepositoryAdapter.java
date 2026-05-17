package com.openlab.qualitos.quality.aieudb.infrastructure;

import com.openlab.qualitos.quality.aieudb.application.TenantProvider;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationRepository;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EudbRegistrationRepositoryAdapter implements EudbRegistrationRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final EudbRegistrationJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public EudbRegistrationRepositoryAdapter(
            EudbRegistrationJpaRepository jpa,
            @Qualifier("eudbTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public EudbRegistration save(EudbRegistration registration) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(registration.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        EudbRegistrationJpaEntity existing = registration.getId() != null
                ? jpa.findByIdAndTenantId(registration.getId(), currentTenant).orElse(null)
                : null;
        EudbRegistrationJpaEntity saved = jpa.save(
                EudbRegistrationMapper.toEntity(registration, existing));
        EudbRegistration out = EudbRegistrationMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<EudbRegistration> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(EudbRegistrationMapper::toDomain);
    }

    @Override
    public List<EudbRegistration> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(EudbRegistrationMapper::toDomain).getContent();
    }

    @Override
    public List<EudbRegistration> findByTenantAndStatus(UUID tenantId, EudbRegistrationStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(EudbRegistrationMapper::toDomain).getContent();
    }

    @Override
    public List<EudbRegistration> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId) {
        return jpa.findByTenantIdAndAiSystemId(tenantId, aiSystemId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(EudbRegistrationMapper::toDomain).getContent();
    }

    @Override
    public Optional<EudbRegistration> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(EudbRegistrationMapper::toDomain);
    }

    @Override
    public Optional<EudbRegistration> findByTenantAndEudbId(UUID tenantId, String eudbId) {
        return jpa.findByTenantIdAndEudbId(tenantId, eudbId)
                .map(EudbRegistrationMapper::toDomain);
    }

    @Override
    public boolean existsByTenantAndReference(UUID tenantId, String reference) {
        return jpa.existsByTenantIdAndReference(tenantId, reference);
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
