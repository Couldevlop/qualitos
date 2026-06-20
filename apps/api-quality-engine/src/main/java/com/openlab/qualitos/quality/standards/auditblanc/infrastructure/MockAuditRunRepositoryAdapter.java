package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditTenantProvider;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA du port {@link MockAuditRunRepository}. Filtre TOUJOURS par tenant
 * (issu du JWT) et refuse les écritures cross-tenant (OWASP A01).
 */
@Component
public class MockAuditRunRepositoryAdapter implements MockAuditRunRepository {

    private static final int MAX_PAGE_SIZE = 200;

    private final MockAuditRunJpaRepository jpa;
    private final MockAuditTenantProvider tenantProvider;
    private final ObjectMapper objectMapper;

    public MockAuditRunRepositoryAdapter(
            MockAuditRunJpaRepository jpa,
            @Qualifier("mockAuditTenantContextProvider") MockAuditTenantProvider tenantProvider,
            ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public MockAuditRun save(MockAuditRun run) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(run.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        MockAuditRunJpaEntity saved = jpa.save(MockAuditRunMapper.toEntity(run, objectMapper));
        MockAuditRun out = MockAuditRunMapper.toDomain(saved, objectMapper);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<MockAuditRun> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant)
                .map(e -> MockAuditRunMapper.toDomain(e, objectMapper));
    }

    @Override
    public List<MockAuditRun> findByAdoption(UUID adoptionId) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByTenantIdAndAdoptionId(tenant, adoptionId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .stream().map(e -> MockAuditRunMapper.toDomain(e, objectMapper)).toList();
    }
}
