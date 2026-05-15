package com.openlab.qualitos.quality.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findByTenantIdAndCode(UUID tenantId, String code);

    Page<Supplier> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Supplier> findByTenantIdAndStatus(UUID tenantId, SupplierStatus status, Pageable pageable);

    Page<Supplier> findByTenantIdAndSupplierType(UUID tenantId, SupplierType type, Pageable pageable);
}
