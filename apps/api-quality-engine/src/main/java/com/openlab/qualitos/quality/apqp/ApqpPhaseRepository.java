package com.openlab.qualitos.quality.apqp;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApqpPhaseRepository extends JpaRepository<ApqpPhase, UUID> {

    /**
     * Le cycle d'un client, dans l'ordre du V.
     *
     * <p>{@code EntityGraph} sur les livrables : l'écran les affiche tous, et
     * sans cela chaque phase déclencherait sa propre requête — cinq phases, six
     * allers-retours pour un seul affichage.
     */
    @EntityGraph(attributePaths = "deliverables")
    List<ApqpPhase> findByTenantIdOrderByPositionAsc(UUID tenantId);

    /** Une phase, à condition qu'elle appartienne au client du jeton. */
    Optional<ApqpPhase> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Sert à savoir s'il faut amorcer le cycle depuis le référentiel AIAG. */
    boolean existsByTenantId(UUID tenantId);

    /** Rang le plus élevé : une phase ajoutée se place à la suite. */
    Optional<ApqpPhase> findFirstByTenantIdOrderByPositionDesc(UUID tenantId);
}
