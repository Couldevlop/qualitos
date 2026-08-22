package com.openlab.qualitos.quality.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductComponentJpaRepository extends JpaRepository<ProductComponentJpaEntity, UUID> {

    List<ProductComponentJpaEntity> findByProductIdAndTenantIdOrderBySequenceNoAsc(
            UUID productId, UUID tenantId);

    Optional<ProductComponentJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
