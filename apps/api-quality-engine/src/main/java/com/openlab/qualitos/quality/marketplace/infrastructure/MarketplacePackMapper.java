package com.openlab.qualitos.quality.marketplace.infrastructure;

import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;

final class MarketplacePackMapper {
    private MarketplacePackMapper() {}

    static MarketplacePack toDomain(MarketplacePackJpaEntity e) {
        return new MarketplacePack(
                e.getId(), e.getPackId(), e.getVersion(),
                e.getPublisher(), e.getTitle(), e.getDescription(),
                e.getSector(), e.getNormsCsv(), e.getPriceCents(), e.getCurrency(),
                e.getStatus(), e.getSubmittedBy(), e.getSubmittedAt(),
                e.getReviewedBy(), e.getReviewedAt(), e.getReviewNotes(),
                e.getSignatureHash(), e.getManifestUrl(), e.getManifestJson(),
                e.getRatingAvg(), e.getRatingCount(),
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
        e.setNormsCsv(p.getNormsCsv());
        e.setPriceCents(p.getPriceCents());
        e.setCurrency(p.getCurrency());
        e.setStatus(p.getStatus());
        e.setSubmittedBy(p.getSubmittedBy());
        e.setSubmittedAt(p.getSubmittedAt());
        e.setReviewedBy(p.getReviewedBy());
        e.setReviewedAt(p.getReviewedAt());
        e.setReviewNotes(p.getReviewNotes());
        e.setSignatureHash(p.getSignatureHash());
        e.setManifestUrl(p.getManifestUrl());
        e.setManifestJson(p.getManifestJson());
        e.setRatingAvg(p.getRatingAvg());
        e.setRatingCount(p.getRatingCount());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        return e;
    }
}
