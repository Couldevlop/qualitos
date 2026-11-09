package com.openlab.qualitos.quality.settings.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TenantCatalogEntryRepository extends JpaRepository<TenantCatalogEntryRow, UUID> {

    List<TenantCatalogEntryRow> findByTenantIdAndCatalogueOrderByPositionAsc(UUID tenantId, String catalogue);

    void deleteByTenantIdAndCatalogue(UUID tenantId, String catalogue);

    /** Les listes que ce client a réglées : de quoi marquer l'écran sans tout charger. */
    @Query("select distinct e.catalogue from TenantCatalogEntryRow e where e.tenantId = :tenantId")
    List<String> findCustomisedCatalogues(@Param("tenantId") UUID tenantId);
}
