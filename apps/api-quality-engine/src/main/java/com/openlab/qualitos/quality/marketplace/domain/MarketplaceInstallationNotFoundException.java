package com.openlab.qualitos.quality.marketplace.domain;

import java.util.UUID;

public class MarketplaceInstallationNotFoundException extends RuntimeException {
    public MarketplaceInstallationNotFoundException(UUID installationId) {
        super("Marketplace installation not found: " + installationId);
    }
}
