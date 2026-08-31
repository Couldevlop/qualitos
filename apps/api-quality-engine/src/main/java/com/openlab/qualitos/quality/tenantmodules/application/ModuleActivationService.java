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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ActorProvider actorProvider;
    private final ModuleActivationEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ModuleActivationService(ModuleActivationRepository repo,
                                   TenantProvider tenantProvider,
                                   TenantTierProvider tierProvider,
                                   ActorProvider actorProvider,
                                   Clock clock) {
        this(repo, tenantProvider, tierProvider, actorProvider,
                new ModuleActivationEventPublisher.NoOp(), clock);
    }

    public ModuleActivationService(ModuleActivationRepository repo,
                                   TenantProvider tenantProvider,
                                   TenantTierProvider tierProvider,
                                   ActorProvider actorProvider,
                                   ModuleActivationEventPublisher events,
                                   Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.tierProvider = tierProvider;
        this.actorProvider = actorProvider;
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
                req.trialEndsAt(), actorProvider.requireActorId(), now);
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
                req.expiresAt(), actorProvider.requireActorId(), now);
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.ACTIVATED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView convertTrial(UUID id,
                                                           ModuleActivationDto.ConvertTrialRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.convertTrialToActive(req.expiresAt(), actorProvider.requireActorId(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.ACTIVATED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView suspend(UUID id, ModuleActivationDto.SuspendRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.suspend(actorProvider.requireActorId(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.SUSPENDED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView resume(UUID id, ModuleActivationDto.ResumeRequest req) {
        ModuleActivation a = loadForTenant(id);
        // S'assurer que les dépendances sont toujours actives
        ensureDependenciesSatisfied(a.getTenantId(), ModuleCatalog.require(a.getModuleCode()));
        a.resume(actorProvider.requireActorId(), Instant.now(clock));
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
        a.disable(actorProvider.requireActorId(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.DISABLED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView expire(UUID id, ModuleActivationDto.ExpireRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.expire(actorProvider.requireActorId(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.EXPIRED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView changeTier(UUID id,
                                                          ModuleActivationDto.ChangeTierRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.changeTier(req.newTier(), actorProvider.requireActorId(), Instant.now(clock));
        ModuleActivation saved = repo.save(a);
        events.publish(saved, ModuleActivationEventPublisher.Action.TIER_CHANGED);
        return ModuleActivationDto.ActivationView.of(saved);
    }

    public ModuleActivationDto.ActivationView configure(UUID id,
                                                         ModuleActivationDto.ConfigureRequest req) {
        ModuleActivation a = loadForTenant(id);
        a.configure(req.configurationJson(), actorProvider.requireActorId(), Instant.now(clock));
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

    /**
     * Le module est-il utilisable par le tenant courant ?
     *
     * <p>Une activation explicite fait foi. À défaut, les modules <b>de base</b> du
     * catalogue sont acquis d'office : ce sont ceux que l'on refuse par ailleurs de
     * désactiver (§10.4, « défaut = modules standards »). Sans cette règle, un tenant
     * fraîchement créé n'avait aucun module actif — pas même ceux qu'il lui est
     * interdit de couper — et devait activer à la main ce qui lui revient de droit.
     * Les modules facturés à l'unité, eux, continuent d'exiger une activation.
     */
    public boolean isEnabled(String moduleCode) {
        UUID tenantId = tenantProvider.requireTenantId();
        return repo.findOpenByTenantIdAndCode(tenantId, moduleCode)
                .map(ModuleActivation::isEnabled)
                .orElseGet(() -> ModuleCatalog.find(moduleCode)
                        .map(ModuleCatalogEntry::coreModule)
                        .orElse(false));
    }

    /**
     * Codes des modules dont dispose le tenant courant : ceux qu'une activation
     * ouvre, plus le socle du catalogue, acquis d'office.
     *
     * <p>Existe pour que l'interface sache quoi montrer. La barre de navigation
     * n'était filtrée que par rôle : un tenant réduit au socle voyait toutes les
     * entrées, y compris des modules qu'il n'a pas — et découvrait le refus en
     * ouvrant l'écran. Interroger le point unitaire une fois par entrée aurait
     * produit une vingtaine d'appels au démarrage de l'application.
     */
    public List<String> enabledModuleCodes() {
        return List.copyOf(availableModuleCodes(tenantProvider.requireTenantId()));
    }

    /**
     * Règle UNIQUE de disponibilité d'un module pour un tenant : le socle du
     * catalogue, plus les activations ouvertes, moins celles explicitement fermées.
     *
     * <p>Elle est extraite ici parce que deux endroits en avaient besoin et en
     * avaient chacun leur version. {@code enabledModuleCodes()} comptait le socle ;
     * la garde des dépendances, elle, exigeait une LIGNE d'activation. Or les
     * modules du socle n'en ont aucune : activer {@code risk}, qui dépend de
     * {@code capa}, répondait donc 409 « Missing dependency » sur un tenant où
     * {@code capa} était pourtant bien disponible — et de façon définitive, aucune
     * manœuvre ne pouvant créer la ligne attendue. Le même piège frappait
     * {@code supplier}, {@code change}, {@code complaints}, {@code ehs},
     * {@code standards} et, par ricochet, {@code controlplan}.
     *
     * <p>Les deux appelants partagent désormais cette méthode : l'interface ne peut
     * plus annoncer un module que la garde refuse de considérer.
     *
     * <p><b>Seule la décision COURANTE de chaque module compte.</b> Le dépôt rend
     * l'historique complet, et un module désactivé puis réactivé porte DEUX
     * lignes — le schéma le prévoit, son index d'unicité ne couvrant que les
     * statuts non terminaux. Les parcourir toutes en appliquant tour à tour
     * « ajoute » et « retire » faisait gagner la ligne lue en dernier : avec un
     * tri par date décroissante, c'était la plus ANCIENNE. Une désactivation de
     * la veille effaçait donc l'activation du jour, et le module réactivé ne
     * revenait jamais dans l'interface. La dépendance lisant la même liste, tout
     * module qui s'appuyait sur lui devenait à son tour inactivable, avec un 409
     * « Missing dependency » qu'aucune manœuvre depuis l'écran ne pouvait lever.
     */
    private Set<String> availableModuleCodes(UUID tenantId) {
        Set<String> enabled = new java.util.LinkedHashSet<>();
        for (ModuleCatalogEntry entry : ModuleCatalog.all()) {
            if (entry.coreModule()) {
                enabled.add(entry.code());
            }
        }
        for (ModuleActivation activation : currentDecisionPerModule(tenantId)) {
            if (activation.isEnabled()) {
                enabled.add(activation.getModuleCode());
            } else {
                // Une activation explicitement fermée l'emporte : c'est une
                // décision, pas une absence.
                enabled.remove(activation.getModuleCode());
            }
        }
        return enabled;
    }

    /**
     * La décision qui fait foi pour chaque module : son activation OUVERTE si
     * elle existe, sinon la plus récente de ses lignes fermées.
     *
     * <p>Ce n'est pas « la dernière ligne rendue » : l'ordre de lecture est un
     * détail du dépôt, et fonder une décision d'autorisation dessus revient à
     * tirer à pile ou face à chaque montée de version. La règle vient du schéma
     * lui-même — {@code uk_tma_open_per_tenant_module} garantit AU PLUS UNE ligne
     * non terminale par (tenant, module) — donc l'ouverte, quand il y en a une,
     * est par construction la seule qui décrive l'état présent.
     */
    private Collection<ModuleActivation> currentDecisionPerModule(UUID tenantId) {
        Map<String, ModuleActivation> current = new java.util.LinkedHashMap<>();
        for (ModuleActivation candidate : repo.findAllByTenantId(tenantId)) {
            current.merge(candidate.getModuleCode(), candidate,
                    ModuleActivationService::moreAuthoritative);
        }
        return current.values();
    }

    /** Entre deux lignes du même module, celle qui décrit l'état présent. */
    private static ModuleActivation moreAuthoritative(ModuleActivation kept, ModuleActivation other) {
        if (kept.isTerminal() != other.isTerminal()) {
            return kept.isTerminal() ? other : kept;
        }
        // Deux lignes de même nature : la plus récemment ouverte. Le cas ne se
        // présente qu'entre lignes fermées, l'index n'en tolérant qu'une ouverte.
        return other.getActivatedAt().isAfter(kept.getActivatedAt()) ? other : kept;
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
        if (entry.dependencies().isEmpty()) {
            return;
        }
        // Une seule lecture pour toutes les dépendances, et la MÊME règle que celle
        // qui décide ce que l'interface affiche (cf. availableModuleCodes).
        Set<String> available = availableModuleCodes(tenantId);
        for (String dep : entry.dependencies()) {
            if (!available.contains(dep)) {
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
