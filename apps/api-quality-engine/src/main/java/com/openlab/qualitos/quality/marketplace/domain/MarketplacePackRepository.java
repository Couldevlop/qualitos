package com.openlab.qualitos.quality.marketplace.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port — persistance des packs marketplace (catalogue cross-tenant).
 */
public interface MarketplacePackRepository {

    MarketplacePack save(MarketplacePack pack);

    Optional<MarketplacePack> findById(UUID id);

    boolean existsByPackIdAndVersion(String packId, String version);

    /** Catalogue PUBLIC : uniquement les packs {@link MarketplacePackStatus#PUBLISHED}. */
    List<MarketplacePack> findPublished(String sectorFilter);

    /** File de l'éditeur filtrée par état (SUBMITTED, IN_REVIEW…). */
    List<MarketplacePack> findByStatus(MarketplacePackStatus status);

    /** File de modération (SUBMITTED + IN_REVIEW), triée par date de soumission. */
    List<MarketplacePack> findModerationQueue();

    void delete(UUID id);
}
