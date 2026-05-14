package com.openlab.qualitos.quality.pdca;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PdcaCycleRepository extends JpaRepository<PdcaCycle, UUID> {

    Page<PdcaCycle> findByTenantId(UUID tenantId, Pageable pageable);

    Page<PdcaCycle> findByTenantIdAndStatus(UUID tenantId, PdcaStatus status, Pageable pageable);

    Optional<PdcaCycle> findByIdAndTenantId(UUID id, UUID tenantId);
}
