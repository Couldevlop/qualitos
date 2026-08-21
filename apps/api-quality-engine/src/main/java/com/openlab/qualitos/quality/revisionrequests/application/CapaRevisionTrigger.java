package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.capa.CapaTransition;
import com.openlab.qualitos.quality.capa.CapaTransitionEvent;
import com.openlab.qualitos.quality.revisionrequests.domain.ProposedChange;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Une CAPA vient d'être close sur efficacité vérifiée : que devrait dire le PFMEA,
 * et que devrait montrer le control plan ?
 *
 * <p>Seule la clôture compte. {@code RESOLVED} signifie « on a fait quelque chose »,
 * pas « ça a marché » : proposer de baisser une cote à ce moment-là reviendrait à
 * inscrire dans le document un progrès que rien n'a encore démontré.
 */
public class CapaRevisionTrigger implements RevisionTrigger<CapaTransitionEvent> {

    /**
     * Ce qui, dans l'intitulé d'une action, dit qu'un contrôle a été ajouté.
     * Une liste de mots plutôt qu'un modèle : elle se lit, se discute et
     * s'allonge, et elle ne tombe jamais en panne au moment de clore un dossier.
     */
    private static final List<String> CONTROL_WORDS = List.of(
            "controle", "contrôle", "inspection", "verification", "vérification",
            "poka", "detrompeur", "détrompeur", "gabarit", "capteur", "camera",
            "caméra", "calibre", "test", "essai", "mesure");

    private final NcLookupPort ncLookup;
    private final CapaActionsPort capaActions;
    private final PfmeaPort pfmea;
    private final Clock clock;

    public CapaRevisionTrigger(NcLookupPort ncLookup, CapaActionsPort capaActions,
                               PfmeaPort pfmea, Clock clock) {
        this.ncLookup = ncLookup;
        this.capaActions = capaActions;
        this.pfmea = pfmea;
        this.clock = clock;
    }

    @Override
    public List<RevisionRequest> propose(CapaTransitionEvent event) {
        if (event.transition() != CapaTransition.CLOSED) return List.of();

        Map<String, Object> payload = event.payload();
        if (!"NON_CONFORMITY".equals(text(payload.get("sourceType")))) return List.of();

        String reference = text(payload.get("sourceRef"));
        if (reference == null) return List.of();

        Optional<NcLookupPort.NcRef> origin = ncLookup.findByReference(event.tenantId(), reference);
        if (origin.isEmpty() || origin.get().productId() == null) return List.of();
        NcLookupPort.NcRef nc = origin.get();

        UUID capaId = uuid(payload.get("id"));
        if (capaId == null) return List.of();

        List<CapaActionsPort.CapaActionSummary> actions =
                capaActions.actionsOf(event.tenantId(), capaId);
        Instant now = Instant.now(clock);
        String label = label(payload, reference);

        List<RevisionRequest> proposals = new ArrayList<>();
        for (CapaActionsPort.CapaActionSummary action : actions) {
            // Une mesure d'endiguement est temporaire par définition. La graver
            // dans un control plan serait un contresens : on la lève quand la
            // cause est traitée.
            if (action.containment()) continue;

            ratingProposal(event.tenantId(), capaId, nc, action, label, now)
                    .ifPresent(proposals::add);
            proposals.add(controlPlanLineProposal(event.tenantId(), capaId, nc, action, label, now));
        }
        return proposals;
    }

