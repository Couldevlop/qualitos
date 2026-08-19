package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.revisionrequests.application.TenantProvider;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestRepository;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStatus;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptateur de persistance.
 *
 * <p>L'écriture prend son tenant de l'agrégat, pas du contexte : elle est
 * déclenchée après commit par un écouteur d'événements, à un moment où lire le
 * fil d'exécution serait fragile. La lecture par identifiant, elle, sert la voie
 * HTTP et filtre bien sur le tenant courant.
 */
@Component
public class RevisionRequestRepositoryAdapter implements RevisionRequestRepository {

    private final RevisionRequestJpaRepository jpa;
    private final TenantProvider tenants;

    public RevisionRequestRepositoryAdapter(
            RevisionRequestJpaRepository jpa,
            @Qualifier("revisionRequestTenantContextProvider") TenantProvider tenants) {
        this.jpa = jpa;
        this.tenants = tenants;
    }

    @Override
    public RevisionRequest save(RevisionRequest request) {
        RevisionRequestJpaEntity existing = request.getId() != null
                ? jpa.findByIdAndTenantId(request.getId(), request.getTenantId()).orElse(null)
                : null;
        RevisionRequestJpaEntity saved = jpa.save(RevisionRequestMapper.toEntity(request, existing));
        request.assignId(saved.getId());
        return RevisionRequestMapper.toDomain(saved);
    }

    @Override
    public Optional<RevisionRequest> findById(UUID id) {
        return jpa.findByIdAndTenantId(id, tenants.requireTenantId())
                .map(RevisionRequestMapper::toDomain);
    }

    @Override
    public List<RevisionRequest> findPendingByProduct(UUID tenantId, UUID productId) {
        return jpa.findByTenantIdAndProductIdAndStatusOrderByCreatedAtDesc(
                        tenantId, productId, RevisionRequestStatus.PENDING.name()).stream()
                .map(RevisionRequestMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RevisionRequest> findPendingForTarget(UUID tenantId, RevisionTargetType type,
                                                          UUID targetId) {
        return jpa.findByTenantIdAndTargetTypeAndTargetIdAndStatus(
                        tenantId, type.name(), targetId, RevisionRequestStatus.PENDING.name())
                .map(RevisionRequestMapper::toDomain);
    }

    @Override
    public List<RevisionRequest> findByTrigger(UUID tenantId, UUID triggerRefId) {
        return jpa.findByTenantIdAndTriggerRefIdOrderByCreatedAtDesc(tenantId, triggerRefId).stream()
                .map(RevisionRequestMapper::toDomain)
                .toList();
    }

    @Override
    public int countPendingByProduct(UUID tenantId, UUID productId) {
        return jpa.countByTenantIdAndProductIdAndStatus(
                tenantId, productId, RevisionRequestStatus.PENDING.name());
    }
}
