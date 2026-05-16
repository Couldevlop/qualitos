package com.openlab.qualitos.quality.tenantmodules.application;

import com.openlab.qualitos.quality.tenantmodules.domain.ActivationStatus;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationNotFoundException;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationRepository;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationStateException;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleCatalog;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleCatalogEntry;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases §10.4 — activations modules par tenant avec :
 *  - validation catalogue (code + tier minimum)
 *  - validation dépendances (modules requis doivent être enabled)
 *  - garde-fous core (un module core ne peut être DISABLED qu'avec confirmation forte côté API)
 *  - publication d'événements sur chaque mutation
 *  - expiration en masse (scheduler-callable)
 */
public class ModuleActivationService {

    private final ModuleActivationRepository repo;
    private final TenantProvider tenantProvider;
    private final TenantTierProvider tierProvider;
    private final ModuleActivationEventPublisher events;
    private final Clock clock;

    public ModuleActivationService(ModuleActivationRepository repo,
                                   TenantProvider tenantProvider,
                                   TenantTierProvider tierProvider,
                                   Clock clock) {
        this(repo, tenantProvider, tierProvider, new ModuleActivationEventPublisher.NoOp(), clock);
    }

    public ModuleActivationService(ModuleActivationRepository repo,
                                   TenantProvider tenantProvider,
                                   TenantTierProvider tierProvider,
                                   ModuleActivationEventPublisher events,
                                   Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.tierProvider = tierProvider;
        this.events = events;
        this.clock = clock;
    }

    // ----- Catalogue -----

    public List<ModuleActivationDto.CatalogEntryView> listCatalog() {
        return ModuleCatalog.all().stream().map(ModuleActivationDto.CatalogEntryView::of).toList();
    }

    // ----- Lifecycle -----

    public ModuleActivationDto.ActivationView startTrial(ModuleActivationDto.StartTrialRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        ModuleCatalogEntry entry = ensureKnownAndNoExisting(tenantId, req.moduleCode());
        ensureTierAllowed(tenantId, entry);
        ensureDependenciesSatisfied(tenantId, entry);
        Instant now = Instant.now(clock);
        ModuleActivation a = ModuleActivation.startTrial(
                tenantId, entry.code(), entry.minimumTier(),
                req.trialEndsAt(), req.actor(), now);
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.TRIAL_STARTED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView activate(ModuleActivationDto.ActivateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        ModuleCatalogEntry entry = ensureKnownAndNoExisting(tenantId, req.moduleCode());
        ensureTierAllowed(tenantId, entry);
        ensureDependenciesSatisfied(tenantId, entry);
        Instant now = Instant.now(clock);
        ModuleActivation a = ModuleActivation.activateNow(
                tenantId, entry.code(), entry.minimumTier(),
                req.expiresAt(), req.actor(), now);
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.ACTIVATED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView convertTrial(UUID id,
                                                           ModuleActivationDto.ConvertTrialRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.convertTrialToActive(req.expiresAt(), req.actor(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.ACTIVATED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView suspend(UUID id, ModuleActivationDto.SuspendRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.suspend(req.actor(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.SUSPENDED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView resume(UUID id, ModuleActivationDto.ResumeRequest req) {
        ModuleActivation a = loadForTenant(id);
        // S'assurer que les dépendances sont toujours actives
        ensureDependenciesSatisfied(a.getTenantId(), ModuleCatalog.require(a.getModuleCode()));
        a.resume(req.actor(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.RESUMED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView disable(UUID id, ModuleActivationDto.DisableRequest req) {
        ModuleActivation a = loadForTenant(id);
        ModuleCatalogEntry entry = ModuleCatalog.require(a.getModuleCode());
        if (entry.coreModule()) {
            throw new ModuleActivationStateException(
                    "Cannot disable a core module: " + entry.code());
        }
        ensureNoDependentModulesEnabled(a.getTenantId(), entry.code());
        a.disable(req.actor(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.DISABLED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView expire(UUID id, ModuleActivationDto.ExpireRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.expire(req.actor(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.EXPIRED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView changeTier(UUID id,
                                                          ModuleActivationDto.ChangeTierRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.changeTier(req.newTier(), req.actor(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.TIER_CHANGED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView configure(UUID id,
                                                         ModuleActivationDto.ConfigureRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.configure(req.configurationJson(), req.actor(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.CONFIGURED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    /** Scheduler-callable : passe en EXPIRED les activations dont la date est due. */
    public int expireDue(int limit) {
        Instant now = Instant.now(clock);
        List<ModuleActivation> due = repo.findDueForExpiration(now, Math.max(1, Math.min(limit, 500)));
        int expired = 0;
        for (ModuleActivation a : due) {
            if (a.expireIfDue(now)) {
                repo.save(a);
                events.publish(a, ModuleActivationEventPublisher.Action.EXPIRED);
                expired++;
            }
        }
        return expired;
    }

    // ----- Queries -----

    public ModuleActivationDto.ActivationView get(UUID id) {
        return ModuleActivationDto.ActivationView.of(loadForTenant(id));
    }

    public List<ModuleActivationDto.ActivationView> listForCurrentTenant() {
        UUID tenantId = tenantProvider.requireTenantId();
        return repo.findAllByTenantId(tenantId).stream()
                .map(ModuleActivationDto.ActivationView::of).toList();
    }

    public boolean isEnabled(String moduleCode) {
        UUID tenantId = tenantProvider.requireTenantId();
        return repo.findOpenByTenantIdAndCode(tenantId, moduleCode)
                .map(ModuleActivation::isEnabled).orElse(false);
    }

    public ModuleActivationDto.TenantModuleSummary summary() {
        UUID tenantId = tenantProvider.requireTenantId();
        BillingTier tier = tierProvider.currentTier(tenantId);
        List<ModuleActivation> all = repo.findAllByTenantId(tenantId);
        int trial = 0, active = 0, suspended = 0, expired = 0, disabled = 0, enabled = 0;
        for (ModuleActivation a : all) {
            switch (a.getStatus()) {
                case TRIAL -> trial++;
                case ACTIVE -> active++;
                case SUSPENDED -> suspended++;
                case EXPIRED -> expired++;
                case DISABLED -> disabled++;
            }
            if (a.isEnabled()) enabled++;
        }
        return new ModuleActivationDto.TenantModuleSummary(
                tenantId, tier, all.size(), enabled,
                trial, active, suspended, expired, disabled,
                all.stream().map(ModuleActivationDto.ActivationView::of).toList());
    }

    // ----- Guards -----

    private ModuleCatalogEntry ensureKnownAndNoExisting(UUID tenantId, String code) {
        ModuleCatalogEntry entry = ModuleCatalog.require(code);
        repo.findOpenByTenantIdAndCode(tenantId, code).ifPresent(existing -> {
            throw new ModuleActivationStateException(
                    "Module already has an open activation: " + code + " (status="
                            + existing.getStatus() + ")");
        });
        return entry;
    }

    private void ensureTierAllowed(UUID tenantId, ModuleCatalogEntry entry) {
        BillingTier current = tierProvider.currentTier(tenantId);
        if (current.compareTo(entry.minimumTier()) < 0) {
            throw new ModuleActivationStateException(
                    "Tenant tier " + current + " is below required " + entry.minimumTier()
                            + " for module " + entry.code());
        }
    }

    private void ensureDependenciesSatisfied(UUID tenantId, ModuleCatalogEntry entry) {
        for (String dep : entry.dependencies()) {
            boolean ok = repo.findOpenByTenantIdAndCode(tenantId, dep)
                    .map(ModuleActivation::isEnabled).orElse(false);
            if (!ok) {
                throw new ModuleActivationStateException(
                        "Missing dependency for " + entry.code() + ": " + dep + " must be enabled");
            }
        }
    }

    private void ensureNoDependentModulesEnabled(UUID tenantId, String code) {
        for (ModuleActivation a : repo.findEnabledByTenantId(tenantId)) {
            ModuleCatalog.find(a.getModuleCode()).ifPresent(other -> {
                if (other.dependencies().contains(code)) {
                    throw new ModuleActivationStateException(
                            "Cannot disable " + code + " — required by " + other.code());
                }
            });
        }
    }

    private ModuleActivation loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        ModuleActivation a = repo.findById(id)
                .orElseThrow(() -> new ModuleActivationNotFoundException(id.toString()));
        if (!a.getTenantId().equals(tenantId)) {
            throw new ModuleActivationNotFoundException(id.toString());
        }
        return a;
    }
}
