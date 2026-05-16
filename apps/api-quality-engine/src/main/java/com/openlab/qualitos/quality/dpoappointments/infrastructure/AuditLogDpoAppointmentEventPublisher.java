package com.openlab.qualitos.quality.dpoappointments.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.dpoappointments.application.DpoAppointmentEventPublisher;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements de désignation DPO vers le journal d'audit.
 * Wire format : {@code gdpr.dpo.<action>}.
 * Privacy : l'email et téléphone du DPO ne figurent PAS dans le payload audit —
 * uniquement le type, le scope et le statut (OWASP A02).
 */
@Component
public class AuditLogDpoAppointmentEventPublisher implements DpoAppointmentEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-dpo-appointment";

    private final AuditEventService auditEvents;

    public AuditLogDpoAppointmentEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(DpoAppointment a, Action action) {
        String wire = "gdpr.dpo." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + a.getReference() + "\""
                + ",\"scope\":\"" + a.getScope() + "\""
                + ",\"dpoType\":\"" + a.getDpoType() + "\""
                + ",\"status\":\"" + a.getStatus() + "\""
                + ",\"regulatorNotified\":" + (a.getRegulatorNotifiedAt() != null)
                + "}";
        String summary = action.name() + " — " + a.getReference()
                + " (scope=" + a.getScope() + ")";
        auditEvents.recordForTenant(a.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, a.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, a.getId(),
                        summary, payload, null, null));
    }
}
