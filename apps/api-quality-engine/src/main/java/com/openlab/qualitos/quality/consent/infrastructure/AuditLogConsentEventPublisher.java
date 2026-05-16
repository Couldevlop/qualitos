package com.openlab.qualitos.quality.consent.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.consent.application.ConsentEventPublisher;
import com.openlab.qualitos.quality.consent.domain.Consent;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Adapter — route les événements consentement vers le journal d'audit immuable.
 * Wire format : {@code gdpr.consent.<action>}.
 *
 * Privacy : on n'écrit JAMAIS l'identifiant en clair dans le payload audit,
 * uniquement le hash + purposeCode + status (OWASP A02).
 */
@Component
public class AuditLogConsentEventPublisher implements ConsentEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-consent";

    private final AuditEventService auditEvents;

    public AuditLogConsentEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(Consent c, Action action) {
        String wire = "gdpr.consent." + action.name().toLowerCase(Locale.ROOT);
        UUID actor = switch (action) {
            case GRANTED -> c.getGrantedByUserId();
            case WITHDRAWN -> c.getWithdrawnByUserId() != null
                    ? c.getWithdrawnByUserId() : c.getGrantedByUserId();
            case EXPIRED -> null;
        };
        ActorType actorType = (action == Action.EXPIRED || actor == null)
                ? ActorType.SYSTEM : ActorType.USER;
        String payload = "{"
                + "\"subjectHash\":\"" + c.getSubjectIdentifierHash() + "\""
                + ",\"purpose\":\"" + c.getPurposeCode() + "\""
                + ",\"purposeVersion\":\"" + c.getPurposeVersion() + "\""
                + ",\"source\":\"" + c.getSource() + "\""
                + ",\"status\":\"" + c.getStatus() + "\""
                + "}";
        String summary = action.name() + " — purpose=" + c.getPurposeCode()
                + " v" + c.getPurposeVersion();
        auditEvents.recordForTenant(c.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, actorType, actor,
                        wire, RESOURCE_TYPE, c.getId(),
                        summary, payload, null, null));
    }
}
