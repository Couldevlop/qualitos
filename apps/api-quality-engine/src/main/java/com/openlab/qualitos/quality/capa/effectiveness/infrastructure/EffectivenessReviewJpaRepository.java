package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface EffectivenessReviewJpaRepository extends JpaRepository<EffectivenessReviewJpaEntity, UUID> {

    /** L'historique d'un dossier, du plus récent au plus ancien. */
    List<EffectivenessReviewJpaEntity> findByTenantIdAndCapaCaseIdOrderByReviewedAtDesc(
            UUID tenantId, UUID capaCaseId);

    /**
     * La DERNIÈRE évaluation de chaque dossier du tenant, en une requête.
     *
     * <p>« La plus récente par dossier » s'écrit ici comme « celle dont aucune
     * autre du même dossier n'est postérieure ». C'est délibérément formulé en
     * JPQL portable plutôt qu'avec une fonction de fenêtrage : la requête doit
     * s'exécuter telle quelle sur la base H2 des tests comme sur PostgreSQL, et
     * une divergence de dialecte entre les deux ferait passer les tests sur du
     * SQL qui n'est pas celui de la production.
     *
     * <p>Départage par l'identifiant en cas d'horodatage identique — deux
     * évaluations à la même microseconde sont improbables, mais une requête qui
     * rendrait deux lignes pour un dossier casserait la carte construite par
     * l'appelant, et ce genre de panne ne se reproduit jamais à la demande.
     */
    @Query("""
            select r from EffectivenessReviewJpaEntity r
            where r.tenantId = :tenantId
              and not exists (
                select o.id from EffectivenessReviewJpaEntity o
                where o.tenantId = r.tenantId
                  and o.capaCaseId = r.capaCaseId
                  and (o.reviewedAt > r.reviewedAt
                       or (o.reviewedAt = r.reviewedAt and o.id > r.id))
              )
            """)
    List<EffectivenessReviewJpaEntity> findLastPerCase(@Param("tenantId") UUID tenantId);

    /**
     * Le nombre d'évaluations par dossier.
     *
     * <p>Rendu en {@code Object[]} — {@code [capaCaseId, count]} — faute de
     * projection plus parlante sans créer un type dont la seule raison d'être
     * serait de porter deux colonnes. L'adaptateur le transforme aussitôt en
     * carte, et rien d'autre ne voit cette forme.
     */
    @Query("""
            select r.capaCaseId, count(r.id) from EffectivenessReviewJpaEntity r
            where r.tenantId = :tenantId
            group by r.capaCaseId
            """)
    List<Object[]> countPerCase(@Param("tenantId") UUID tenantId);
}
