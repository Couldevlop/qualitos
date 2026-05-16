package com.openlab.qualitos.quality.nis2measures.infrastructure;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Nis2RiskMeasureJpaRepository
        extends JpaRepository<Nis2RiskMeasureJpaEntity, UUID> {

    Optional<Nis2RiskMeasureJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<Nis2RiskMeasureJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Nis2RiskMeasureJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, Nis2MeasureStatus status, Pageable pageable);

    Page<Nis2RiskMeasureJpaEntity> findByTenantIdAndCategory(
            UUID tenantId, Nis2MeasureCategory category, Pageable pageable);

    Optional<Nis2RiskMeasureJpaEntity> findByTenantIdAndReference(
            UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    @Query("select e from Nis2RiskMeasureJpaEntity e " +
           "where e.nextReviewDueAt is not null " +
           "  and e.status <> :deprecated " +
           "  and e.nextReviewDueAt < :now " +
           "order by e.nextReviewDueAt asc")
    List<Nis2RiskMeasureJpaEntity> findReviewOverdue(
            @Param("deprecated") Nis2MeasureStatus deprecated,
            @Param("now") Instant now, Pageable pageable);
}
