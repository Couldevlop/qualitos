package com.openlab.qualitos.quality.airouting.application;

import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.airouting.domain.AiProviderNotConfiguredException;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicy;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicyRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Paramétrage, par l'administrateur du tenant, du fournisseur qui reçoit ses appels
 * LLM (ADR 0102, §12.2).
 *
 * <p>Ne porte que sur le tenant du jeton : aucun tenant ne lit ni n'écrit la
 * politique d'un autre (OWASP A01). Chaque changement est journalisé.
 */
public class AiPolicyAdminService {

    private final TenantAiPolicyRepository repository;
    private final AiProviderCatalog catalog;
    private final AiRoutingContext context;
    private final AiPolicyAuditPublisher audit;
    private final Clock clock;

    public AiPolicyAdminService(TenantAiPolicyRepository repository, AiProviderCatalog catalog,
                                AiRoutingContext context, AiPolicyAuditPublisher audit, Clock clock) {
        this.repository = repository;
        this.catalog = catalog;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    public AiPolicyDto.View view() {
        UUID tenant = context.requireTenantId();
        return render(repository.findByTenantId(tenant), catalog.describe());
    }

    /**
     * Épingle le tenant sur un fournisseur. Refusé si l'ai-service le déclare non
     * configuré ; accepté s'il ne répond pas, faute de pouvoir le savoir.
     */
    public AiPolicyDto.View pin(AiPolicyDto.PinCommand command) {
        UUID tenant = context.requireTenantId();
        UUID actor = context.requireActorId();
        AiProvider provider = AiProvider.parse(command.provider());
        Optional<AiProviderCatalog.Snapshot> snapshot = catalog.describe();
        if (snapshot.map(s -> s.refuses(provider)).orElse(false)) {
            throw new AiProviderNotConfiguredException(provider);
        }
        Optional<AiProvider> previous = repository.findByTenantId(tenant).map(TenantAiPolicy::getProvider);
        TenantAiPolicy saved = repository.save(new TenantAiPolicy(tenant, provider, Instant.now(clock), actor));
        if (previous.isEmpty() || previous.get() != provider) {
            audit.pinned(tenant, provider, previous, actor);
        }
        return render(Optional.of(saved), snapshot);
    }

    /** Rend le tenant au défaut de la plateforme. Sans politique, ne fait rien. */
    public AiPolicyDto.View release() {
        UUID tenant = context.requireTenantId();
        UUID actor = context.requireActorId();
        Optional<TenantAiPolicy> existing = repository.findByTenantId(tenant);
        if (existing.isPresent()) {
            repository.deleteByTenantId(tenant);
            audit.released(tenant, existing.get().getProvider(), actor);
        }
        return render(Optional.empty(), catalog.describe());
    }

    private static AiPolicyDto.View render(Optional<TenantAiPolicy> policy,
                                           Optional<AiProviderCatalog.Snapshot> snapshot) {
        Optional<AiProvider> platformDefault = snapshot.flatMap(AiProviderCatalog.Snapshot::platformDefault);
        Optional<AiProvider> pinned = policy.map(TenantAiPolicy::getProvider);
        Optional<AiProvider> effective = pinned.isPresent() ? pinned : platformDefault;
        List<AiPolicyDto.ProviderOption> options = Arrays.stream(AiProvider.values())
                .map(p -> new AiPolicyDto.ProviderOption(p.wireName(), p.isOnPremise(),
                        snapshot.map(s -> s.configured().get(p)).orElse(null)))
                .toList();
        return new AiPolicyDto.View(
                pinned.isPresent(),
                pinned.map(AiProvider::wireName).orElse(null),
                effective.map(AiProvider::wireName).orElse(null),
                platformDefault.map(AiProvider::wireName).orElse(null),
                snapshot.isPresent(),
                options,
                policy.map(TenantAiPolicy::getUpdatedAt).orElse(null),
                policy.map(TenantAiPolicy::getUpdatedBy).orElse(null));
    }
}
