package com.openlab.qualitos.quality.breach.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.breach.application.BreachEventPublisher;
import com.openlab.qualitos.quality.breach.domain.BreachIncident;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Adapter — route les événements de violation vers le journal d'audit immuable.
 * Wire format : {@code gdpr.breach.<action>}.
 *
 * Privacy : seules les métadonnées (severity, status, nombre estimé de
 * personnes affectées) figurent dans le payload — jamais d'identifiants
 * personnels (OWASP A02, RGPD Art. 32).
 */
@Component
public class AuditLogBreachEventPublisher implements BreachEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-breach-incident";

    private final AuditEventService auditEvents;

    public AuditLogBreachEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(BreachIncident i, Action action) {
        String wire = "gdpr.breach." + action.name().toLowerCase(Locale.ROOT);
        UUID actor = i.getHandledByUserId() != null
                ? i.getHandledByUserId() : i.getReportedByUserId();
        String payload = "{"
                + "\"reference\":\"" + i.getInternalReference() + "\""
                + ",\"severity\":\"" + i.getSeverity() + "\""
                + ",\"status\":\"" + i.getStatus() + "\""
                + ",\"affectedCount\":" + i.getAffectedSubjectsCount()
                + ",\"dpaDeadlineAt\":\"" + i.getDpaDeadlineAt() + "\""
                + ",\"dpaNotified\":" + (i.getDpaNotifiedAt() != null)
                + ",\"subjectsNotified\":" + (i.getSubjectsNotifiedAt() != null)
                + "}";
        String summary = action.name() + " — " + i.getInternalReference()
                + " (" + i.getSeverity() + ")";
        auditEvents.recordForTenant(i.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, actor,
                        wire, RESOURCE_TYPE, i.getId(),
                        summary, payload, null, null));
    }
}
