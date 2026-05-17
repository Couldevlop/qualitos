package com.openlab.qualitos.quality.aiconformity.infrastructure;

import com.openlab.qualitos.quality.aiconformity.application.ConformityAssessmentEventPublisher;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;
import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter — route les événements de conformité vers le journal d'audit.
 * Wire format : {@code ai.conformity.<action>}.
 */
@Component
public class AuditLogConformityAssessmentEventPublisher
        implements ConformityAssessmentEventPublisher {

    static final String RESOURCE_TYPE = "ai-act-conformity";

    private final AuditEventService auditEvents;

    public AuditLogConformityAssessmentEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(ConformityAssessment a, Action action) {
        String wire = "ai.conformity." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"reference\":\"" + a.getReference() + "\""
                + ",\"aiSystemId\":\"" + a.getAiSystemId() + "\""
                + ",\"procedure\":\"" + a.getProcedure() + "\""
                + ",\"status\":\"" + a.getStatus() + "\""
                + ",\"hasNotifiedBody\":" + (a.getNotifiedBodyId() != null)
                + ",\"hasCertificate\":" + (a.getCertificateNumber() != null)
                + "}";
        String summary = action.name() + " — " + a.getReference()
                + " (" + a.getProcedure() + ")";
        auditEvents.recordForTenant(a.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, a.getCreatedByUserId(),
                        wire, RESOURCE_TYPE, a.getId(),
                        summary, payload, null, null));
    }
}
