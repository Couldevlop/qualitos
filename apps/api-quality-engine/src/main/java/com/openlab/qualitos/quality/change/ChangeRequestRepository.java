package com.openlab.qualitos.quality.change;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, UUID> {

    Optional<ChangeRequest> findByTenantIdAndCode(UUID tenantId, String code);

    Page<ChangeRequest> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ChangeRequest> findByTenantIdAndStatus(UUID tenantId, ChangeRequestStatus status, Pageable pageable);

    Page<ChangeRequest> findByTenantIdAndType(UUID tenantId, ChangeRequestType type, Pageable pageable);
}
