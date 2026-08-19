package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionAuditPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Inscrit la décision au journal chaîné du tenant.
 *
 * <p>L'acteur est celui que le service a lu du jeton, jamais un champ de requête :
 * un fait d'audit dont l'auteur serait falsifiable ne vaudrait rien devant un
 * auditeur (correctif H2 de l'audit du 6 juin).
 */
@Component
public class RevisionAuditAdapter implements RevisionAuditPort {

    private static final String RESOURCE_TYPE = "quality_revision_request";

    private final AuditEventService auditEvents;

    public RevisionAuditAdapter(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void record(UUID tenantId, UUID actorId, String action, UUID requestId,
                       String summary, String detailsJson) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actorId == null ? ActorType.SYSTEM : ActorType.USER,
                actorId,
                action,
                RESOURCE_TYPE,
                requestId,
                summary,
                detailsJson,
                null,
                null));
    }
}
