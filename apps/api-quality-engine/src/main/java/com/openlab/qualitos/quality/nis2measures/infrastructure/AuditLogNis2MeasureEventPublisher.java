package com.openlab.qualitos.quality.nis2measures.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.nis2measures.application.Nis2MeasureEventPublisher;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Adapter — route les événements de mesure NIS2 vers le journal d'audit.
 * Wire format : {@code nis2.measure.<action>}.
 */
@Component
public class AuditLogNis2MeasureEventPublisher implements Nis2MeasureEventPublisher {

    static final String RESOURCE_TYPE = "nis2-risk-measure";

    private final AuditEventService auditEvents;

    public AuditLogNis2MeasureEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(Nis2RiskMeasure m, Action action) {
        String wire = "nis2.measure." + action.name().toLowerCase(Locale.ROOT);
        UUID actor = (action == Action.VERIFIED || action == Action.REVIEWED)
                && m.getReviewedByUserId() != null
                ? m.getReviewedByUserId()
                : (m.getOwnerUserId() != null ? m.getOwnerUserId() : m.getCreatedByUserId());
        String payload = "{"
                + "\"reference\":\"" + m.getReference() + "\""
                + ",\"category\":\"" + m.getCategory() + "\""
                + ",\"status\":\"" + m.getStatus() + "\""
                + ",\"maturity\":" + m.getMaturityLevel()
                + ",\"residualRisk\":\"" + m.getResidualRiskRating() + "\""
                + "}";
        String summary = action.name() + " — " + m.getReference()
                + " (" + m.getCategory() + ", maturity=" + m.getMaturityLevel() + ")";
        auditEvents.recordForTenant(m.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, actor,
                        wire, RESOURCE_TYPE, m.getId(),
                        summary, payload, null, null));
    }
}
