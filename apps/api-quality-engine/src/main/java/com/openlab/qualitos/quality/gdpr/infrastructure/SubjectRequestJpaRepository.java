package com.openlab.qualitos.quality.gdpr.infrastructure;

import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRequestJpaRepository extends JpaRepository<SubjectRequestJpaEntity, UUID> {

    Optional<SubjectRequestJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<SubjectRequestJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<SubjectRequestJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, SubjectRequestStatus status, Pageable pageable);

    Page<SubjectRequestJpaEntity> findByTenantIdAndSubjectIdentifierHash(
            UUID tenantId, String subjectIdentifierHash, Pageable pageable);

    @Query("select e from SubjectRequestJpaEntity e " +
           "where e.tenantId = :tenantId and e.status in :statuses " +
           "and e.deadlineAt < :now order by e.deadlineAt asc")
    List<SubjectRequestJpaEntity> findOverdue(
            @Param("tenantId") UUID tenantId,
            @Param("statuses") List<SubjectRequestStatus> statuses,
            @Param("now") Instant now);
}
