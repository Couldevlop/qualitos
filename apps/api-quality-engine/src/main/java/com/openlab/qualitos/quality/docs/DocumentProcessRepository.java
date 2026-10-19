package com.openlab.qualitos.quality.docs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentProcessRepository extends JpaRepository<DocumentProcess, UUID> {

    List<DocumentProcess> findByTenantIdOrderByPositionAscNameAsc(UUID tenantId);

    Optional<DocumentProcess> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
