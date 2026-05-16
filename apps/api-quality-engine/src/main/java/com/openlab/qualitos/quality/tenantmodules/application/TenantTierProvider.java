package com.openlab.qualitos.quality.tenantmodules.application;

import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;

import java.util.UUID;

/**
 * Port — résout le tier de facturation actuel d'un tenant (table billing,
 * provider externe…). L'implémentation par défaut renvoie FREE.
 */
public interface TenantTierProvider {
    BillingTier currentTier(UUID tenantId);

    final class FreeByDefault implements TenantTierProvider {
        @Override public BillingTier currentTier(UUID tenantId) { return BillingTier.FREE; }
    }
}
