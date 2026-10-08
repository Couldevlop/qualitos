package com.openlab.qualitos.quality.capa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * Les dossiers qui concernent un utilisateur (ADR 0081) : il les pilote, il en
     * vérifie l'efficacité, ou une de leurs actions lui est confiée. Filtré en
     * base, pour que la pagination reste juste. Un statut nul ne filtre pas.
     */
    @Query("""
            select c from CapaCase c
            where c.tenantId = :tenantId
              and (:status is null or c.status = :status)
              and (c.ownerId = :user
                   or c.verificationAssigneeId = :user
                   or exists (select a.id from CapaAction a where a.capa = c and a.assigneeId = :user))
            """)
    Page<CapaCase> findConcerning(@Param("tenantId") UUID tenantId, @Param("status") CapaStatus status,
                                  @Param("user") UUID user, Pageable pageable);

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
    /**
     * Les dossiers ouverts depuis une même origine, du plus ancien au plus
     * récent : le tableau « Traitement » d'une fiche de risque.
     */
    List<CapaCase> findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
            UUID tenantId, CapaSourceType sourceType, String sourceRef);

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
