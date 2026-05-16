package com.openlab.qualitos.quality.tenantmodules.application;

import com.openlab.qualitos.quality.tenantmodules.domain.ActivationStatus;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleCatalogEntry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ModuleActivationDto {

    private ModuleActivationDto() {}

    public record StartTrialRequest(String moduleCode, Instant trialEndsAt, UUID actor) {}
    public record ActivateRequest(String moduleCode, Instant expiresAt, UUID actor) {}
    public record ConvertTrialRequest(Instant expiresAt, UUID actor) {}
    public record SuspendRequest(UUID actor) {}
    public record ResumeRequest(UUID actor) {}
    public record DisableRequest(UUID actor) {}
    public record ExpireRequest(UUID actor) {}
    public record ChangeTierRequest(BillingTier newTier, UUID actor) {}
    public record ConfigureRequest(String configurationJson, UUID actor) {}

    public record ActivationView(
            UUID id, UUID tenantId, String moduleCode,
            ActivationStatus status, boolean enabled, BillingTier billingTier,
            String configurationJson, Instant trialEndsAt, Instant expiresAt,
            Instant activatedAt, UUID activatedBy,
            Instant statusChangedAt, UUID lastChangedBy, Instant updatedAt
    ) {
        public static ActivationView of(ModuleActivation a) {
            return new ActivationView(
                    a.getId(), a.getTenantId(), a.getModuleCode(),
                    a.getStatus(), a.isEnabled(), a.getBillingTier(),
                    a.getConfigurationJson(), a.getTrialEndsAt(), a.getExpiresAt(),
                    a.getActivatedAt(), a.getActivatedBy(),
                    a.getStatusChangedAt(), a.getLastChangedBy(), a.getUpdatedAt());
        }
    }

    public record CatalogEntryView(
            String code, String name, String category,
            BillingTier minimumTier, List<String> dependencies, boolean coreModule
    ) {
        public static CatalogEntryView of(ModuleCatalogEntry e) {
            return new CatalogEntryView(e.code(), e.name(), e.category(),
                    e.minimumTier(), e.dependencies().stream().sorted().toList(), e.coreModule());
        }
    }

    public record TenantModuleSummary(
            UUID tenantId,
            BillingTier tenantTier,
            int totalActivations,
            int enabledCount,
            int trialCount,
            int activeCount,
            int suspendedCount,
            int expiredCount,
            int disabledCount,
            List<ActivationView> activations
    ) {}
}
