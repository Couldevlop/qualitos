package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DossierJpaRepository extends JpaRepository<DossierJpaEntity, UUID> {

    Optional<DossierJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<DossierJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);
}
