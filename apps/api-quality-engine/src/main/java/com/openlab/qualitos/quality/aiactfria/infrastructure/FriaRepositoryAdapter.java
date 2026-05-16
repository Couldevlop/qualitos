package com.openlab.qualitos.quality.aiactfria.infrastructure;

import com.openlab.qualitos.quality.aiactfria.application.TenantProvider;
import com.openlab.qualitos.quality.aiactfria.domain.Fria;
import com.openlab.qualitos.quality.aiactfria.domain.FriaRepository;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FriaRepositoryAdapter implements FriaRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final FriaJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public FriaRepositoryAdapter(
            FriaJpaRepository jpa,
            @Qualifier("friaTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public Fria save(Fria fria) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(fria.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        FriaJpaEntity existing = fria.getId() != null
                ? jpa.findByIdAndTenantId(fria.getId(), currentTenant).orElse(null)
                : null;
        FriaJpaEntity saved = jpa.save(FriaMapper.toEntity(fria, existing));
        Fria out = FriaMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<Fria> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(FriaMapper::toDomain);
    }

    @Override
    public List<Fria> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(FriaMapper::toDomain).getContent();
    }

    @Override
    public List<Fria> findByTenantAndStatus(UUID tenantId, FriaStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(FriaMapper::toDomain).getContent();
    }

    @Override
    public List<Fria> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId) {
        return jpa.findByTenantIdAndAiSystemId(tenantId, aiSystemId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(FriaMapper::toDomain).getContent();
    }

    @Override
    public Optional<Fria> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference).map(FriaMapper::toDomain);
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
