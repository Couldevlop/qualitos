package com.openlab.qualitos.quality.marketplace.application;

import com.openlab.qualitos.quality.marketplace.domain.MarketplacePack;
import java.time.Instant;
import java.util.UUID;

public final class MarketplacePackDto {
    private MarketplacePackDto() {}

    public record RegisterRequest(
            String packId,
            String version,
            String publisher,
            String title,
            String description,
            String sector,
            int priceCents,
            String currency,
            String manifestUrl,
            String signatureHash
    ) {}

    public record View(
            UUID id, String packId, String version,
            String publisher, String title, String description,
            String sector, int priceCents, String currency,
            boolean verified, UUID verifiedBy, Instant verifiedAt,
            String signatureHash, String manifestUrl,
            Instant createdAt, Instant updatedAt) {
        public static View of(MarketplacePack p) {
            return new View(p.getId(), p.getPackId(), p.getVersion(),
                    p.getPublisher(), p.getTitle(), p.getDescription(),
                    p.getSector(), p.getPriceCents(), p.getCurrency(),
                    p.isVerified(), p.getVerifiedBy(), p.getVerifiedAt(),
                    p.getSignatureHash(), p.getManifestUrl(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
