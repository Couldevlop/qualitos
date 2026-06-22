package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.normdoc.application.NormDocTenantProvider;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA du port {@link DossierRepository}. Filtre TOUJOURS par tenant
 * (issu du JWT) et refuse les écritures cross-tenant (OWASP A01).
 */
@Component
public class DossierRepositoryAdapter implements DossierRepository {

    private static final int MAX_PAGE_SIZE = 200;

    private final DossierJpaRepository jpa;
    private final NormDocTenantProvider tenantProvider;
    private final ObjectMapper objectMapper;

    public DossierRepositoryAdapter(
            DossierJpaRepository jpa,
            @Qualifier("normDocTenantContextProvider") NormDocTenantProvider tenantProvider,
            ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentationDossier save(DocumentationDossier dossier) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(dossier.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        DossierJpaEntity existing = dossier.getId() != null
                ? jpa.findByIdAndTenantId(dossier.getId(), currentTenant).orElse(null)
                : null;
        DossierJpaEntity saved = jpa.save(DossierMapper.toEntity(dossier, existing, objectMapper));
        DocumentationDossier out = DossierMapper.toDomain(saved, objectMapper);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<DocumentationDossier> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant)
                .map(e -> DossierMapper.toDomain(e, objectMapper));
    }

    @Override
    public List<DocumentationDossier> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .stream().map(e -> DossierMapper.toDomain(e, objectMapper)).toList();
    }
}
