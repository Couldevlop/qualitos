package com.openlab.qualitos.quality.itsm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItsmConnectionRepository extends JpaRepository<ItsmConnection, UUID> {

    Page<ItsmConnection> findByTenantId(UUID tenantId, Pageable pageable);

    List<ItsmConnection> findByTenantIdAndStatus(UUID tenantId, ConnectionStatus status);
}