    /**
     * Au plus UNE proposition par ligne d'analyse. La base n'admet qu'une demande
     * en attente par cible — un ingénieur ne tranche pas deux fois la même ligne
     * dans deux encarts distincts, et l'index partiel le lui rappellerait par une
     * erreur illisible.
     *
     * <p>Quand l'action ajoute un contrôle, c'est la détection qui parle : c'est le
     * signal le plus précis des deux, et le plus souvent mal lu.
     */
    private Optional<RevisionRequest> ratingProposal(UUID tenantId, UUID capaId,
                                                     NcLookupPort.NcRef nc,
                                                     CapaActionsPort.CapaActionSummary action,
                                                     String label, Instant now) {
        if (nc.fmeaItemId() == null) return Optional.empty();
        Optional<PfmeaPort.PfmeaItemSnapshot> found = pfmea.item(tenantId, nc.fmeaItemId());
        if (found.isEmpty()) return Optional.empty();
        PfmeaPort.PfmeaItemSnapshot item = found.get();

        // Ajouter un contrôle améliore la détection — et améliorer la détection
        // veut dire BAISSER la note. C'est l'erreur classique de lecture du FMEA.
        if (addsAControl(action) && item.detection() > 1) {
            return Optional.of(RevisionRequest.propose(tenantId, nc.productId(),
                    RevisionTargetType.PFMEA_ITEM, item.id(),
                    RevisionTriggerType.CAPA_CLOSED, capaId, label,
                    "Action « " + shorten(action.title()) + " » ajoute un contrôle :"
                            + " la détection s'améliore, donc la note baisse "
                            + item.detection() + " → " + (item.detection() - 1),
                    ProposedChange.rating("detection", item.detection(), item.detection() - 1),
                    now));
        }
        // Supprimer la cause fait baisser l'occurrence.
        if (item.occurrence() > 1) {
            return Optional.of(RevisionRequest.propose(tenantId, nc.productId(),
                    RevisionTargetType.PFMEA_ITEM, item.id(),
                    RevisionTriggerType.CAPA_CLOSED, capaId, label,
                    "Action « " + shorten(action.title()) + " » close sur efficacité vérifiée :"
                            + " la cause traitée fait baisser l'occurrence "
                            + item.occurrence() + " → " + (item.occurrence() - 1),
                    ProposedChange.rating("occurrence", item.occurrence(), item.occurrence() - 1),
                    now));
        }
        return Optional.empty();
    }

    private RevisionRequest controlPlanLineProposal(UUID tenantId, UUID capaId,
                                                    NcLookupPort.NcRef nc,
                                                    CapaActionsPort.CapaActionSummary action,
                                                    String label, Instant now) {
        return RevisionRequest.propose(tenantId, nc.productId(),
                RevisionTargetType.CONTROL_PLAN_LINE_CREATE, null,
                RevisionTriggerType.CAPA_CLOSED, capaId, label,
                "L'action « " + shorten(action.title()) + " » a produit son effet :"
                        + " elle mérite une ligne au control plan pour ne pas se perdre",
                ProposedChange.creation(draftOf(action, nc)),
                now);
    }

    private static boolean addsAControl(CapaActionsPort.CapaActionSummary action) {
        String haystack = fold((action.title() == null ? "" : action.title())
                + " " + (action.description() == null ? "" : action.description()));
        return CONTROL_WORDS.stream().anyMatch(word -> haystack.contains(fold(word)));
    }

    private static String fold(String value) {
        return java.text.Normalizer.normalize(value.toLowerCase(Locale.ROOT),
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    /** JSON plat, construit à la main : trois champs, aucun sérialiseur à convoquer. */
    private static String draftOf(CapaActionsPort.CapaActionSummary action, NcLookupPort.NcRef nc) {
        return "{\"characteristicLabel\":\"" + escape(shorten(action.title()))
                + "\",\"controlMethod\":\"" + escape(action.description())
                + "\",\"fmeaItemId\":" + (nc.fmeaItemId() == null
                        ? "null" : "\"" + nc.fmeaItemId() + "\"") + "}";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    /** Le libellé du déclencheur tient en 120 caractères : c'est une étiquette, pas un récit. */
    private static String label(Map<String, Object> payload, String reference) {
        String title = text(payload.get("title"));
        String value = title == null ? reference : reference + " — " + title;
        return value.length() > 120 ? value.substring(0, 120) : value;
    }

    private static String shorten(String value) {
        if (value == null || value.isBlank()) return "action";
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static UUID uuid(Object value) {
        try {
            return value == null ? null : UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
