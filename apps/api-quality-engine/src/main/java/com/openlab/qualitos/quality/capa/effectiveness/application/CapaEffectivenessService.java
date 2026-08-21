package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.capa.effectiveness.domain.CapaEffectiveness;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessCalculator;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessWindow;
import com.openlab.qualitos.quality.capa.effectiveness.domain.RecurrenceSignature;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage — l'efficacité mesurée des CAPA closes.
 *
 * <p>Aucune écriture : c'est une lecture, recalculée à chaque appel. Stocker le
 * taux le figerait au jour du calcul, alors qu'il change par nature — une
 * récidive survenue demain doit corriger le verdict d'aujourd'hui.
 *
 * <p>Le tri place en tête ce qui appelle une décision : les dossiers aggravés,
 * puis les taux les plus faibles. Un tableau trié par date met en haut ce qui
 * vient d'arriver, c'est-à-dire ce dont on ne peut encore rien dire.
 */
public class CapaEffectivenessService {

    private final ClosedCapaPort closedCapas;
    private final NcOccurrencePort occurrences;
    private final TenantProvider tenants;
    private final Clock clock;

    public CapaEffectivenessService(ClosedCapaPort closedCapas, NcOccurrencePort occurrences,
                                    TenantProvider tenants, Clock clock) {
        this.closedCapas = closedCapas;
        this.occurrences = occurrences;
        this.tenants = tenants;
        this.clock = clock;
    }

    public CapaEffectivenessDto.Summary measure(int windowMonths) {
        EffectivenessWindow window = EffectivenessWindow.ofMonths(windowMonths);
        UUID tenantId = tenants.requireTenantId();
        Instant now = Instant.now(clock);

        List<CapaEffectivenessDto.Row> rows = new ArrayList<>();
        for (ClosedCapaPort.ClosedCapa capa : closedCapas.findClosed(tenantId)) {
            // Sans signature, il n'y a rien à recouper : une CAPA née d'un audit
            // ou d'une décision interne n'a pas de « même problème » à guetter.
            // L'écarter est plus honnête que de lui inventer des récidives.
            if (capa.closedAt() == null || !capa.signature().isMeasurable()) {
                continue;
            }
            rows.add(CapaEffectivenessDto.Row.of(capa, evaluate(tenantId, capa, window, now),
                    capa.signature().isPrecise()));
        }

        rows.sort(Comparator
                .comparing(CapaEffectivenessDto.Row::aggravated).reversed()
                .thenComparing(row -> row.ratePercent() == null ? Integer.MAX_VALUE : row.ratePercent())
                .thenComparing(CapaEffectivenessDto.Row::closedAt, Comparator.reverseOrder()));

        return summarize(window, rows);
    }

    private CapaEffectiveness evaluate(UUID tenantId, ClosedCapaPort.ClosedCapa capa,
                                       EffectivenessWindow window, Instant now) {
        RecurrenceSignature signature = capa.signature();
        // La fenêtre « avant » se termine à l'OUVERTURE du dossier, pas à sa
        // clôture : les non-conformités survenues pendant le traitement sont ce
        // qui a motivé l'action, pas ce qu'elle a échoué à empêcher.
        Instant beforeStart = capa.openedAt().minus(window.days(), ChronoUnit.DAYS);
        int before = occurrences.countBetween(tenantId, signature, beforeStart, capa.openedAt());

        Instant afterEnd = capa.closedAt().plus(window.days(), ChronoUnit.DAYS);
        int after = occurrences.countBetween(tenantId, signature, capa.closedAt(),
                afterEnd.isBefore(now) ? afterEnd : now);

        return EffectivenessCalculator.evaluate(capa.closedAt(), now, window, before, after);
    }

    private CapaEffectivenessDto.Summary summarize(EffectivenessWindow window,
                                                   List<CapaEffectivenessDto.Row> rows) {
        int measured = 0;
        int inObservation = 0;
        int notMeasurable = 0;
        int aggravated = 0;
        int declaredButFailed = 0;
        int sum = 0;

        for (CapaEffectivenessDto.Row row : rows) {
            if (row.aggravated()) aggravated++;
            switch (row.status()) {
                case MEASURED -> {
                    measured++;
                    sum += row.ratePercent();
                    // « Efficacité vérifiée » cochée à la clôture, et pourtant le
                    // problème est revenu autant ou plus qu'avant.
                    if (Boolean.TRUE.equals(row.declaredEffective()) && row.ratePercent() == 0) {
                        declaredButFailed++;
                    }
                }
                case IN_OBSERVATION -> inObservation++;
                case NOT_MEASURABLE -> notMeasurable++;
            }
        }

        Integer average = measured == 0 ? null : Math.round((float) sum / measured);
        return new CapaEffectivenessDto.Summary(window.months(), measured, inObservation,
                notMeasurable, average, aggravated, declaredButFailed, rows);
    }
}
