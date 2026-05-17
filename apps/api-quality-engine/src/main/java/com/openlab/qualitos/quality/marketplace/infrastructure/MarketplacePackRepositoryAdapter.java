package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MarketplacePackRepositoryAdapter implements MarketplacePackRepository {

    private final MarketplacePackJpaRepository jpa;

    public MarketplacePackRepositoryAdapter(MarketplacePackJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MarketplacePack save(MarketplacePack pack) {
        MarketplacePackJpaEntity e = MarketplacePackMapper.toEntity(pack);
        if (e.getId() == null) {
            e.setId(UUID.randomUUID());
        }
        MarketplacePackJpaEntity saved = jpa.save(e);
        if (pack.getId() == null) {
            pack.assignId(saved.getId());
        }
        return MarketplacePackMapper.toDomain(saved);
    }

    @Override
    public Optional<MarketplacePack> findById(UUID id) {
        return jpa.findById(id).map(MarketplacePackMapper::toDomain);
    }

    @Override
    public boolean existsByPackIdAndVersion(String packId, String version) {
        return jpa.existsByPackIdAndVersion(packId, version);
    }

    @Override
    public List<MarketplacePack> findVerified(String sectorFilter) {
        return jpa.findVerified(sectorFilter).stream()
                .map(MarketplacePackMapper::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }
}
