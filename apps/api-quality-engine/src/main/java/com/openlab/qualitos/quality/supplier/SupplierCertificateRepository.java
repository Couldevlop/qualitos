package com.openlab.qualitos.quality.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SupplierCertificateRepository extends JpaRepository<SupplierCertificate, UUID> {

    Page<SupplierCertificate> findBySupplierIdOrderByExpiresOnAsc(UUID supplierId, Pageable pageable);

    List<SupplierCertificate> findBySupplierIdAndExpiresOnBefore(UUID supplierId, LocalDate cutoff);

    long countBySupplierIdAndExpiresOnBefore(UUID supplierId, LocalDate cutoff);

    void deleteBySupplierId(UUID supplierId);
}
