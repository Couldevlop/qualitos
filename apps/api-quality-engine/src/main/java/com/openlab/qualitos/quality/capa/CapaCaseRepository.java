package com.openlab.qualitos.quality.capa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CapaCaseRepository extends JpaRepository<CapaCase, UUID> {

    Page<CapaCase> findByTenantId(UUID tenantId, Pageable pageable);

    Page<CapaCase> findByTenantIdAndStatus(UUID tenantId, CapaStatus status, Pageable pageable);

    Optional<CapaCase> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Idempotence des CAPA auto-générées (ex. dérive IoT) : vrai si une CAPA
     * non terminale existe déjà pour la même origine. Évite le spam d'une CAPA
     * par mesure tant que la précédente n'est pas clôturée.
     */
    boolean existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
            UUID tenantId, CapaSourceType sourceType, String sourceRef, Collection<CapaStatus> statuses);
}
