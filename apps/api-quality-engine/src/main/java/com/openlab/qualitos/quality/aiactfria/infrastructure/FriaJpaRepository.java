package com.openlab.qualitos.quality.aiactfria.infrastructure;

import com.openlab.qualitos.quality.aiactfria.domain.FriaStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FriaJpaRepository extends JpaRepository<FriaJpaEntity, UUID> {

    Optional<FriaJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<FriaJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<FriaJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, FriaStatus status, Pageable pageable);

    Page<FriaJpaEntity> findByTenantIdAndAiSystemId(
            UUID tenantId, UUID aiSystemId, Pageable pageable);

    Optional<FriaJpaEntity> findByTenantIdAndReference(UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
