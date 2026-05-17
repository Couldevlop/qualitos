package com.openlab.qualitos.quality.aiconformity.infrastructure;

import com.openlab.qualitos.quality.aiconformity.application.TenantProvider;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentRepository;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConformityAssessmentRepositoryAdapter implements ConformityAssessmentRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final ConformityAssessmentJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public ConformityAssessmentRepositoryAdapter(
            ConformityAssessmentJpaRepository jpa,
            @Qualifier("aicaTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public ConformityAssessment save(ConformityAssessment assessment) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(assessment.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        ConformityAssessmentJpaEntity existing = assessment.getId() != null
                ? jpa.findByIdAndTenantId(assessment.getId(), currentTenant).orElse(null)
                : null;
        ConformityAssessmentJpaEntity saved = jpa.save(
                ConformityAssessmentMapper.toEntity(assessment, existing));
        ConformityAssessment out = ConformityAssessmentMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<ConformityAssessment> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(ConformityAssessmentMapper::toDomain);
    }

    @Override
    public List<ConformityAssessment> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(ConformityAssessmentMapper::toDomain).getContent();
    }

    @Override
    public List<ConformityAssessment> findByTenantAndStatus(
            UUID tenantId, ConformityAssessmentStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(ConformityAssessmentMapper::toDomain).getContent();
    }

    @Override
    public List<ConformityAssessment> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId) {
        return jpa.findByTenantIdAndAiSystemId(tenantId, aiSystemId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(ConformityAssessmentMapper::toDomain).getContent();
    }

    @Override
    public List<ConformityAssessment> findExpiringCertificates(
            UUID tenantId, Instant now, int limit) {
        return jpa.findExpiringCertificates(tenantId, now, PageRequest.of(0, limit)).stream()
                .map(ConformityAssessmentMapper::toDomain).toList();
    }

    @Override
    public Optional<ConformityAssessment> findByTenantAndReference(
            UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(ConformityAssessmentMapper::toDomain);
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
