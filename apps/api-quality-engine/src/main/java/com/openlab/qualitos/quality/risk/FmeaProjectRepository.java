package com.openlab.qualitos.quality.risk;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FmeaProjectRepository extends JpaRepository<FmeaProject, UUID> {

    Optional<FmeaProject> findByTenantIdAndCode(UUID tenantId, String code);

    Page<FmeaProject> findByTenantId(UUID tenantId, Pageable pageable);

    Page<FmeaProject> findByTenantIdAndStatus(UUID tenantId, FmeaStatus status, Pageable pageable);

    Page<FmeaProject> findByTenantIdAndType(UUID tenantId, FmeaType type, Pageable pageable);
}
