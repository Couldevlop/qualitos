package com.openlab.qualitos.quality.capa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapaEvidenceRepository extends JpaRepository<CapaEvidence, UUID> {

    Optional<CapaEvidence> findByIdAndTenantIdAndCapaId(UUID id, UUID tenantId, UUID capaId);

    /**
     * Poids déjà versé au dossier. La somme est calculée en base plutôt que sur
     * une liste chargée : le plafond doit être vérifié même si le dossier porte
     * ses dix pièces, et charger les métadonnées pour additionner un entier
     * serait payer un aller-retour pour rien.
     *
     * <p>Elle compte les DEUX niveaux (dossier et actions) : ce plafond protège
     * le disque, pas la lisibilité d'une liste — et le disque ne fait pas la
     * différence.
     */
    @Query("select coalesce(sum(e.sizeBytes), 0) from CapaEvidence e "
            + "where e.tenantId = :tenantId and e.capaId = :capaId")
    long sumSizeBytes(UUID tenantId, UUID capaId);

    // --- pièces du DOSSIER (action_id NULL, ADR 0050) --------------------------
    // Les dérivations Spring Data ne savent pas exprimer « IS NULL » sur un
    // paramètre : d'où les requêtes explicites. Sans le filtre, les pièces
    // d'actions remonteraient aussi dans la liste du dossier et l'écran les
    // afficherait deux fois.

    @Query("select e from CapaEvidence e where e.tenantId = :tenantId and e.capaId = :capaId "
            + "and e.actionId is null order by e.createdAt asc")
    List<CapaEvidence> findCaseLevel(UUID tenantId, UUID capaId);

    @Query("select count(e) from CapaEvidence e where e.tenantId = :tenantId "
            + "and e.capaId = :capaId and e.actionId is null")
    long countCaseLevel(UUID tenantId, UUID capaId);

    // --- pièces des ACTIONS (ADR 0052) ------------------------------------------

    /** Toutes les pièces d'actions d'un dossier, en une passe : le tableau les range par action. */
    @Query("select e from CapaEvidence e where e.tenantId = :tenantId and e.capaId = :capaId "
            + "and e.actionId is not null order by e.createdAt asc")
    List<CapaEvidence> findActionLevel(UUID tenantId, UUID capaId);

    Optional<CapaEvidence> findByTenantIdAndActionId(UUID tenantId, UUID actionId);

    long countByTenantIdAndActionId(UUID tenantId, UUID actionId);
}
