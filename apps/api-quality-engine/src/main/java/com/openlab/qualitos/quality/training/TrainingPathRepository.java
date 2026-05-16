package com.openlab.qualitos.quality.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainingPathRepository extends JpaRepository<TrainingPath, UUID> {

    Optional<TrainingPath> findByTenantIdAndCode(UUID tenantId, String code);

    Page<TrainingPath> findByTenantId(UUID tenantId, Pageable pageable);

    Page<TrainingPath> findByTenantIdAndStatus(UUID tenantId, TrainingPathStatus status, Pageable pageable);

    Page<TrainingPath> findByTenantIdAndTargetRole(UUID tenantId, String targetRole, Pageable pageable);
}
