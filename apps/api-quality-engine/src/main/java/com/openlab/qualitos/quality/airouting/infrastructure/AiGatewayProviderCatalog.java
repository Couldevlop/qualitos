package com.openlab.qualitos.quality.airouting.infrastructure;

import com.openlab.qualitos.quality.airouting.application.AiProviderCatalog;
import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.airouting.domain.InvalidAiProviderException;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lit {@code GET /v1/ai/providers} par la passerelle. Un fournisseur que l'engine ne
 * connaît pas est ignoré : on ne propose pas à l'écran ce qu'on ne saurait router.
 */
public class AiGatewayProviderCatalog implements AiProviderCatalog {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayProviderCatalog.class);

    private final AiGatewayClient gateway;

    public AiGatewayProviderCatalog(AiGatewayClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public Optional<Snapshot> describe() {
        Map<String, Object> raw;
        try {
            raw = gateway.providers();
        } catch (AiGatewayException e) {
            log.warn("ai_providers_unavailable reason={}", e.getMessage());
            return Optional.empty();
        }
        Map<AiProvider, Boolean> configured = new EnumMap<>(AiProvider.class);
        if (raw.get("providers") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m && m.get("configured") instanceof Boolean b) {
                    known(m.get("name")).ifPresent(p -> configured.put(p, b));
                }
            }
        }
        return Optional.of(new Snapshot(known(raw.get("default_provider")), configured));
    }

    private static Optional<AiProvider> known(Object name) {
        if (!(name instanceof String s)) {
            return Optional.empty();
        }
        try {
            return Optional.of(AiProvider.parse(s));
        } catch (InvalidAiProviderException e) {
            return Optional.empty();
        }
    }
}
