package com.openlab.qualitos.quality.retention.infrastructure;

import com.openlab.qualitos.quality.retention.domain.RetentionRuleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RetentionRuleJpaRepository extends JpaRepository<RetentionRuleJpaEntity, UUID> {

    Optional<RetentionRuleJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<RetentionRuleJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<RetentionRuleJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, RetentionRuleStatus status, Pageable pageable);

    Optional<RetentionRuleJpaEntity> findByTenantIdAndDataCategoryCodeAndStatus(
            UUID tenantId, String dataCategoryCode, RetentionRuleStatus status);
}
