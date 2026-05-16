package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.privacynotices.application.PrivacyNoticeEventPublisher;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Adapter — route les événements de mention vers le journal d'audit immuable.
 * Wire format : {@code gdpr.notice.<action>}.
 * Payload : métadonnées structurelles (référence, version, langue, status) —
 * pas de contenu pour limiter la taille (le contenu est dans l'agrégat).
 */
@Component
public class AuditLogPrivacyNoticeEventPublisher implements PrivacyNoticeEventPublisher {

    static final String RESOURCE_TYPE = "gdpr-privacy-notice";

    private final AuditEventService auditEvents;

    public AuditLogPrivacyNoticeEventPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void publish(PrivacyNotice n, Action action) {
        String wire = "gdpr.notice." + action.name().toLowerCase(Locale.ROOT);
        UUID actor = (action == Action.PUBLISHED && n.getPublishedByUserId() != null)
                ? n.getPublishedByUserId() : n.getCreatedByUserId();
        String payload = "{"
                + "\"reference\":\"" + n.getReference() + "\""
                + ",\"version\":\"" + n.getVersion() + "\""
                + ",\"language\":\"" + n.getLanguage() + "\""
                + ",\"status\":\"" + n.getStatus() + "\""
                + "}";
        String summary = action.name() + " — " + n.getReference()
                + "@" + n.getVersion() + " [" + n.getLanguage() + "]";
        auditEvents.recordForTenant(n.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, actor,
                        wire, RESOURCE_TYPE, n.getId(),
                        summary, payload, null, null));
    }
}
