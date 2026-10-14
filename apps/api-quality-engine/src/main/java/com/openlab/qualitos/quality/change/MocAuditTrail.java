package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Inscrit les actions de la gestion des changements au journal d'audit chaîné
 * (§11.5) : {@code moc.<action>} sur la ressource {@code change-request}, ou
 * {@code moc.settings.updated} sur {@code moc-settings}.
 *
 * <p>L'historique affiché dans l'écran raconte la demande ; le journal, lui, est
 * la preuve infalsifiable — il survit à la suppression d'un brouillon. Aucune
 * donnée personnelle dans le résumé : le code et l'action, l'acteur par son
 * identifiant seulement.
 */
@Component
public class MocAuditTrail {

    static final String RESOURCE_CHANGE = "change-request";
    static final String RESOURCE_SETTINGS = "moc-settings";

    private final AuditEventService auditEvents;

    public MocAuditTrail(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    /** Une action sur une demande, par l'utilisateur du jeton (ou le système, acteur nul). */
    public void change(UUID tenantId, UUID actor, String action, ChangeRequest c, String detail) {
        String summary = c.getCode() + " — " + action + (detail == null ? "" : " — " + detail);
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null, actor == null ? ActorType.SYSTEM : ActorType.USER, actor,
                "moc." + action, RESOURCE_CHANGE, c.getId(),
                summary.length() > 500 ? summary.substring(0, 500) : summary,
                "{\"code\":\"" + c.getCode() + "\",\"status\":\"" + c.getStatus() + "\"}",
                null, null));
    }

    public void settings(UUID tenantId, UUID actor) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, "moc.settings.updated", RESOURCE_SETTINGS, tenantId,
                "Paramètres de la gestion des changements modifiés", null, null, null));
    }
}
