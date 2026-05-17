package com.openlab.qualitos.quality.aiqms.infrastructure;

import com.openlab.qualitos.quality.aiqms.domain.AiQmsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiQmsJpaRepository extends JpaRepository<AiQmsJpaEntity, UUID> {

    Optional<AiQmsJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<AiQmsJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AiQmsJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, AiQmsStatus status, Pageable pageable);

    List<AiQmsJpaEntity> findByTenantIdAndReferenceOrderByCreatedAtDesc(
            UUID tenantId, String reference);

    boolean existsByTenantIdAndReferenceAndVersion(
            UUID tenantId, String reference, String version);
}
