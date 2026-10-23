package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatrixOperatorJpaRepository extends JpaRepository<MatrixOperatorJpaEntity, UUID> {

    List<MatrixOperatorJpaEntity> findByTenantId(UUID tenantId);

    Optional<MatrixOperatorJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<MatrixOperatorJpaEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);
}
