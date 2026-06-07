package com.openlab.qualitos.quality.ehrconnector;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EhrConnectionRepository extends JpaRepository<EhrConnection, UUID> {

    Page<EhrConnection> findByTenantId(UUID tenantId, Pageable pageable);
}
