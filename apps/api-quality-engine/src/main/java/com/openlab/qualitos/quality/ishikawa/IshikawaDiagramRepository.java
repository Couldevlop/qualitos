package com.openlab.qualitos.quality.ishikawa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IshikawaDiagramRepository extends JpaRepository<IshikawaDiagram, UUID> {

    Page<IshikawaDiagram> findByTenantId(UUID tenantId, Pageable pageable);

    Page<IshikawaDiagram> findByTenantIdAndStatus(UUID tenantId, IshikawaStatus status, Pageable pageable);

    Optional<IshikawaDiagram> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Diagrammes partant d'une non-conformité, du plus récent au plus ancien :
     * la fiche ouvre le dernier, qui est celui qu'on vient de travailler.
     */
    List<IshikawaDiagram> findByTenantIdAndNcIdOrderByCreatedAtDesc(UUID tenantId, UUID ncId);
}
