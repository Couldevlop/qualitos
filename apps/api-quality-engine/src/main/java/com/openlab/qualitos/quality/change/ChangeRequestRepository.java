package com.openlab.qualitos.quality.change;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {

    Optional<ChangeRequest> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    List<ChangeRequest> findByTenantId(UUID tenantId, Sort sort);

    List<ChangeRequest> findByTenantIdAndStatusIn(UUID tenantId, Collection<ChangeRequestStatus> statuses);

    /** Les changements dans l'un des états donnés, prévus au plus tard le jour dit — le plus proche d'abord. */
    List<ChangeRequest> findByTenantIdAndStatusInAndPlannedForLessThanEqualOrderByPlannedForAsc(
            UUID tenantId, Collection<ChangeRequestStatus> statuses, LocalDate until, Pageable pageable);
}
