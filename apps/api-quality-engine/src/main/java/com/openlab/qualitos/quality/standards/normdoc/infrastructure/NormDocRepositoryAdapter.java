package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA du port {@link NormDocRepository}. Filtre TOUJOURS par tenant
 * (issu du JWT) et refuse les écritures cross-tenant (OWASP A01).
 */
@Component
public class NormDocRepositoryAdapter implements NormDocRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final NormDocJpaRepository jpa;
    private final NormDocTenantProvider tenantProvider;
    private final ObjectMapper objectMapper;

    public NormDocRepositoryAdapter(
            NormDocJpaRepository jpa,
            @Qualifier("normDocTenantContextProvider") NormDocTenantProvider tenantProvider,
            ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public NormativeDocument save(NormativeDocument doc) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(doc.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        NormDocJpaEntity existing = doc.getId() != null
                ? jpa.findByIdAndTenantId(doc.getId(), currentTenant).orElse(null)
                : null;
        NormDocJpaEntity saved = jpa.save(NormDocMapper.toEntity(doc, existing, objectMapper));
        NormativeDocument out = NormDocMapper.toDomain(saved, objectMapper);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<NormativeDocument> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant)
                .map(e -> NormDocMapper.toDomain(e, objectMapper));
    }

    @Override
    public List<NormativeDocument> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .stream().map(e -> NormDocMapper.toDomain(e, objectMapper)).toList();
    }

    @Override
    public List<NormativeDocument> findByTenantAndStatus(UUID tenantId, NormDocStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .stream().map(e -> NormDocMapper.toDomain(e, objectMapper)).toList();
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
