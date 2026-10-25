package com.openlab.qualitos.quality.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, UUID>,
        JpaSpecificationExecutor<SupplierDocument> {

    List<SupplierDocument> findByTenantIdAndOwnerTypeAndOwnerIdOrderByCreatedAtAsc(
            UUID tenantId, SupplierDocumentOwnerType ownerType, UUID ownerId);

    Optional<SupplierDocument> findByIdAndTenantIdAndOwnerTypeAndOwnerId(
            UUID id, UUID tenantId, SupplierDocumentOwnerType ownerType, UUID ownerId);

    List<SupplierDocument> findByTenantIdAndSupplierId(UUID tenantId, UUID supplierId);

    long countByTenantIdAndOwnerTypeAndOwnerId(UUID tenantId, SupplierDocumentOwnerType ownerType, UUID ownerId);

    /** Poids déjà joint au porteur, calculé en base plutôt que sur une liste chargée. */
    @Query("select coalesce(sum(d.sizeBytes), 0) from SupplierDocument d "
            + "where d.tenantId = :tenantId and d.ownerType = :ownerType and d.ownerId = :ownerId")
    long sumSizeBytes(UUID tenantId, SupplierDocumentOwnerType ownerType, UUID ownerId);

    /** Nombre de documents par porteur, en une requête pour toute une page de liste. */
    @Query("select d.ownerId, count(d) from SupplierDocument d "
            + "where d.tenantId = :tenantId and d.ownerType = :ownerType and d.ownerId in :ownerIds "
            + "group by d.ownerId")
    List<Object[]> countByOwners(UUID tenantId, SupplierDocumentOwnerType ownerType, Collection<UUID> ownerIds);

    /**
     * Existence par clé d'objet, sans filtre de tenant — pour le balayage des
     * orphelins, qui s'exécute hors requête. La clé porte déjà le tenant.
     */
    boolean existsByObjectKey(String objectKey);
}
