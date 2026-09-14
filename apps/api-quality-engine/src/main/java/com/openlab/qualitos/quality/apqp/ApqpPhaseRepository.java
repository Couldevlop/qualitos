package com.openlab.qualitos.quality.apqp;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApqpPhaseRepository extends JpaRepository<ApqpPhase, UUID> {

    /**
     * Le cycle d'un projet, dans l'ordre du V.
     *
     * <p>{@code EntityGraph} sur les livrables : l'écran les affiche tous, et
     * sans cela chaque phase déclencherait sa propre requête — cinq phases, six
     * allers-retours pour un seul affichage.
     *
     * <p>Le filtre de client accompagne celui de projet, et ne fait pas double
     * emploi avec lui : c'est la seule barrière qui tienne si un identifiant de
     * projet est emprunté ailleurs.
     */
    @EntityGraph(attributePaths = "deliverables")
    List<ApqpPhase> findByProjectIdAndTenantIdOrderByPositionAsc(UUID projectId, UUID tenantId);

    /** Une phase, à condition qu'elle appartienne à ce projet et à ce client. */
    Optional<ApqpPhase> findByIdAndProjectIdAndTenantId(UUID id, UUID projectId, UUID tenantId);

    /** Sert à savoir s'il faut amorcer le cycle depuis le référentiel AIAG. */
    boolean existsByProjectIdAndTenantId(UUID projectId, UUID tenantId);

    /**
     * Efface le cycle d'un projet, pour le réamorcer depuis le référentiel.
     *
     * <p>Les livrables et leurs pièces suivent en cascade (contraintes des V124 et
     * V126) : un livrable sans phase n'a pas d'existence propre, et une preuve sans
     * livrable ne prouve plus rien.
     */
    void deleteByProjectIdAndTenantId(UUID projectId, UUID tenantId);

    /** Rang le plus élevé : une phase ajoutée se place à la suite. */
    Optional<ApqpPhase> findFirstByProjectIdAndTenantIdOrderByPositionDesc(
            UUID projectId, UUID tenantId);

    /**
     * L'avancement de chaque projet d'un client, en UNE requête groupée.
     *
     * <p>La liste des projets affiche quatre compteurs par ligne. Les demander
     * projet par projet ferait autant d'allers-retours que de programmes ouverts,
     * pour une colonne de chiffres.
     *
     * <p>Rendu : identifiant du projet, livrables, livrables acquis, livrables du
     * dossier PPAP, livrables du dossier acquis.
     */
    @Query("select p.project.id,"
            + " count(d),"
            + " sum(case when d.done then 1 else 0 end),"
            + " sum(case when d.ppap then 1 else 0 end),"
            + " sum(case when d.ppap and d.done then 1 else 0 end)"
            + " from ApqpDeliverable d join d.phase p"
            + " where p.tenantId = :tenantId group by p.project.id")
    List<Object[]> avancementParProjet(UUID tenantId);
}
