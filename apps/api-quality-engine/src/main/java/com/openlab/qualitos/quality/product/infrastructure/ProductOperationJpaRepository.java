package com.openlab.qualitos.quality.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductOperationJpaRepository extends JpaRepository<ProductOperationJpaEntity, UUID> {

    List<ProductOperationJpaEntity> findByProductIdAndTenantIdOrderBySequenceNoAsc(
            UUID productId, UUID tenantId);

    Optional<ProductOperationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
