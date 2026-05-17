package com.openlab.qualitos.quality.aiqms.infrastructure;

import com.openlab.qualitos.quality.aiqms.application.TenantProvider;
import com.openlab.qualitos.quality.aiqms.domain.AiQms;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsRepository;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AiQmsRepositoryAdapter implements AiQmsRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final AiQmsJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public AiQmsRepositoryAdapter(
            AiQmsJpaRepository jpa,
            @Qualifier("aqmsTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public AiQms save(AiQms qms) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(qms.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        AiQmsJpaEntity existing = qms.getId() != null
                ? jpa.findByIdAndTenantId(qms.getId(), currentTenant).orElse(null)
                : null;
        AiQmsJpaEntity saved = jpa.save(AiQmsMapper.toEntity(qms, existing));
        AiQms out = AiQmsMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<AiQms> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(AiQmsMapper::toDomain);
    }

    @Override
    public List<AiQms> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiQmsMapper::toDomain).getContent();
    }

    @Override
    public List<AiQms> findByTenantAndStatus(UUID tenantId, AiQmsStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiQmsMapper::toDomain).getContent();
    }

    @Override
    public Optional<AiQms> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReferenceOrderByCreatedAtDesc(tenantId, reference)
                .stream().findFirst().map(AiQmsMapper::toDomain);
    }

    @Override
    public boolean existsByTenantAndReferenceAndVersion(
            UUID tenantId, String reference, String version) {
        return jpa.existsByTenantIdAndReferenceAndVersion(tenantId, reference, version);
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
