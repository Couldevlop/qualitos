package com.openlab.qualitos.quality.standards.normdoc.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NormDocJpaRepository extends JpaRepository<NormDocJpaEntity, UUID> {

    Optional<NormDocJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<NormDocJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    List<NormDocJpaEntity> findByTenantIdAndStatus(UUID tenantId, NormDocStatus status, Pageable pageable);

    /** Pièces approuvées d'un type donné, hors d'une norme — réutilisation transversale (§8.9). */
    List<NormDocJpaEntity> findByTenantIdAndKindAndStatusAndStandardIdNot(
            UUID tenantId, NormDocKind kind, NormDocStatus status, UUID excludeStandardId,
            Pageable pageable);
}
