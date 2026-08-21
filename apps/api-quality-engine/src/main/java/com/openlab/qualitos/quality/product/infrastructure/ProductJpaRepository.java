package com.openlab.qualitos.quality.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<ProductJpaEntity> findByTenantIdOrderByCodeAsc(UUID tenantId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
