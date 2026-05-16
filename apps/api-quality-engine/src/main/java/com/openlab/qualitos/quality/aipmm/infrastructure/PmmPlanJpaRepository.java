package com.openlab.qualitos.quality.aipmm.infrastructure;

import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PmmPlanJpaRepository extends JpaRepository<PmmPlanJpaEntity, UUID> {

    Optional<PmmPlanJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<PmmPlanJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<PmmPlanJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, PmmPlanStatus status, Pageable pageable);

    Page<PmmPlanJpaEntity> findByTenantIdAndAiSystemId(
            UUID tenantId, UUID aiSystemId, Pageable pageable);

    Optional<PmmPlanJpaEntity> findByTenantIdAndReference(UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    /**
     * Plans actifs avec prochaine revue dépassée (deadline = base + INTERVAL).
     * base = COALESCE(last_reviewed_at, activated_at).
     * Index partiel {@code idx_pmm_active_review} sert le scan.
     */
    @Query(value = """
            SELECT * FROM ai_act_pmm_plans
             WHERE tenant_id = :tenantId
               AND status = 'ACTIVE'
               AND review_frequency IS NOT NULL
               AND (COALESCE(last_reviewed_at, activated_at) +
                    CASE review_frequency
                        WHEN 'WEEKLY'      THEN INTERVAL '7 days'
                        WHEN 'MONTHLY'     THEN INTERVAL '30 days'
                        WHEN 'QUARTERLY'   THEN INTERVAL '90 days'
                        WHEN 'SEMI_ANNUAL' THEN INTERVAL '182 days'
                        WHEN 'ANNUAL'      THEN INTERVAL '365 days'
                    END) < :now
             ORDER BY COALESCE(last_reviewed_at, activated_at) ASC
             LIMIT :limit
            """, nativeQuery = true)
    List<PmmPlanJpaEntity> findOverdueReviews(
            @Param("tenantId") UUID tenantId,
            @Param("now") Instant now,
            @Param("limit") int limit);
}
