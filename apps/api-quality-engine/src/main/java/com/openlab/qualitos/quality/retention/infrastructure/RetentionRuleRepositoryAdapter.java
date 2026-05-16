package com.openlab.qualitos.quality.retention.infrastructure;

import com.openlab.qualitos.quality.retention.application.TenantProvider;
import com.openlab.qualitos.quality.retention.domain.RetentionRule;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleRepository;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RetentionRuleRepositoryAdapter implements RetentionRuleRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final RetentionRuleJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public RetentionRuleRepositoryAdapter(RetentionRuleJpaRepository jpa,
                                          @Qualifier("retentionTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public RetentionRule save(RetentionRule rule) {
        UUID currentTenant = tenantProvider.requireTenantId();
        // OWASP A01 — cross-tenant prevention.
        if (!currentTenant.equals(rule.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        RetentionRuleJpaEntity existing = rule.getId() != null
                ? jpa.findByIdAndTenantId(rule.getId(), currentTenant).orElse(null)
                : null;
        RetentionRuleJpaEntity saved = jpa.save(RetentionRuleMapper.toEntity(rule, existing));
        RetentionRule out = RetentionRuleMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<RetentionRule> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(RetentionRuleMapper::toDomain);
    }

    @Override
    public List<RetentionRule> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(RetentionRuleMapper::toDomain).getContent();
    }

    @Override
    public List<RetentionRule> findByTenantAndStatus(UUID tenantId, RetentionRuleStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(RetentionRuleMapper::toDomain).getContent();
    }

    @Override
    public Optional<RetentionRule> findActiveByCategory(UUID tenantId, String dataCategoryCode) {
        return jpa.findByTenantIdAndDataCategoryCodeAndStatus(
                tenantId, dataCategoryCode, RetentionRuleStatus.ACTIVE)
                .map(RetentionRuleMapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
