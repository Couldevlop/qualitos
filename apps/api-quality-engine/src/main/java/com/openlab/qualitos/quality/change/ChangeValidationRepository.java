package com.openlab.qualitos.quality.change;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChangeValidationRepository extends JpaRepository<ChangeValidation, UUID> {

    List<ChangeValidation> findByTenantIdAndChangeIdOrderByOrdinalAsc(UUID tenantId, UUID changeId);

    List<ChangeValidation> findByTenantIdAndChangeIdIn(UUID tenantId, Collection<UUID> changeIds);

    /** Les cartes encore en attente d'un rôle : celles qu'un changement de paramétrage concerne. */
    List<ChangeValidation> findByTenantIdAndRoleIdInAndDecision(UUID tenantId, Collection<UUID> roleIds,
                                                                ApprovalDecision decision);

    void deleteByTenantIdAndChangeId(UUID tenantId, UUID changeId);

    /**
     * Les cartes en attente des demandes en validation, TOUS clients confondus :
     * c'est la tâche de rappel qui les lit, hors requête, puis les traite client
     * par client avec le client porté par chaque ligne.
     */
    @Query("select v from ChangeValidation v, ChangeRequest c where c.id = v.changeId "
            + "and c.tenantId = v.tenantId and c.status = :status and v.decision = :decision "
            + "order by v.tenantId, v.changeId, v.ordinal")
    List<ChangeValidation> findPendingOfStatus(@Param("status") ChangeRequestStatus status,
                                               @Param("decision") ApprovalDecision decision);

    /**
     * Réserve le PREMIER rappel d'une carte : 1 si l'appelant l'obtient, 0 si une
     * autre réplique l'a pris ou si la carte a été décidée entre-temps. Même
     * dispositif que le rappel d'audit : réserver avant d'envoyer, jamais l'inverse.
     */
    @Modifying
    @Transactional
    @Query("update ChangeValidation v set v.lastRemindedAt = :now where v.id = :id "
            + "and v.decision = com.openlab.qualitos.quality.change.ApprovalDecision.PENDING "
            + "and v.lastRemindedAt is null")
    int claimFirstReminder(@Param("id") UUID id, @Param("now") Instant now);

    /** Réserve un rappel SUIVANT : la carte doit porter encore le rappel lu au départ. */
    @Modifying
    @Transactional
    @Query("update ChangeValidation v set v.lastRemindedAt = :now where v.id = :id "
            + "and v.decision = com.openlab.qualitos.quality.change.ApprovalDecision.PENDING "
            + "and v.lastRemindedAt = :previous")
    int claimNextReminder(@Param("id") UUID id, @Param("previous") Instant previous, @Param("now") Instant now);
}
