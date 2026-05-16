package com.openlab.qualitos.quality.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface SupplierAuditRecordRepository extends JpaRepository<SupplierAuditRecord, UUID> {

    Page<SupplierAuditRecord> findBySupplierIdOrderByAuditedOnDesc(UUID supplierId, Pageable pageable);

    @Query("select max(a.auditedOn) from SupplierAuditRecord a where a.supplierId = :supplierId")
    Optional<LocalDate> findLatestAuditDate(UUID supplierId);

    void deleteBySupplierId(UUID supplierId);
}
