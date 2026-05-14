package com.openlab.qualitos.quality.circle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QualityCircleRepository extends JpaRepository<QualityCircle, UUID> {

    Page<QualityCircle> findByTenantId(UUID tenantId, Pageable pageable);

    Page<QualityCircle> findByTenantIdAndStatus(UUID tenantId, CircleStatus status, Pageable pageable);

    Optional<QualityCircle> findByIdAndTenantId(UUID id, UUID tenantId);
}
