package com.openlab.qualitos.quality.airouting.infrastructure;

import com.openlab.qualitos.quality.airouting.application.AiPolicyAuditPublisher;
import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;

import java.util.Optional;
import java.util.UUID;

/**
 * Route les changements de politique IA vers le journal d'audit chaîné. La charge
 * utile ne porte que des noms de fournisseurs, et l'indication on-premise : de quoi
 * prouver, devant un auditeur, depuis quand les prompts du tenant restent chez lui.
 */
public class AuditLogAiPolicyPublisher implements AiPolicyAuditPublisher {

    static final String RESOURCE = "ai-policy";
    static final String PINNED = "ai.policy.pinned";
    static final String RELEASED = "ai.policy.released";

    private final AuditEventService auditEvents;

    public AuditLogAiPolicyPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void pinned(UUID tenantId, AiProvider provider, Optional<AiProvider> previous, UUID actor) {
        String payload = "{\"provider\":\"" + provider.wireName() + "\""
                + ",\"onPremise\":" + provider.isOnPremise()
                + ",\"previous\":" + previous.map(p -> "\"" + p.wireName() + "\"").orElse("null")
                + "}";
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, PINNED, RESOURCE, tenantId,
                "Fournisseur d'IA du tenant : " + provider.wireName(), payload, null, null));
    }

    @Override
    public void released(UUID tenantId, AiProvider previous, UUID actor) {
        String payload = "{\"previous\":\"" + previous.wireName() + "\"}";
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, RELEASED, RESOURCE, tenantId,
                "Fournisseur d'IA du tenant : retour au défaut de la plateforme", payload, null, null));
    }
}
