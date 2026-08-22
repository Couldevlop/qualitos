package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RevisionRequestJpaRepository extends JpaRepository<RevisionRequestJpaEntity, UUID> {

    List<RevisionRequestJpaEntity> findByTenantIdAndProductIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, UUID productId, String status);

    Optional<RevisionRequestJpaEntity> findByTenantIdAndTargetTypeAndTargetIdAndStatus(
            UUID tenantId, String targetType, UUID targetId, String status);

    List<RevisionRequestJpaEntity> findByTenantIdAndTriggerRefIdOrderByCreatedAtDesc(
            UUID tenantId, UUID triggerRefId);

    int countByTenantIdAndProductIdAndStatus(UUID tenantId, UUID productId, String status);

    Optional<RevisionRequestJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
