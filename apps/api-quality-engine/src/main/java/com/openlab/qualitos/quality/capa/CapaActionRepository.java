package com.openlab.qualitos.quality.capa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CapaActionRepository extends JpaRepository<CapaAction, UUID> {

    Optional<CapaAction> findByIdAndCapaId(UUID id, UUID capaId);

    /**
     * Actions en retard : pas terminées, échéance dépassée, dans un dossier
     * encore vivant. Un dossier clos ou rejeté n'a plus d'action « en retard » —
     * ses actions ne se mèneront plus. Compté en base : le tableau de bord SMI
     * n'a pas à charger chaque action pour un nombre.
     */
    @Query("""
            select count(a) from CapaAction a
            where a.capa.tenantId = :tenantId
              and a.status <> :done
              and a.dueDate < :today
              and a.capa.status not in :terminal""")
    long countOverdue(@Param("tenantId") UUID tenantId,
                      @Param("done") CapaActionStatus done,
                      @Param("today") LocalDate today,
                      @Param("terminal") Collection<CapaStatus> terminal);

    /** Même compte, restreint aux dossiers d'une criticité donnée. */
    @Query("""
            select count(a) from CapaAction a
            where a.capa.tenantId = :tenantId
              and a.status <> :done
              and a.dueDate < :today
              and a.capa.status not in :terminal
              and a.capa.criticity = :criticity""")
    long countOverdueWithCriticity(@Param("tenantId") UUID tenantId,
                                   @Param("done") CapaActionStatus done,
                                   @Param("today") LocalDate today,
                                   @Param("terminal") Collection<CapaStatus> terminal,
                                   @Param("criticity") CapaCriticity criticity);

    /**
     * Les actions à mener d'ici {@code until}, retards compris, la plus pressée
     * d'abord — « À traiter cette semaine ». Le dossier vient avec : on affiche
     * sa référence et on y renvoie.
     */
    @Query("""
            select a from CapaAction a join fetch a.capa c
            where c.tenantId = :tenantId
              and a.status <> :done
              and a.dueDate <= :until
              and c.status not in :terminal
            order by a.dueDate asc""")
    List<CapaAction> findDueBy(@Param("tenantId") UUID tenantId,
                               @Param("done") CapaActionStatus done,
                               @Param("until") LocalDate until,
                               @Param("terminal") Collection<CapaStatus> terminal,
                               Pageable page);
}
