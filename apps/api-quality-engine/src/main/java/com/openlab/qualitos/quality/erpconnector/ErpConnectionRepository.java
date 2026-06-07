package com.openlab.qualitos.quality.erpconnector;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ErpConnectionRepository extends JpaRepository<ErpConnection, UUID> {

    Page<ErpConnection> findByTenantId(UUID tenantId, Pageable pageable);
}
