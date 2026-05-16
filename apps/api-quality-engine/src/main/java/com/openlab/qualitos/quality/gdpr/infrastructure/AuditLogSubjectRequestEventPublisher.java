package com.openlab.qualitos.quality.gdpr.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.gdpr.application.SubjectRequestEventPublisher;
import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Adapter qui route les événements RGPD vers le journal d'audit immuable.
 * Wire format : {@code gdpr.subject.<action>} (RGPD Art. 30 — register of processing).
 *
 * Note privacy : on n'inclut PAS l'identifier label (potentiellement PII) dans le payload
 * conservé par l'audit log — seulement le hash et le type.
 */
@Component
public class AuditLogSubjectRequestEventPublisher implements SubjectRequestEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-subject-request";

    private final AuditEventService auditEvents;

    public AuditLogSubjectRequestEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(DataSubjectRequest r, Action action) {
        String wire = "gdpr.subject." + action.name().toLowerCase(Locale.ROOT);
        String summary = r.getType().name() + " request " + r.getStatus().name();
        // Hash en payload pour traçabilité (jamais la PII en clair, OWASP A02 / RGPD Art. 32).
        String payload = "{\"subjectHash\":\"" + r.getSubjectIdentifierHash()
                + "\",\"type\":\"" + r.getType() + "\""
                + ",\"deadlineAt\":\"" + r.getDeadlineAt() + "\""
                + ",\"extended\":" + r.isExtended() + "}";
        auditEvents.recordForTenant(r.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER,
                        r.getHandledByUserId() != null ? r.getHandledByUserId() : r.getRequestedByUserId(),
                        wire, RESOURCE_TYPE, r.getId(),
                        summary, payload, null, null));
    }
}
