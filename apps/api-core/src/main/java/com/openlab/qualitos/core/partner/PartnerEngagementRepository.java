package com.openlab.qualitos.core.partner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les accords.
 *
 * <p>La table porte {@code tenant_id} : elle est sous la politique de
 * cloisonnement (ADR 0086), et un administrateur de client ne lit donc jamais
 * les accords d'un autre, meme en forgeant sa requete. Les lectures faites pour
 * l'editeur ou pour un consultant traversent plusieurs clients : elles passent
 * par le drapeau de contournement, et sont journalisees comme acces privilegie.
 */
public interface PartnerEngagementRepository extends JpaRepository<PartnerEngagement, UUID> {

    List<PartnerEngagement> findByTenantIdOrderByProposedAtDesc(UUID tenantId);

    List<PartnerEngagement> findByFirmIdOrderByProposedAtDesc(UUID firmId);

    /**
     * L'accord vivant d'un couple client/cabinet, s'il y en a un. L'index unique
     * partiel de la migration garantit qu'il n'y en a jamais deux.
     */
    @Query("""
            select e from PartnerEngagement e
            where e.tenantId = :tenantId and e.firmId = :firmId
              and e.status in (com.openlab.qualitos.core.partner.EngagementStatus.PROPOSED,
                               com.openlab.qualitos.core.partner.EngagementStatus.GRANTED)
            """)
    Optional<PartnerEngagement> findLive(@Param("tenantId") UUID tenantId,
                                         @Param("firmId") UUID firmId);

    @Query("""
            select e from PartnerEngagement e
            where e.firmId = :firmId
              and e.status = com.openlab.qualitos.core.partner.EngagementStatus.GRANTED
            order by e.endsOn asc
            """)
    List<PartnerEngagement> findGrantedForFirm(@Param("firmId") UUID firmId);

    long countByFirmIdAndStatus(UUID firmId, EngagementStatus status);
}
