package com.openlab.qualitos.quality.ehs.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.ehs.application.IncidentEventPublisher;
import com.openlab.qualitos.quality.ehs.domain.Incident;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter qui route les événements EHS vers le journal d'audit immuable
 * ({@link AuditEventService}). L'action est convertie en code dotté
 * {@code ehs.incident.<action>} (convention §11.5).
 *
 * Recordé via {@code recordForTenant} (tenant tiré de l'aggregate) — pas de
 * dépendance au TenantContext ici : le service application a déjà validé.
 */
@Component
public class AuditLogIncidentEventPublisher implements IncidentEventPublisher {

    static final String RESOURCE_TYPE = "ehs-incident";

    private final AuditEventService auditEvents;

    public AuditLogIncidentEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(Incident incident, Action action) {
        String wire = "ehs.incident." + action.name().toLowerCase(Locale.ROOT);
        auditEvents.recordForTenant(incident.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, incident.getOwnerUserId(),
                        wire, RESOURCE_TYPE, incident.getId(),
                        incident.getCode() + " — " + incident.getTitle(),
                        null, null, null));
    }
}
