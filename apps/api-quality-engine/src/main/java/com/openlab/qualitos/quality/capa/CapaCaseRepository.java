package com.openlab.qualitos.quality.capa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapaCaseRepository extends JpaRepository<CapaCase, UUID> {

    Page<CapaCase> findByTenantId(UUID tenantId, Pageable pageable);

    Page<CapaCase> findByTenantIdAndStatus(UUID tenantId, CapaStatus status, Pageable pageable);

    Optional<CapaCase> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Les dossiers clos d'un tenant, du plus récent au plus ancien.
     *
     * <p>Non paginée à dessein : la mesure d'efficacité les parcourt tous pour
     * rendre une moyenne, et une moyenne calculée sur une page n'aurait aucun
     * sens. Le volume est celui des CAPA CLOSES d'un tenant, pas de son
     * historique complet de non-conformités.
     */
    List<CapaCase> findByTenantIdAndStatusOrderByClosedAtDesc(UUID tenantId, CapaStatus status);

    /**
     * Idempotence des CAPA auto-générées (ex. dérive IoT) : vrai si une CAPA
     * non terminale existe déjà pour la même origine. Évite le spam d'une CAPA
     * par mesure tant que la précédente n'est pas clôturée.
     */
    boolean existsByTenantIdAndSourceTypeAndSourceRefAndStatusIn(
            UUID tenantId, CapaSourceType sourceType, String sourceRef, Collection<CapaStatus> statuses);

    /**
     * CAPA encore ouvertes dont l'échéance est dépassée — alimente les « risques majeurs »
     * du dashboard exécutif (§7.1). Triées par échéance croissante : le plus en retard
     * d'abord.
     */
    List<CapaCase> findTop10ByTenantIdAndStatusNotInAndDueDateBeforeOrderByDueDateAsc(
            UUID tenantId, Collection<CapaStatus> excludedStatuses, LocalDate before);
}
