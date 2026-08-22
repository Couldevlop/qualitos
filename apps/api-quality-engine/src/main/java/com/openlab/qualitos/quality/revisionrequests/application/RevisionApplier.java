package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.revisionrequests.domain.ProposedChange;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStateException;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;

import java.util.Optional;
import java.util.UUID;

/**
 * Applique une proposition acceptée aux documents — et jamais à celui qui est en
 * vigueur.
 *
 * <p>Le document affiché au poste ne bouge pas sous les pieds de la production :
 * la révision suivante naît en brouillon, et son approbation reste une décision
 * distincte, réservée à un rôle plus étroit. Si un brouillon est déjà ouvert, la
 * modification s'y ajoute : ouvrir deux brouillons concurrents ferait rejeter
 * l'écriture par l'index partiel d'unicité, avec une erreur illisible à la clé.
 */
public class RevisionApplier {

    private final PfmeaPort pfmea;
    private final ControlPlanDraftPort controlPlans;

    public RevisionApplier(PfmeaPort pfmea, ControlPlanDraftPort controlPlans) {
        this.pfmea = pfmea;
        this.controlPlans = controlPlans;
    }

    public void apply(RevisionRequest request) {
        if (request.getTargetType().isPfmea()) {
            applyToPfmea(request);
        } else {
            applyToControlPlan(request);
        }
    }

    private void applyToPfmea(RevisionRequest request) {
        UUID tenantId = request.getTenantId();
        UUID projectId = projectOf(request)
                .orElseThrow(() -> new RevisionRequestStateException(
                        "Ce produit n'a pas de PFMEA sur lequel appliquer la proposition"));

        if (pfmea.isProjectActive(tenantId, projectId)) {
            pfmea.openRevision(tenantId, projectId);
        }

        ProposedChange change = request.getChange();
        if (request.getTargetType() == RevisionTargetType.PFMEA_ITEM) {
            pfmea.updateRating(tenantId, request.getTargetId(), change.field(),
                    Integer.parseInt(change.to()));
        } else {
            Draft draft = Draft.parse(change.draftJson());
            pfmea.addItem(tenantId, projectId, draft.failureMode(), draft.failureEffect());
        }
    }

    private Optional<UUID> projectOf(RevisionRequest request) {
        if (request.getTargetType() == RevisionTargetType.PFMEA_ITEM) {
            return pfmea.item(request.getTenantId(), request.getTargetId())
                    .map(PfmeaPort.PfmeaItemSnapshot::projectId);
        }
        return pfmea.activeProjectOf(request.getTenantId(), request.getProductId());
    }

    private void applyToControlPlan(RevisionRequest request) {
        UUID planId = controlPlans.draftPlanFor(request.getTenantId(), request.getProductId())
                .orElseThrow(() -> new RevisionRequestStateException(
                        "Ce produit n'a pas de control plan sur lequel appliquer la proposition"));

        Draft draft = Draft.parse(request.getChange().draftJson());
        controlPlans.addLine(request.getTenantId(), planId, draft.failureMode(),
                draft.failureEffect(), draft.fmeaItemId());
    }

    /**
     * Lecture du brouillon JSON déposé par le déclencheur.
     *
     * <p>Trois champs plats, écrits par nous, relus par nous : un analyseur à la
     * main suffit et évite d'introduire une dépendance de sérialisation dans une
     * couche qui doit rester nue. Un champ absent vaut vide, jamais une panne —
     * refuser d'appliquer une proposition parce qu'un libellé manque serait
     * disproportionné.
     */
    record Draft(String failureMode, String failureEffect, UUID fmeaItemId) {

        static Draft parse(String json) {
            if (json == null || json.isBlank()) return new Draft("", "", null);
            return new Draft(
                    value(json, "failureMode").isEmpty()
                            ? value(json, "characteristicLabel") : value(json, "failureMode"),
                    value(json, "failureEffect").isEmpty()
                            ? value(json, "controlMethod") : value(json, "failureEffect"),
                    uuid(value(json, "fmeaItemId")));
        }

        private static String value(String json, String field) {
            String needle = "\"" + field + "\":\"";
            int start = json.indexOf(needle);
            if (start < 0) return "";
            start += needle.length();
            StringBuilder out = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    out.append(json.charAt(++i));
                } else if (c == '"') {
                    break;
                } else {
                    out.append(c);
                }
            }
            return out.toString();
        }

        private static UUID uuid(String raw) {
            try {
                return raw == null || raw.isBlank() ? null : UUID.fromString(raw);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }
}
