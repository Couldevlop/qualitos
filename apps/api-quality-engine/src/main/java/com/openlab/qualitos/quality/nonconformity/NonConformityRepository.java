package com.openlab.qualitos.quality.nonconformity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface NonConformityRepository
        extends JpaRepository<NonConformity, UUID>, JpaSpecificationExecutor<NonConformity> {

    Page<NonConformity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<NonConformity> findByTenantIdAndStatus(UUID tenantId, NcStatus status, Pageable pageable);

    Page<NonConformity> findByTenantIdAndSeverity(UUID tenantId, NcSeverity severity, Pageable pageable);

    Page<NonConformity> findByTenantIdAndCategory(UUID tenantId, NcCategory category, Pageable pageable);

    Page<NonConformity> findByTenantIdAndStatusAndSeverity(
            UUID tenantId, NcStatus status, NcSeverity severity, Pageable pageable);

    Page<NonConformity> findByTenantIdAndStatusAndCategory(
            UUID tenantId, NcStatus status, NcCategory category, Pageable pageable);

    Page<NonConformity> findByTenantIdAndSeverityAndCategory(
            UUID tenantId, NcSeverity severity, NcCategory category, Pageable pageable);

    Page<NonConformity> findByTenantIdAndStatusAndSeverityAndCategory(
            UUID tenantId, NcStatus status, NcSeverity severity, NcCategory category, Pageable pageable);

    Optional<NonConformity> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Vrai si la référence générée est déjà prise pour ce tenant (collision improbable). */
    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    /** Numérotation séquentielle annuelle, par tenant. */
    long countByTenantIdAndReferenceStartingWith(UUID tenantId, String prefix);

    /**
     * Nombre de NC d'une catégorie qui ne sont PAS dans un statut terminal — alimente la
     * répartition des défauts du dashboard exécutif (§7.1). Un compte en base évite de
     * charger les entités juste pour les dénombrer.
     */
    long countByTenantIdAndCategoryAndStatusNotIn(
            UUID tenantId, NcCategory category, Collection<NcStatus> excludedStatuses);

    /**
     * Non-conformités rattachées à une CAPA et pas encore refermées.
     *
     * <p>Sert au verrou de clôture : un dossier d'action corrective ne se clôt
     * pas tant que l'écart qui l'a motivé reste ouvert. Compter suffit — l'écran
     * a besoin du nombre, pas des lignes.
     */
    long countByTenantIdAndCapaCaseIdAndStatusNotIn(UUID tenantId, UUID capaCaseId, java.util.Collection<NcStatus> statuses);

    /**
     * Écart d'origine d'un dossier CAPA, pour l'afficher par son nom plutôt que
     * par un identifiant.
     *
     * <p>Le lien est porté par la NC (colonne {@code capa_case_id}), pas par le
     * dossier : c'est l'écart qui s'escalade en CAPA, pas l'inverse. Plusieurs
     * écarts peuvent pointer vers le même dossier ; on retient le plus ancien,
     * celui qui l'a motivé — {@code findFirst} plutôt qu'une liste, parce
     * qu'un tableau montre une origine, pas un inventaire.
     */
    java.util.Optional<NonConformity> findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(
            UUID tenantId, UUID capaCaseId);

    /** Repli quand aucune NC ne pointe vers le dossier : la référence saisie à la main. */
    java.util.Optional<NonConformity> findByTenantIdAndReference(UUID tenantId, String reference);
}
