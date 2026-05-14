package com.openlab.qualitos.quality.ishikawa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IshikawaDiagramRepository extends JpaRepository<IshikawaDiagram, UUID> {

    Page<IshikawaDiagram> findByTenantId(UUID tenantId, Pageable pageable);

    Page<IshikawaDiagram> findByTenantIdAndStatus(UUID tenantId, IshikawaStatus status, Pageable pageable);

    Optional<IshikawaDiagram> findByIdAndTenantId(UUID id, UUID tenantId);
}
