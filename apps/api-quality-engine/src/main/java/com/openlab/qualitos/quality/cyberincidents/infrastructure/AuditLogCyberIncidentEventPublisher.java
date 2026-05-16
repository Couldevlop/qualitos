package com.openlab.qualitos.quality.cyberincidents.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.cyberincidents.application.CyberIncidentEventPublisher;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Adapter — route les événements d'incident cyber vers le journal d'audit.
 * Wire format : {@code nis2.cyber.<action>}.
 * Privacy : pas de PII (l'agrégat décrit l'incident, pas les utilisateurs affectés).
 */
@Component
public class AuditLogCyberIncidentEventPublisher implements CyberIncidentEventPublisher {

    static final String RESOURCE_TYPE = "nis2-cyber-incident";

    private final AuditEventService auditEvents;

    public AuditLogCyberIncidentEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(CyberIncident i, Action action) {
        String wire = "nis2.cyber." + action.name().toLowerCase(Locale.ROOT);
        UUID actor = i.getHandledByUserId() != null
                ? i.getHandledByUserId() : i.getReportedByUserId();
        String payload = "{"
                + "\"reference\":\"" + i.getReference() + "\""
                + ",\"type\":\"" + i.getIncidentType() + "\""
                + ",\"severity\":\"" + i.getSeverity() + "\""
                + ",\"status\":\"" + i.getStatus() + "\""
                + ",\"affectedUsers\":" + i.getEstimatedAffectedUsers()
                + ",\"earlyWarningSent\":" + (i.getEarlyWarningSentAt() != null)
                + ",\"initialAssessmentSent\":" + (i.getInitialAssessmentSentAt() != null)
                + ",\"finalReportSent\":" + (i.getFinalReportSentAt() != null)
                + ",\"linkedToGdprBreach\":" + (i.getLinkedBreachId() != null)
                + "}";
        String summary = action.name() + " — " + i.getReference()
                + " (" + i.getIncidentType() + "/" + i.getSeverity() + ")";
        auditEvents.recordForTenant(i.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, actor,
                        wire, RESOURCE_TYPE, i.getId(),
                        summary, payload, null, null));
    }
}
