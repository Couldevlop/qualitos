package com.openlab.qualitos.quality.derogation.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Depot Spring Data des lignes d'ecart. */
public interface DerogationLineJpaRepository
        extends JpaRepository<DerogationLineJpaEntity, UUID> {

    List<DerogationLineJpaEntity> findByTenantIdAndDerogationIdOrderByPositionAsc(
            UUID tenantId, UUID derogationId);

    /**
     * Les lignes d'un lot de dossiers, pour la colonne « defaut » du registre.
     *
     * <p>En UNE requete : une par ligne de tableau ferait cinquante appels sur un
     * ecran qui en affiche cinquante.
     */
    List<DerogationLineJpaEntity> findByTenantIdAndDerogationIdInOrderByPositionAsc(
            UUID tenantId, List<UUID> derogationIds);

    /**
     * Suppression en MASSE, et non par entite.
     *
     * <p>C'est volontaire et c'est le point delicat de ce depot. Les lignes se
     * reecrivent en bloc : si la suppression passait par le contexte de
     * persistance, Hibernate ordonnerait les INSERT avant les DELETE, et le
     * nouveau rang 1 heurterait l'ancien sur la contrainte d'unicite. Une requete
     * de suppression, suivie d'un `flush`, impose l'ordre.
     */
    @Modifying
    @Query("delete from DerogationLineJpaEntity l "
            + "where l.tenantId = :tenantId and l.derogationId = :derogationId")
    int deleteLinesOf(@Param("tenantId") UUID tenantId,
                      @Param("derogationId") UUID derogationId);
}
