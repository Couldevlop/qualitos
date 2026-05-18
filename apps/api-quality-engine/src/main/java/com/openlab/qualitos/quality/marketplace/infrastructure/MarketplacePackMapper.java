package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;

final class MarketplacePackMapper {
    private MarketplacePackMapper() {}

    static MarketplacePack toDomain(MarketplacePackJpaEntity e) {
        return new MarketplacePack(
                e.getId(), e.getPackId(), e.getVersion(),
                e.getPublisher(), e.getTitle(), e.getDescription(),
                e.getSector(), e.getPriceCents(), e.getCurrency(),
                e.isVerified(), e.getVerifiedBy(), e.getVerifiedAt(),
                e.getSignatureHash(), e.getManifestUrl(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    static MarketplacePackJpaEntity toEntity(MarketplacePack p) {
        MarketplacePackJpaEntity e = new MarketplacePackJpaEntity();
        e.setId(p.getId());
        e.setPackId(p.getPackId());
        e.setVersion(p.getVersion());
        e.setPublisher(p.getPublisher());
        e.setTitle(p.getTitle());
        e.setDescription(p.getDescription());
        e.setSector(p.getSector());
        e.setPriceCents(p.getPriceCents());
        e.setCurrency(p.getCurrency());
        e.setVerified(p.isVerified());
        e.setVerifiedBy(p.getVerifiedBy());
        e.setVerifiedAt(p.getVerifiedAt());
        e.setSignatureHash(p.getSignatureHash());
        e.setManifestUrl(p.getManifestUrl());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        return e;
    }
}
