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
              and (:criticity is null or c.criticity = :criticity)
              and (c.ownerId = :user
                   or c.verificationAssigneeId = :user
                   or exists (select a.id from CapaAction a where a.capa = c and a.assigneeId = :user))
            """)
    Page<CapaCase> findConcerning(@Param("tenantId") UUID tenantId, @Param("status") CapaStatus status,
                                  @Param("criticity") CapaCriticity criticity,
                                  @Param("user") UUID user, Pageable pageable);

    /** La liste du registre, filtrée en base par statut et criticité ; un filtre nul ne filtre pas. */
    @Query("""
            select c from CapaCase c
            where c.tenantId = :tenantId
              and (:status is null or c.status = :status)
              and (:criticity is null or c.criticity = :criticity)
            """)
    Page<CapaCase> search(@Param("tenantId") UUID tenantId, @Param("status") CapaStatus status,
                          @Param("criticity") CapaCriticity criticity, Pageable pageable);

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
     * Les dossiers qui entrent dans la boucle d'efficacité (ADR 0108) : tous
     * ceux dont le statut n'est pas exclu — en pratique, tous sauf les dossiers
     * rejetés, qui n'ont jamais été instruits et dont il n'y a rien à juger.
     *
     * <p><b>Les actions sont chargées AVEC le dossier</b> ({@code left join
     * fetch}). Sans cela, le plan de travail déclenche une requête par ligne
     * pour compter les actions réalisées — exactement le défaut que le port
     * d'évaluation interdit dans son contrat. {@code distinct} est nécessaire
     * parce que la jointure duplique le dossier autant de fois qu'il a
     * d'actions.
     *
     * <p>Non paginée à dessein, comme la mesure par récidive : les indicateurs
     * portent sur l'ensemble, et un taux calculé sur une page n'aurait aucun
     * sens. C'est le service qui borne ce qu'il AFFICHE, et qui le dit.
     */
    @Query("""
            select distinct c from CapaCase c
            left join fetch c.actions
            where c.tenantId = :tenantId
              and c.status not in :excludedStatuses
            """)
    List<CapaCase> findForEffectivenessBoard(
            @Param("tenantId") UUID tenantId,
            @Param("excludedStatuses") Collection<CapaStatus> excludedStatuses);

    /**
     * Le même périmètre, restreint aux dossiers qui CONCERNENT un utilisateur
     * (ADR 0081) : il les pilote, il en vérifie l'efficacité, ou une de leurs
     * actions lui est confiée.
     *
     * <p>Une seconde requête plutôt qu'un {@code :user is null} dans la
     * précédente : c'est le choix déjà fait pour le registre paginé, et il évite
     * de confier à chaque base le typage d'un paramètre nul.
     *
     * <p>Le {@code exists} porte sur les actions et non sur la jointure
     * ramenée : filtrer sur {@code join fetch} restreindrait la COLLECTION
     * chargée — le dossier arriverait avec les seules actions de cet
     * utilisateur, et son décompte « 1/1 faite » serait faux.
     */
    @Query("""
            select distinct c from CapaCase c
            left join fetch c.actions
            where c.tenantId = :tenantId
              and c.status not in :excludedStatuses
              and (c.ownerId = :user
                   or c.verificationAssigneeId = :user
                   or exists (select a.id from CapaAction a where a.capa = c and a.assigneeId = :user))
            """)
    List<CapaCase> findForEffectivenessBoardConcerning(
            @Param("tenantId") UUID tenantId,
            @Param("excludedStatuses") Collection<CapaStatus> excludedStatuses,
            @Param("user") UUID user);

    /** Le dossier existe-t-il dans ce client ? Sans le charger. */
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Ce dossier concerne-t-il cet utilisateur (ADR 0081) ?
     *
     * <p>Répondu en base, et non en chargeant le dossier pour interroger
     * {@code CapaScope} : la question porte sur les actions, dont la collection
     * est paresseuse — y répondre en mémoire demanderait une session ouverte et
     * ramènerait tout le dossier pour ne rendre qu'un oui ou un non. Le
     * prédicat est donc écrit ici, comme il l'est déjà pour le registre
     * paginé ; {@code CapaScope.concerns} en reste la définition de référence,
     * et les deux doivent changer ensemble.
     */
    @Query("""
            select count(c.id) > 0 from CapaCase c
            where c.id = :id
              and c.tenantId = :tenantId
              and (c.ownerId = :user
                   or c.verificationAssigneeId = :user
                   or exists (select a.id from CapaAction a where a.capa = c and a.assigneeId = :user))
            """)
    boolean existsConcerning(@Param("id") UUID id, @Param("tenantId") UUID tenantId,
                             @Param("user") UUID user);

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
