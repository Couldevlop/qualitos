package com.openlab.qualitos.quality.crossbordertransfers.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.crossbordertransfers.application.CrossBorderTransferEventPublisher;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements de transfert vers le journal d'audit immuable.
 * Wire format : {@code gdpr.transfer.<action>}.
 */
@Component
public class AuditLogCrossBorderTransferEventPublisher implements CrossBorderTransferEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-cross-border-transfer";

    private final AuditEventService auditEvents;

    public AuditLogCrossBorderTransferEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(CrossBorderTransfer t, Action action) {
        String wire = "gdpr.transfer." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + t.getReference() + "\""
                + ",\"recipient\":\"" + t.getRecipientName() + "\""
                + ",\"mechanism\":\"" + t.getMechanism() + "\""
                + ",\"countries\":" + t.getDestinationCountries().size()
                + ",\"status\":\"" + t.getStatus() + "\""
                + "}";
        String summary = action.name() + " — " + t.getReference()
                + " (" + t.getMechanism() + ")";
        auditEvents.recordForTenant(t.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, t.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, t.getId(),
                        summary, payload, null, null));
    }
}
