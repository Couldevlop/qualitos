package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.nonconformity.NcCreatedEvent;
import com.openlab.qualitos.quality.revisionrequests.domain.OccurrenceProposalCalculator;
import com.openlab.qualitos.quality.revisionrequests.domain.ProposedChange;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Une non-conformité vient d'être constatée : que devrait dire le PFMEA ?
 *
 * <p>Deux cas, et deux seulement. Le défaut illustre un mode de défaillance déjà
 * analysé, et l'historique contredit sa cote d'occurrence : on propose de la
 * relever. Ou bien aucun mode ne correspond, et on propose d'en créer un — c'est
 * l'écart le plus intéressant à montrer à un auditeur, celui d'un défaut que
 * l'analyse n'avait pas prévu.
 */
public class NcRevisionTrigger implements RevisionTrigger<NcCreatedEvent> {

    /** Fenêtre de comptage, en jours. Douze mois glissants, comme la revue annuelle. */
    private static final int WINDOW_DAYS = 365;

    private final NcHistoryPort history;
    private final PfmeaPort pfmea;
    private final Clock clock;

    public NcRevisionTrigger(NcHistoryPort history, PfmeaPort pfmea, Clock clock) {
        this.history = history;
        this.pfmea = pfmea;
        this.clock = clock;
    }

    @Override
    public List<RevisionRequest> propose(NcCreatedEvent event) {
        // L'événement est publié pour TOUTE NC : le filtre est ici, chez le
        // consommateur, pas chez l'émetteur.
        if (event.productId() == null) return List.of();
        Instant now = Instant.now(clock);

        return event.fmeaItemId() == null
                ? proposeCreation(event, now)
                : proposeRaise(event, now);
    }

    private List<RevisionRequest> proposeRaise(NcCreatedEvent event, Instant now) {
        Optional<PfmeaPort.PfmeaItemSnapshot> found = pfmea.item(event.tenantId(), event.fmeaItemId());
        if (found.isEmpty()) return List.of();
        PfmeaPort.PfmeaItemSnapshot item = found.get();

        int count = history.countForProductAndFailureMode(event.tenantId(), event.productId(),
                item.id(), now.minus(WINDOW_DAYS, ChronoUnit.DAYS));
        OptionalInt proposed = OccurrenceProposalCalculator.proposal(item.occurrence(), count);
        if (proposed.isEmpty()) return List.of();

        String rationale = count + " NC en 12 mois sur ce mode de défaillance"
                + " (comptage de non-conformités, faute de volume produit connu)"
                + " — occurrence " + item.occurrence() + " → " + proposed.getAsInt();

        return List.of(RevisionRequest.propose(event.tenantId(), event.productId(),
                RevisionTargetType.PFMEA_ITEM, item.id(),
                RevisionTriggerType.NC_CREATED, event.ncId(), label(event),
                rationale,
                ProposedChange.rating("occurrence", item.occurrence(), proposed.getAsInt()),
                now));
    }

    private List<RevisionRequest> proposeCreation(NcCreatedEvent event, Instant now) {
        Optional<UUID> project = pfmea.activeProjectOf(event.tenantId(), event.productId());
        if (project.isEmpty()) return List.of();

        return List.of(RevisionRequest.propose(event.tenantId(), event.productId(),
                RevisionTargetType.PFMEA_ITEM_CREATE, null,
                RevisionTriggerType.NC_CREATED, event.ncId(), label(event),
                "Aucun mode de défaillance ne correspond à cette non-conformité :"
                        + " le défaut est survenu sans avoir été analysé",
                ProposedChange.creation(draftOf(event)),
                now));
    }

    /** JSON plat, construit à la main : deux champs, aucun sérialiseur à convoquer. */
    private static String draftOf(NcCreatedEvent event) {
        return "{\"failureMode\":\"" + escape(event.title())
                + "\",\"failureEffect\":\"" + escape(event.description()) + "\"}";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    private static String label(NcCreatedEvent event) {
        String title = event.title() == null || event.title().isBlank()
                ? "NC " + event.ncId()
                : event.title();
        return title.length() > 120 ? title.substring(0, 120) : title;
    }
}
