package com.openlab.qualitos.quality.standards.auditblanc.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditEventPublisher;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter — route l'exécution d'un audit blanc vers le journal d'audit chaîné
 * (OWASP A09). Wire format : {@code standards.audit-blanc.executed}. Aucune PII :
 * seules des métadonnées (code norme, scores, décomptes) sont journalisées.
 */
@Component
public class AuditLogMockAuditEventPublisher implements MockAuditEventPublisher {

    static final String RESOURCE_TYPE = "standard-mock-audit";
    static final String WIRE = "standards.audit-blanc.executed";

    private final AuditEventService auditEvents;

    public AuditLogMockAuditEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void published(MockAuditRun run) {
        String payload = "{"
                + "\"standardCode\":\"" + run.getStandardCode() + "\""
                + ",\"adoptionId\":\"" + run.getAdoptionId() + "\""
                + ",\"readiness\":" + run.getReadiness()
                + ",\"major\":" + run.getMajorCount()
                + ",\"minor\":" + run.getMinorCount()
                + ",\"observations\":" + run.getObservationCount()
                + ",\"questions\":" + run.getQuestionCount()
                + ",\"remediationActions\":" + run.getRemediationPlan().size()
                + "}";
        String summary = "Audit blanc IA — " + run.getStandardCode()
                + " (" + run.getMajorCount() + " NC majeure(s), "
                + run.getMinorCount() + " mineure(s))";
        UUID actor = run.getCreatedByUserId();
        auditEvents.recordForTenant(run.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, actor,
                        WIRE, RESOURCE_TYPE, run.getId(),
                        summary, payload, null, null));
    }
}
