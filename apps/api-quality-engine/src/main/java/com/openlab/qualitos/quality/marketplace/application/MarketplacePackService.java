package com.openlab.qualitos.quality.marketplace.application;

import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackNotFoundException;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackRepository;
import com.openlab.qualitos.quality.marketplace.domain.MarketplacePackStateException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — marketplace catalog.
 *
 *  - list: every authenticated user can browse VERIFIED packs only
 *  - register: only super admin
 *  - verify: only super admin (separate step, after signature check)
 */
public class MarketplacePackService {

    private final MarketplacePackRepository repo;
    private final SuperAdminProvider superAdmin;
    private final Clock clock;

    public MarketplacePackService(MarketplacePackRepository repo,
                                  SuperAdminProvider superAdmin,
                                  Clock clock) {
        this.repo = repo;
        this.superAdmin = superAdmin;
        this.clock = clock;
    }

    public List<MarketplacePackDto.View> listVerified(String sectorFilter) {
        return repo.findVerified(sectorFilter).stream()
                .map(MarketplacePackDto.View::of)
                .toList();
    }

    public MarketplacePackDto.View get(UUID id) {
        MarketplacePack p = repo.findById(id)
                .orElseThrow(() -> new MarketplacePackNotFoundException(id));
        return MarketplacePackDto.View.of(p);
    }

    public MarketplacePackDto.View register(MarketplacePackDto.RegisterRequest req) {
        superAdmin.requireSuperAdminId();
        if (repo.existsByPackIdAndVersion(req.packId(), req.version())) {
            throw new MarketplacePackStateException(
                    "pack already registered: " + req.packId() + " v" + req.version());
        }
        Instant now = Instant.now(clock);
        MarketplacePack p = MarketplacePack.register(
                req.packId(), req.version(), req.publisher(), req.title(),
                req.description(), req.sector(), req.priceCents(), req.currency(),
                req.manifestUrl(), req.signatureHash(), now);
        return MarketplacePackDto.View.of(repo.save(p));
    }

    public MarketplacePackDto.View verify(UUID id) {
        UUID superAdminId = superAdmin.requireSuperAdminId();
        MarketplacePack p = repo.findById(id)
                .orElseThrow(() -> new MarketplacePackNotFoundException(id));
        p.verify(superAdminId, Instant.now(clock));
        return MarketplacePackDto.View.of(repo.save(p));
    }
}
