package com.openlab.qualitos.quality.ropa.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.ropa.application.ProcessingActivityEventPublisher;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivity;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements RoPA vers le journal d'audit immuable.
 * Wire format : {@code gdpr.ropa.<action>}.
 * Le payload contient les métadonnées structurelles (reference / lawfulBasis /
 * special / third-country transfer flag) — jamais de contenu textuel libre
 * (purposes, controllerContact…) afin de limiter la surface PII en audit.
 */
@Component
public class AuditLogProcessingActivityEventPublisher implements ProcessingActivityEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-processing-activity";

    private final AuditEventService auditEvents;

    public AuditLogProcessingActivityEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(ProcessingActivity a, Action action) {
        String wire = "gdpr.ropa." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + a.getReference() + "\""
                + ",\"lawfulBasis\":\"" + a.getLawfulBasis() + "\""
                + ",\"status\":\"" + a.getStatus() + "\""
                + ",\"specialCategories\":" + a.isSpecialCategoriesProcessed()
                + ",\"thirdCountryTransfers\":" + (!a.getThirdCountryTransfers().isEmpty())
                + "}";
        String summary = action.name() + " — " + a.getReference()
                + " (lawful=" + a.getLawfulBasis() + ")";
        auditEvents.recordForTenant(a.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, a.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, a.getId(),
                        summary, payload, null, null));
    }
}
