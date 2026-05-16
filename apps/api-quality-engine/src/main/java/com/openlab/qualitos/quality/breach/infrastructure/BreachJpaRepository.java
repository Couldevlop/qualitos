package com.openlab.qualitos.quality.breach.infrastructure;

import com.openlab.qualitos.quality.breach.domain.BreachStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BreachJpaRepository extends JpaRepository<BreachJpaEntity, UUID> {

    Optional<BreachJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<BreachJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<BreachJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, BreachStatus status, Pageable pageable);

    boolean existsByTenantIdAndInternalReference(UUID tenantId, String internalReference);

    @Query("select e from BreachJpaEntity e " +
           "where e.dpaNotifiedAt is null " +
           "  and e.status not in :terminals " +
           "  and e.dpaDeadlineAt < :now " +
           "order by e.dpaDeadlineAt asc")
    List<BreachJpaEntity> findDpaOverdue(
            @Param("terminals") List<BreachStatus> terminals,
            @Param("now") Instant now,
            Pageable pageable);
}
