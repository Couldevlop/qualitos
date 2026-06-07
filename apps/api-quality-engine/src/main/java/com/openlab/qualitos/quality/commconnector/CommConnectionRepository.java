package com.openlab.qualitos.quality.commconnector;

import com.openlab.qualitos.quality.itsm.ConnectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommConnectionRepository extends JpaRepository<CommConnection, UUID> {

    Page<CommConnection> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<CommConnection> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CommConnection> findByTenantIdAndStatus(UUID tenantId, ConnectionStatus status);
}
