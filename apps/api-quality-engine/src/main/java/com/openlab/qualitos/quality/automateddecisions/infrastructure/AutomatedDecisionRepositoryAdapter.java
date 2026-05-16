package com.openlab.qualitos.quality.automateddecisions.infrastructure;

import com.openlab.qualitos.quality.automateddecisions.application.TenantProvider;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRepository;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AutomatedDecisionRepositoryAdapter implements AutomatedDecisionRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final AutomatedDecisionJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public AutomatedDecisionRepositoryAdapter(
            AutomatedDecisionJpaRepository jpa,
            @Qualifier("admTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public AutomatedDecisionRecord save(AutomatedDecisionRecord record) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(record.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        AutomatedDecisionJpaEntity existing = record.getId() != null
                ? jpa.findByIdAndTenantId(record.getId(), currentTenant).orElse(null)
                : null;
        AutomatedDecisionJpaEntity saved = jpa.save(
                AutomatedDecisionMapper.toEntity(record, existing));
        AutomatedDecisionRecord out = AutomatedDecisionMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<AutomatedDecisionRecord> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(AutomatedDecisionMapper::toDomain);
    }

    @Override
    public List<AutomatedDecisionRecord> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AutomatedDecisionMapper::toDomain).getContent();
    }

    @Override
    public List<AutomatedDecisionRecord> findByTenantAndStatus(UUID tenantId,
                                                               AutomatedDecisionStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AutomatedDecisionMapper::toDomain).getContent();
    }

    @Override
    public Optional<AutomatedDecisionRecord> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(AutomatedDecisionMapper::toDomain);
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
