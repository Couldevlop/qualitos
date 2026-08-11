package com.openlab.qualitos.quality.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditPlanRepository extends JpaRepository<AuditPlan, UUID> {

    Page<AuditPlan> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AuditPlan> findByTenantIdAndStatus(UUID tenantId, AuditStatus status, Pageable pageable);

    Page<AuditPlan> findByTenantIdAndType(UUID tenantId, AuditType type, Pageable pageable);

    Optional<AuditPlan> findByIdAndTenantId(UUID id, UUID tenantId);

    // --- référence lisible (§4.4) ---

    /** Garde anti-collision de la référence, adossée à l'unicité (tenant, reference). */
    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    /** Rang de départ de la séquence annuelle du tenant — cf. AuditService#generateReference. */
    long countByTenantIdAndReferenceStartingWith(UUID tenantId, String prefix);

    // --- planning (§4.4) : les audits à venir, du plus proche au plus lointain ---

    /**
     * Audits encore à réaliser dont l'échéance ne dépasse pas {@code horizon}.
     *
     * <p>Les échéances déjà passées sont volontairement INCLUSES (aucune borne
     * basse) : un audit planifié le mois dernier et jamais lancé est justement ce
     * qu'un planning doit faire remonter. Les taire produirait un écran rassurant
     * et faux. L'écran les distingue par un décompte négatif.
     *
     * <p>Les plans sans date sont exclus : ils ne sont pas planifiés, ils sont
     * seulement créés.
     */
    @Query("""
            select p from AuditPlan p
            where p.tenantId = :tenantId
              and p.status = :status
              and p.scheduledDate is not null
              and p.scheduledDate <= :horizon
            order by p.scheduledDate asc
            """)
    List<AuditPlan> findUpcoming(@Param("tenantId") UUID tenantId,
                                 @Param("status") AuditStatus status,
                                 @Param("horizon") LocalDate horizon,
                                 Pageable pageable);

    /** Même fenêtre, restreinte à un type d'audit (interne / externe / …). */
    @Query("""
            select p from AuditPlan p
            where p.tenantId = :tenantId
              and p.status = :status
              and p.type = :type
              and p.scheduledDate is not null
              and p.scheduledDate <= :horizon
            order by p.scheduledDate asc
            """)
    List<AuditPlan> findUpcomingByType(@Param("tenantId") UUID tenantId,
                                       @Param("status") AuditStatus status,
                                       @Param("type") AuditType type,
                                       @Param("horizon") LocalDate horizon,
                                       Pageable pageable);

    // --- rappel d'échéance (§4.4) ---

    /**
     * Audits à rappeler : planifiés, non encore rappelés, échéance dans la fenêtre
     * {@code [from, to]}.
     *
     * <p>PAS DE FILTRE TENANT, et c'est délibéré : cette requête sert
     * l'ordonnanceur, qui s'exécute hors requête HTTP, donc sans {@code TenantContext}.
     * Chaque ligne rapportée porte son propre {@code tenantId}, que l'appelant
     * utilise explicitement pour adresser la notification. Se reposer ici sur un
     * contexte ambiant absent aurait produit soit une exception, soit — pire — un
     * balayage limité au dernier tenant vu par le thread.
     *
     * <p>Borne basse {@code from} = aujourd'hui : on ne rappelle pas une échéance
     * déjà passée. Un courriel annonçant « votre audit approche » pour une date
     * dépassée décrédibilise le dispositif ; le retard, lui, se voit sur l'écran
     * de planning, qui est fait pour ça.
     */
    @Query("""
            select p from AuditPlan p
            where p.status = :status
              and p.reminderSentAt is null
              and p.scheduledDate is not null
              and p.scheduledDate >= :from
              and p.scheduledDate <= :to
            order by p.scheduledDate asc
            """)
    List<AuditPlan> findDueForReminder(@Param("status") AuditStatus status,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to,
                                       Pageable pageable);

    /**
     * Réserve le rappel d'un audit : renvoie 1 si l'appelant vient de gagner le
     * droit d'envoyer, 0 si un autre l'a déjà pris.
     *
     * <p>La clause {@code reminder_sent_at is null} est le cœur du dispositif.
     * L'engine tourne en plusieurs répliques, chacune avec son ordonnanceur ; deux
     * répliques lisent la même liste à la même minute. C'est la base qui tranche,
     * en sérialisant les deux UPDATE sur la même ligne : le second n'affecte aucune
     * ligne. Un « lire puis écrire » côté application, lui, laisserait les deux
     * répliques croire qu'elles sont seules.
     *
     * <p>{@code @Transactional} sur la méthode : l'ordonnanceur n'ouvre pas de
     * transaction, et la réservation doit être validée AVANT l'envoi — pas à la fin
     * d'une transaction englobante qu'une panne d'envoi ferait annuler.
     */
    @Modifying
    @Transactional
    @Query("update AuditPlan p set p.reminderSentAt = :now where p.id = :id and p.reminderSentAt is null")
    int claimReminder(@Param("id") UUID id, @Param("now") Instant now);
}
