package com.openlab.qualitos.quality.marketplace.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarketplacePackRepository {
    MarketplacePack save(MarketplacePack pack);
    Optional<MarketplacePack> findById(UUID id);
    boolean existsByPackIdAndVersion(String packId, String version);
    List<MarketplacePack> findVerified(String sectorFilter);
    void delete(UUID id);
}
