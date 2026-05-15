package com.openlab.qualitos.quality.dmaic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DmaicProjectRepository extends JpaRepository<DmaicProject, UUID> {

    Page<DmaicProject> findByTenantId(UUID tenantId, Pageable pageable);

    Page<DmaicProject> findByTenantIdAndStatus(UUID tenantId, DmaicStatus status, Pageable pageable);

    Page<DmaicProject> findByTenantIdAndPhase(UUID tenantId, DmaicPhase phase, Pageable pageable);

    Optional<DmaicProject> findByIdAndTenantId(UUID id, UUID tenantId);
}
