package com.openlab.qualitos.quality.standards;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès au catalogue normatif.
 *
 * <p>N'étend PAS {@code JpaRepository} ni {@code CrudRepository} : ces interfaces
 * apportent {@code findById}/{@code existsById}/{@code findAll}/{@code deleteById}
 * SANS tenant, et le catalogue mêle désormais des normes de plateforme et des
 * référentiels appartenant à des tenants — une seule lecture non filtrée
 * exposerait les procédures internes d'une organisation à toutes les autres.
 * L'invariant est donc structurel, pas déclaratif : en se limitant au marqueur
 * minimal {@code Repository<Standard, UUID>} et en ne déclarant explicitement que
 * ce dont l'application a besoin, aucune méthode non filtrée n'existe sur ce
 * type — il n'y a donc rien à interdire par une liste noire de noms, qu'un
 * héritage aurait de toute façon pu contourner (voir
 * {@code StandardTenantIsolationTest}). {@code save}/{@code delete} restent
 * disponibles (signatures identiques à {@code CrudRepository}, implémentées par
 * Spring Data JPA sans avoir besoin d'étendre cette interface) : une écriture
 * porte déjà son tenant via {@code Standard.ownerTenantId} sur l'entité.
 */
public interface StandardRepository extends Repository<Standard, UUID> {

    String VISIBLE = "(s.ownerTenantId is null or s.ownerTenantId = :tenantId)";

    @Query("select s from Standard s where " + VISIBLE)
    Page<Standard> findVisible(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("select s from Standard s where s.status = :status and " + VISIBLE)
    Page<Standard> findVisibleByStatus(@Param("status") StandardStatus status,
                                       @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("select s from Standard s where s.family = :family and " + VISIBLE)
    Page<Standard> findVisibleByFamily(@Param("family") String family,
                                       @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("select s from Standard s where s.id = :id and " + VISIBLE)
    Optional<Standard> findVisibleById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("select s from Standard s where s.code = :code and " + VISIBLE)
    Optional<Standard> findVisibleByCode(@Param("code") String code, @Param("tenantId") UUID tenantId);

    /** Pour l'ÉCRITURE : seul un référentiel appartenant au tenant est modifiable. */
    @Query("select s from Standard s where s.id = :id and s.ownerTenantId = :tenantId")
    Optional<Standard> findOwnedById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("select count(s) > 0 from Standard s where s.sourceDocumentId = :documentId")
    boolean existsBySourceDocument(@Param("documentId") UUID documentId);

    Standard save(Standard standard);

    void delete(Standard standard);
}
