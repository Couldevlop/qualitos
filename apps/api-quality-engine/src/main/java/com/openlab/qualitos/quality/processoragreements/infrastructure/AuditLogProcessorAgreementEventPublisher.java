package com.openlab.qualitos.quality.processoragreements.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.processoragreements.application.ProcessorAgreementEventPublisher;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements DPA vers le journal d'audit immuable.
 * Wire format : {@code gdpr.dpa.<action>}.
 * Action EXPIRED → ActorType.SYSTEM (action automatisée).
 */
@Component
public class AuditLogProcessorAgreementEventPublisher implements ProcessorAgreementEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-processor-agreement";

    private final AuditEventService auditEvents;

    public AuditLogProcessorAgreementEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(ProcessorAgreement a, Action action) {
        String wire = "gdpr.dpa." + action.name().toLowerCase(Locale.ROOT);
        ActorType actorType = action == Action.EXPIRED ? ActorType.SYSTEM : ActorType.USER;
        String payload = "{"
                + "\"reference\":\"" + a.getReference() + "\""
                + ",\"processor\":\"" + a.getProcessorName() + "\""
                + ",\"status\":\"" + a.getStatus() + "\""
                + ",\"thirdCountryTransfer\":" + (!a.getThirdCountryTransfers().isEmpty())
                + ",\"breachSLAHours\":" + a.getBreachNotificationCommitmentHours()
                + "}";
        String summary = action.name() + " — " + a.getReference()
                + " (" + a.getProcessorName() + ")";
        auditEvents.recordForTenant(a.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, actorType,
                        actorType == ActorType.SYSTEM ? null : a.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, a.getId(),
                        summary, payload, null, null));
    }
}
