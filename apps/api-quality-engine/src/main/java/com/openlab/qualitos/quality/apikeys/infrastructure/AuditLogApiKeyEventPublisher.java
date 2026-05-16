package com.openlab.qualitos.quality.apikeys.infrastructure;

import com.openlab.qualitos.quality.apikeys.application.ApiKeyEventPublisher;
import com.openlab.qualitos.quality.apikeys.domain.ApiKey;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Route les événements de clés API vers le journal d'audit (OWASP A09).
 * Wire format : {@code apikey.<action>}. Ne logue JAMAIS le secret/hash.
 */
@Component
public class AuditLogApiKeyEventPublisher implements ApiKeyEventPublisher {

    static final String RESOURCE_TYPE = "api-key";

    private final AuditEventService auditEvents;

    public AuditLogApiKeyEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(ApiKey key, Action action) {
        String wire = "apikey." + action.name().toLowerCase(Locale.ROOT);
        auditEvents.recordForTenant(key.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER,
                        action == Action.REVOKED ? key.getRevokedBy() : key.getCreatedBy(),
                        wire, RESOURCE_TYPE, key.getId(),
                        key.getName() + " [" + key.getPrefix() + "] → " + key.getStatus(),
                        null, null, null));
    }
}
