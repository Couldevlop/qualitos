package com.openlab.qualitos.quality.aiact.infrastructure;

import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiSystemJpaRepository extends JpaRepository<AiSystemJpaEntity, UUID> {

    Optional<AiSystemJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<AiSystemJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AiSystemJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, AiSystemStatus status, Pageable pageable);

    Page<AiSystemJpaEntity> findByTenantIdAndRiskClassification(
            UUID tenantId, AiRiskClassification riskClassification, Pageable pageable);

    Optional<AiSystemJpaEntity> findByTenantIdAndReference(UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
