package com.openlab.qualitos.quality.itsm;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItsmIncidentMappingRepository extends JpaRepository<ItsmIncidentMapping, UUID> {

    Optional<ItsmIncidentMapping> findByConnectionIdAndExternalId(UUID connectionId, String externalId);

    Page<ItsmIncidentMapping> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ItsmIncidentMapping> findByTenantIdAndConnectionId(UUID tenantId, UUID connectionId, Pageable pageable);
}
