package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.CurrentUser;
import com.openlab.qualitos.quality.derogation.application.DerogationAuditPort;
import com.openlab.qualitos.quality.derogation.domain.Derogation;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

/**
 * Route les gestes de dérogation vers le journal chaîné.
 *
 * <p>Format de fil : {@code derogation.<action>}.
 *
 * <p>L'acteur est celui du jeton, et non le créateur du dossier : c'est le
 * manager qui signe, et écrire l'initiateur à sa place ferait mentir la piste
 * d'audit sur le seul point qui l'intéresse.
 */
@Component
public class AuditLogDerogationAuditAdapter implements DerogationAuditPort {

    static final String RESOURCE_TYPE = "derogation";

    private final AuditEventService auditEvents;

    public AuditLogDerogationAuditAdapter(AuditEventService auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Override
    public void record(Derogation d, Action action) {
        String fil = "derogation." + action.name().toLowerCase(Locale.ROOT);
        UUID acteur = CurrentUser.userId().orElse(d.getCreatedByUserId());
        auditEvents.recordForTenant(d.getTenantId(),
                new AuditEventDto.RecordEventRequest(
                        null, ActorType.USER, acteur,
                        fil, RESOURCE_TYPE, d.getId(),
                        resume(d, action), charge(d), null, null));
    }

    private static String resume(Derogation d, Action action) {
        return action.name() + " — " + d.getReference() + " (" + d.getStatus() + ")";
    }

    /**
     * Ce que la charge retient.
     *
     * <p><b>Le motif du renvoi n'y entre pas</b> : il peut citer un client, une
     * pièce ou une mesure. Sa <i>présence</i>, elle, se note — c'est ce qui
     * permet de vérifier qu'un renvoi a bien été motivé sans recopier le motif
     * dans un journal qui se lit plus largement que le dossier.
     */
    private static String charge(Derogation d) {
        return "{"
                + "\"reference\":\"" + echapper(d.getReference()) + "\""
                + ",\"status\":\"" + d.getStatus() + "\""
                + ",\"decision\":\"" + d.getAnswer().decision() + "\""
                + ",\"manager\":" + (d.getApproval().managerAccountId() == null
                        ? "null" : "\"" + d.getApproval().managerAccountId() + "\"")
                + ",\"signed\":" + d.getApproval().estSignee()
                + ",\"sent\":" + d.getDispatch().estParti()
                + ",\"hasAmendReason\":" + (d.getApproval().amendReason() != null)
                + "}";
    }

    /** Une référence est en {@code [A-Z0-9-]}, mais on ne parie pas sur la donnée. */
    private static String echapper(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
