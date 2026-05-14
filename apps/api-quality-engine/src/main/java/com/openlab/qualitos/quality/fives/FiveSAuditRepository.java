package com.openlab.qualitos.quality.fives;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FiveSAuditRepository extends JpaRepository<FiveSAudit, UUID> {

    Page<FiveSAudit> findByTenantId(UUID tenantId, Pageable pageable);

    Page<FiveSAudit> findByTenantIdAndStatus(UUID tenantId, FiveSAuditStatus status, Pageable pageable);

    Optional<FiveSAudit> findByIdAndTenantId(UUID id, UUID tenantId);
}
