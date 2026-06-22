package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.standards.normdoc.dossier.application.DossierEventPublisher;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Adapter — route les transitions d'un dossier documentaire vers le journal
 * d'audit chaîné (OWASP A09). Wire format : {@code standards.dossier.<action>}.
 * Aucune PII : on ne journalise que des métadonnées (code norme, progression).
 */
@Component
public class AuditLogDossierEventPublisher implements DossierEventPublisher {

    static final String RESOURCE_TYPE = "standard-doc-dossier";

    private final AuditEventService auditEvents;

    public AuditLogDossierEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(DocumentationDossier dossier, Action action) {
        String wire = "standards.dossier." + action.name().toLowerCase(Locale.ROOT);
        String payload = "{"
                + "\"standardCode\":\"" + dossier.getStandardCode() + "\""
                + ",\"status\":\"" + dossier.getStatus() + "\""
                + ",\"documentCount\":" + dossier.totalCount()
                + ",\"generatedCount\":" + dossier.generatedCount()
                + ",\"progressPercent\":" + dossier.progressPercent()
                + "}";
        String summary = action.name() + " — dossier " + dossier.getStandardCode()
                + " (" + dossier.generatedCount() + "/" + dossier.totalCount() + ")";
        auditEvents.recordForTenant(dossier.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, actorFor(dossier, action),
                        wire, RESOURCE_TYPE, dossier.getId(),
                        summary, payload, null, null));
    }

    private static UUID actorFor(DocumentationDossier dossier, Action action) {
        return action == Action.FINALIZED && dossier.getFinalizedByUserId() != null
                ? dossier.getFinalizedByUserId()
                : dossier.getCreatedByUserId();
    }
}
