package com.openlab.qualitos.quality.marketplace.domain;

import java.util.UUID;

public class MarketplacePackNotFoundException extends RuntimeException {
    public MarketplacePackNotFoundException(UUID id) {
        super("Marketplace pack not found: " + id);
    }
    public MarketplacePackNotFoundException(String packId, String version) {
        super("Marketplace pack not found: " + packId + " v" + version);
    }
}
