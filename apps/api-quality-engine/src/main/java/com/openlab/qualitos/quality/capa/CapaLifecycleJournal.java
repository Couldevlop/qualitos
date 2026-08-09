package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Consigne les moments de la vie d'un dossier CAPA (§11.5, §13.2).
 *
 * <p>Un seul collaborateur pour le service métier, et deux destinations : le
 * journal chaîné du tenant — qui ne laisse rien passer — et les systèmes abonnés,
 * qui ne reçoivent que les faits qui les concernent. Le service de CAPA n'a ainsi
 * pas à connaître le journal d'audit ni les webhooks : il déclare ce qui vient
 * d'arriver au dossier, et c'est tout.
 *
 * <p><b>Deux temporalités, et c'est délibéré.</b> L'inscription au journal se
 * fait DANS la transaction : la chaîne d'empreintes doit être annulée avec le
 * changement qu'elle décrit, sinon elle attesterait d'une transition qui n'a
 * jamais eu lieu. L'annonce aux abonnés, elle, part APRÈS la validation
 * (`AFTER_COMMIT`, cf. {@link CapaWebhookRelay}) : une requête HTTP ne se rattrape
 * pas, et prévenir un système tiers d'une clôture que la base finit par annuler
 * serait irréparable. Cela évite au passage de tenir une transaction ouverte
 * pendant un appel réseau.
 *
 * <p>L'acteur vient de l'identité authentifiée ({@link CurrentUser}), jamais du
 * corps de la requête (§18.2 #2). Quand aucune identité n'est exploitable — une
 * transition déclenchée par un traitement automatique — l'événement est attribué
 * au système plutôt qu'à un utilisateur inventé, qu'un audit prendrait pour
 * argent comptant.
 */
@Component
public class CapaLifecycleJournal {

    static final String RESOURCE_TYPE = "capa_case";

    private final AuditEventService auditEvents;
    private final ApplicationEventPublisher events;

    public CapaLifecycleJournal(AuditEventService auditEvents, ApplicationEventPublisher events) {
        this.auditEvents = auditEvents;
        this.events = events;
    }

    /**
     * @param capa le dossier DANS SON ÉTAT D'ARRIVÉE — c'est celui-là qu'un
     *             relecteur veut voir, pas celui d'avant.
     */
    public void record(CapaCase capa, CapaTransition transition) {
        UUID tenantId = capa.getTenantId();
        UUID actor = CurrentUser.userId().orElse(null);
        Map<String, Object> payload = payload(capa);

        auditEvents.recordForTenant(tenantId, new AuditEventDto.RecordEventRequest(
                null,
                actor == null ? ActorType.SYSTEM : ActorType.USER,
                actor,
                transition.auditAction(),
                RESOURCE_TYPE,
                capa.getId(),
                transition.summary(),
                json(payload),
                null,
                null));

        if (transition.eventType() != null) {
            events.publishEvent(new CapaTransitionEvent(tenantId, transition, payload));
        }
    }

    /**
     * Ce qu'un abonné et un relecteur ont besoin de savoir : de quel dossier il
     * s'agit, où il en est, et d'où il vient. La description n'y figure pas —
     * elle est libre, souvent longue, et raconte un incident : elle n'a pas à
     * partir chez un tiers ni à grossir chaque ligne de journal.
     */
    private static Map<String, Object> payload(CapaCase c) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", str(c.getId()));
        data.put("title", c.getTitle());
        data.put("status", c.getStatus() == null ? null : c.getStatus().name());
        data.put("type", c.getType() == null ? null : c.getType().name());
        data.put("criticity", c.getCriticity() == null ? null : c.getCriticity().name());
        data.put("sourceType", c.getSourceType() == null ? null : c.getSourceType().name());
        data.put("sourceRef", c.getSourceRef());
        data.put("ownerId", str(c.getOwnerId()));
        data.put("dueDate", c.getDueDate() == null ? null : c.getDueDate().toString());
        data.put("effectivenessVerified", c.getEffectivenessVerified());
        return data;
    }

    private static String str(UUID value) {
        return value == null ? null : value.toString();
    }

    /**
     * JSON construit à la main : dix champs plats, aucun sérialiseur à convoquer.
     * Les guillemets et antislashs sont échappés — un titre de dossier est saisi
     * librement, et un seul guillemet mal placé casserait la ligne du journal.
     */
    static String json(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(e.getKey()).append("\":").append(value(e.getValue()));
        }
        return sb.append('}').toString();
    }

    private static String value(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Boolean || v instanceof Number) {
            return v.toString();
        }
        return "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
