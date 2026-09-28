package com.openlab.qualitos.quality.costofquality.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoqEntryJpaRepository extends JpaRepository<CoqEntryJpaEntity, UUID> {

    Optional<CoqEntryJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CoqEntryJpaEntity> findByTenantIdAndImputationDateBetween(
            UUID tenantId, LocalDate from, LocalDate to);
}
