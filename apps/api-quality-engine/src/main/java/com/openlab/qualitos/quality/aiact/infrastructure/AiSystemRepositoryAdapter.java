package com.openlab.qualitos.quality.aiact.infrastructure;

import com.openlab.qualitos.quality.aiact.application.TenantProvider;
import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystem;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRepository;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AiSystemRepositoryAdapter implements AiSystemRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final AiSystemJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public AiSystemRepositoryAdapter(
            AiSystemJpaRepository jpa,
            @Qualifier("aisTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public AiSystem save(AiSystem system) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(system.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        AiSystemJpaEntity existing = system.getId() != null
                ? jpa.findByIdAndTenantId(system.getId(), currentTenant).orElse(null)
                : null;
        AiSystemJpaEntity saved = jpa.save(AiSystemMapper.toEntity(system, existing));
        AiSystem out = AiSystemMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<AiSystem> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(AiSystemMapper::toDomain);
    }

    @Override
    public List<AiSystem> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiSystemMapper::toDomain).getContent();
    }

    @Override
    public List<AiSystem> findByTenantAndStatus(UUID tenantId, AiSystemStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiSystemMapper::toDomain).getContent();
    }

    @Override
    public List<AiSystem> findByTenantAndRiskClassification(UUID tenantId,
                                                            AiRiskClassification risk) {
        return jpa.findByTenantIdAndRiskClassification(tenantId, risk,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiSystemMapper::toDomain).getContent();
    }

    @Override
    public Optional<AiSystem> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(AiSystemMapper::toDomain);
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
