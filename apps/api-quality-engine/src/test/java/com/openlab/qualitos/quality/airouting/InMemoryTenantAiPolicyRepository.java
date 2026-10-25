package com.openlab.qualitos.quality.airouting;

import com.openlab.qualitos.quality.airouting.application.TenantAiPolicyResolver;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicy;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicyRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Dépôt en mémoire, pour les tests qui n'ont pas besoin de la base. */
public class InMemoryTenantAiPolicyRepository implements TenantAiPolicyRepository {

    private final Map<UUID, TenantAiPolicy> rows = new HashMap<>();

    /** Le résolveur d'un tenant sans politique : la passerelle se comporte comme avant. */
    public static TenantAiPolicyResolver noPolicy() {
        return new TenantAiPolicyResolver(new InMemoryTenantAiPolicyRepository());
    }

    @Override
    public Optional<TenantAiPolicy> findByTenantId(UUID tenantId) {
        return Optional.ofNullable(rows.get(tenantId));
    }

    @Override
    public TenantAiPolicy save(TenantAiPolicy policy) {
        rows.put(policy.getTenantId(), policy);
        return policy;
    }

    @Override
    public void deleteByTenantId(UUID tenantId) {
        rows.remove(tenantId);
    }

    public int size() {
        return rows.size();
    }
}
