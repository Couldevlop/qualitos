package com.openlab.qualitos.quality.controlplan.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.controlplan.application.ControlPlanAuditPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Inscrit au journal chaîné du tenant ce qui arrive au control plan.
 *
 * <p>L'acteur est celui que le service a lu du jeton, jamais un champ de requête :
 * une signature dont l'auteur serait falsifiable ne vaudrait rien devant un
 * auditeur (correctif H2 de l'audit du 6 juin).
 */
@Component
public class ControlPlanAuditAdapter implements ControlPlanAuditPort {

    private static final String RESOURCE_TYPE = "control_plan";

    private final AuditEventService auditEvents;

    public ControlPlanAuditAdapter(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void record(UUID tenantId, UUID actorId, String action, UUID planId,
                       String summary, String detailsJson) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actorId == null ? ActorType.SYSTEM : ActorType.USER,
                actorId,
                action,
                RESOURCE_TYPE,
                planId,
                summary,
                detailsJson,
                null,
                null));
    }
}
