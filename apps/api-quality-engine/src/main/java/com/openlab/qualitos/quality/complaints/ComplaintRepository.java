package com.openlab.qualitos.quality.complaints;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    Optional<Complaint> findByTenantIdAndCode(UUID tenantId, String code);

    Page<Complaint> findByTenantId(UUID tenantId, Pageable pageable);

    Page<Complaint> findByTenantIdAndStatus(UUID tenantId, ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByTenantIdAndCategory(UUID tenantId, ComplaintCategory category, Pageable pageable);

    Page<Complaint> findByTenantIdAndSupplierId(UUID tenantId, UUID supplierId, Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, ComplaintStatus status);

    long countByTenantIdAndCategory(UUID tenantId, ComplaintCategory category);
}
