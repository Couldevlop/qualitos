package com.openlab.qualitos.quality.cyberincidents.infrastructure;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CyberIncidentJpaRepository extends JpaRepository<CyberIncidentJpaEntity, UUID> {

    Optional<CyberIncidentJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<CyberIncidentJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<CyberIncidentJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, CyberIncidentStatus status, Pageable pageable);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    Optional<CyberIncidentJpaEntity> findByTenantIdAndReference(UUID tenantId, String reference);

    @Query("select e from CyberIncidentJpaEntity e " +
           "where e.earlyWarningSentAt is null " +
           "  and e.status not in :terminals " +
           "  and e.earlyWarningDeadlineAt < :now " +
           "order by e.earlyWarningDeadlineAt asc")
    List<CyberIncidentJpaEntity> findEarlyWarningOverdue(
            @Param("terminals") List<CyberIncidentStatus> terminals,
            @Param("now") Instant now, Pageable pageable);

    @Query("select e from CyberIncidentJpaEntity e " +
           "where e.initialAssessmentSentAt is null " +
           "  and e.status not in :terminals " +
           "  and e.initialAssessmentDeadlineAt < :now " +
           "order by e.initialAssessmentDeadlineAt asc")
    List<CyberIncidentJpaEntity> findInitialAssessmentOverdue(
            @Param("terminals") List<CyberIncidentStatus> terminals,
            @Param("now") Instant now, Pageable pageable);

    @Query("select e from CyberIncidentJpaEntity e " +
           "where e.finalReportSentAt is null " +
           "  and e.status not in :terminals " +
           "  and e.finalReportDeadlineAt < :now " +
           "order by e.finalReportDeadlineAt asc")
    List<CyberIncidentJpaEntity> findFinalReportOverdue(
            @Param("terminals") List<CyberIncidentStatus> terminals,
            @Param("now") Instant now, Pageable pageable);
}
