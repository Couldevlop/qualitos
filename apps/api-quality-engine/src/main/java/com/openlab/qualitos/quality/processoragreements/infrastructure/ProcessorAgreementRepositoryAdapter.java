package com.openlab.qualitos.quality.processoragreements.infrastructure;

import com.openlab.qualitos.quality.processoragreements.application.TenantProvider;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementRepository;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProcessorAgreementRepositoryAdapter implements ProcessorAgreementRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final ProcessorAgreementJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public ProcessorAgreementRepositoryAdapter(
            ProcessorAgreementJpaRepository jpa,
            @Qualifier("dpaTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public ProcessorAgreement save(ProcessorAgreement agreement) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(agreement.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        ProcessorAgreementJpaEntity existing = agreement.getId() != null
                ? jpa.findByIdAndTenantId(agreement.getId(), currentTenant).orElse(null)
                : null;
        ProcessorAgreementJpaEntity saved = jpa.save(
                ProcessorAgreementMapper.toEntity(agreement, existing));
        ProcessorAgreement out = ProcessorAgreementMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<ProcessorAgreement> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(ProcessorAgreementMapper::toDomain);
    }

    @Override
    public List<ProcessorAgreement> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(ProcessorAgreementMapper::toDomain).getContent();
    }

    @Override
    public List<ProcessorAgreement> findByTenantAndStatus(UUID tenantId,
                                                          ProcessorAgreementStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(ProcessorAgreementMapper::toDomain).getContent();
    }

    @Override
    public Optional<ProcessorAgreement> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(ProcessorAgreementMapper::toDomain);
    }

    @Override
    public boolean existsByTenantAndReference(UUID tenantId, String reference) {
        return jpa.existsByTenantIdAndReference(tenantId, reference);
    }

    @Override
    public List<ProcessorAgreement> findExpirable(Instant now, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return jpa.findExpirable(ProcessorAgreementStatus.ACTIVE, now,
                        PageRequest.of(0, capped))
                .stream().map(ProcessorAgreementMapper::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
