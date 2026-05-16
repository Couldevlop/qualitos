package com.openlab.qualitos.quality.gdpr.infrastructure;

import com.openlab.qualitos.quality.gdpr.application.TenantProvider;
import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequest;
import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequestRepository;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SubjectRequestRepositoryAdapter implements DataSubjectRequestRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final SubjectRequestJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public SubjectRequestRepositoryAdapter(SubjectRequestJpaRepository jpa,
                                           TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public DataSubjectRequest save(DataSubjectRequest request) {
        // OWASP A01 — cross-tenant access prevention : on contraint au tenant courant
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(request.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        SubjectRequestJpaEntity existing = request.getId() != null
                ? jpa.findByIdAndTenantId(request.getId(), currentTenant).orElse(null)
                : null;
        SubjectRequestJpaEntity e = SubjectRequestMapper.toEntity(request, existing);
        SubjectRequestJpaEntity saved = jpa.save(e);
        DataSubjectRequest out = SubjectRequestMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<DataSubjectRequest> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(SubjectRequestMapper::toDomain);
    }

    @Override
    public List<DataSubjectRequest> findByTenantIdAndSubjectIdentifierHash(
            UUID tenantId, String subjectIdentifierHash) {
        return jpa.findByTenantIdAndSubjectIdentifierHash(
                        tenantId, subjectIdentifierHash,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("receivedAt").descending()))
                .map(SubjectRequestMapper::toDomain)
                .getContent();
    }

    @Override
    public List<DataSubjectRequest> findByTenantId(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("receivedAt").descending()))
                .map(SubjectRequestMapper::toDomain)
                .getContent();
    }

    @Override
    public List<DataSubjectRequest> findByTenantIdAndStatus(UUID tenantId, SubjectRequestStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("receivedAt").descending()))
                .map(SubjectRequestMapper::toDomain)
                .getContent();
    }

    @Override
    public List<DataSubjectRequest> findOverdue(Instant now, int limit) {
        UUID tenant = tenantProvider.requireTenantId();
        int capped = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return jpa.findOverdue(tenant,
                        List.of(SubjectRequestStatus.RECEIVED, SubjectRequestStatus.IN_PROGRESS),
                        now)
                .stream()
                .limit(capped)
                .map(SubjectRequestMapper::toDomain)
                .toList();
    }
}
