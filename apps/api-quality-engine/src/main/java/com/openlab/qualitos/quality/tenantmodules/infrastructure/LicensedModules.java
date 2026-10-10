package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.licensing.application.Licensing;
import com.openlab.qualitos.licensing.domain.License;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleLicense;
import com.openlab.qualitos.quality.tenantmodules.application.TenantTierProvider;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;

import java.util.UUID;

/**
 * Les modules et le palier d'une installation on-premise : ceux de sa licence
 * (ADR 0082). Sans licence lisible, seul le socle reste ouvert, au palier FREE.
 */
public final class LicensedModules implements ModuleLicense, TenantTierProvider {

    private final Licensing licensing;

    public LicensedModules(Licensing licensing) {
        this.licensing = licensing;
    }

    @Override
    public boolean governs() {
        return licensing.isOnPrem();
    }

    @Override
    public boolean allows(String moduleCode) {
        return !licensing.isOnPrem() || licensing.state().allowsModule(moduleCode);
    }

    @Override
    public BillingTier currentTier(UUID tenantId) {
        return licensing.state().licenseOpt()
                .map(License::tier)
                .map(BillingTier::valueOf)
                .orElse(BillingTier.FREE);
    }
}
