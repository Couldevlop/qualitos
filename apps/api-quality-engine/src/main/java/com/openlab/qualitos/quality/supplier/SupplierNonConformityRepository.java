package com.openlab.qualitos.quality.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.UUID;

public interface SupplierNonConformityRepository extends JpaRepository<SupplierNonConformity, UUID>,
        JpaSpecificationExecutor<SupplierNonConformity> {

    Page<SupplierNonConformity> findBySupplierIdOrderByDetectedOnDesc(UUID supplierId, Pageable pageable);

    long countBySupplierIdAndStatus(UUID supplierId, NonConformityStatus status);

    long countBySupplierIdAndStatusAndDetectedOnAfter(
            UUID supplierId, NonConformityStatus status, LocalDate cutoff);

    long countBySupplierIdAndSeverityAndStatus(
            UUID supplierId, NonConformitySeverity severity, NonConformityStatus status);

    void deleteBySupplierId(UUID supplierId);
}
