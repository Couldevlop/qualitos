package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionPorts;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionContent;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSettings;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionSheet;
import com.openlab.qualitos.quality.dmaic.resolution.domain.SheetPhoto;

import java.util.UUID;

/**
 * Route les écritures de la fiche de résolution vers le journal d'audit chaîné.
 *
 * <p>Aucune donnée personnelle ni aucun texte libre dans la charge utile : ni
 * noms, ni réponses, ni nom du client. Seulement des comptes et des codes, qui
 * suffisent à dire ce qui a changé sans recopier la réclamation.
 */
public class AuditLogResolutionPublisher implements ResolutionPorts.Audit {

    static final String SHEET = "dmaic-resolution";
    static final String SETTINGS = "dmaic-resolution-settings";
    static final String SAVED = "dmaic.resolution.saved";
    static final String SETTINGS_REPLACED = "dmaic.resolution.settings.replaced";
    static final String SETTINGS_RESET = "dmaic.resolution.settings.reset";
    static final String PHOTO_ADDED = "dmaic.resolution.photo.added";
    static final String PHOTO_REMOVED = "dmaic.resolution.photo.removed";

    private final AuditEventService auditEvents;

    public AuditLogResolutionPublisher(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void sheetSaved(ResolutionSheet sheet, UUID actor) {
        ResolutionContent c = sheet.getContent();
        String payload = "{\"answers\":" + c.answers().size()
                + ",\"team\":" + c.team().size()
                + ",\"whys\":" + c.whys().size()
                + ",\"actions\":" + c.actions().size()
                + ",\"severity\":" + c.severity()
                + ",\"failureType\":" + quoted(c.failureTypeCode())
                + "}";
        record(sheet.getTenantId(), actor, SAVED, SHEET, sheet.getProjectId(),
                "Fiche de résolution DMAIC enregistrée", payload);
    }

    @Override
    public void settingsReplaced(UUID tenantId, ResolutionSettings settings, UUID actor) {
        String payload = "{\"questions\":" + settings.questions().size()
                + ",\"failureTypes\":" + settings.failureTypes().size()
                + ",\"severities\":" + settings.severities().size() + "}";
        record(tenantId, actor, SETTINGS_REPLACED, SETTINGS, null,
                "Paramétrage de la fiche DMAIC modifié", payload);
    }

    @Override
    public void settingsReset(UUID tenantId, UUID actor) {
        record(tenantId, actor, SETTINGS_RESET, SETTINGS, null,
                "Paramétrage de la fiche DMAIC rendu à la plateforme", "{}");
    }

    @Override
    public void photoAdded(SheetPhoto photo, UUID actor) {
        record(photo.tenantId(), actor, PHOTO_ADDED, SHEET, photo.projectId(),
                "Photo ajoutée à la fiche DMAIC", photoPayload(photo));
    }

    @Override
    public void photoRemoved(SheetPhoto photo, UUID actor) {
        record(photo.tenantId(), actor, PHOTO_REMOVED, SHEET, photo.projectId(),
                "Photo retirée de la fiche DMAIC", photoPayload(photo));
    }

    private static String photoPayload(SheetPhoto p) {
        return "{\"slot\":" + p.slot() + ",\"contentType\":" + quoted(p.contentType())
                + ",\"sizeBytes\":" + p.sizeBytes() + "}";
    }

    /** Les codes sont validés (capitales, chiffres, ponctuation simple) : rien à échapper. */
    private static String quoted(String code) {
        return code == null ? "null" : "\"" + code + "\"";
    }

    private void record(UUID tenantId, UUID actor, String action, String resource, UUID resourceId,
                        String summary, String payload) {
        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null, ActorType.USER, actor, action, resource, resourceId, summary, payload, null, null));
    }
}
