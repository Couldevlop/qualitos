package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionStatus;
import com.openlab.qualitos.quality.capa.CapaActionType;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.effectiveness.application.EvaluationCasePort;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Les dossiers CAPA concernés par l'évaluation d'efficacité, avec leurs
 * décomptes (ADR 0108).
 *
 * <p><b>Trois requêtes pour tout le tableau, quel qu'en soit le nombre de
 * lignes</b> : les dossiers avec leurs actions, les écarts ouverts groupés par
 * dossier, et — côté appelant — la dernière évaluation de chaque dossier. Toute
 * information manquante ici se paierait d'une requête par ligne, et c'est la
 * façon la plus courante de transformer l'affichage d'un écran en centaines
 * d'allers-retours.
 */
@Component
public class EvaluationCaseAdapter implements EvaluationCasePort {

    /**
     * Les statuts hors périmètre.
     *
     * <p>Un dossier REJETÉ n'a jamais été instruit : il n'y a rien dont on
     * puisse juger l'efficacité, et l'afficher gonflerait le décompte des
     * sujets ouverts d'autant de dossiers qu'on a eu raison d'écarter.
     */
    private static final Set<CapaStatus> OUT_OF_SCOPE = Set.of(CapaStatus.REJECTED);

    /**
     * Statuts où une non-conformité est refermée. Identique à celui du service
     * de CAPA, et pour la même raison : CANCELLED en fait partie, un constat
     * annulé n'ayant pas lieu d'empêcher une clôture.
     */
    private static final Set<NcStatus> NC_SETTLED =
            Set.of(NcStatus.CLOSED, NcStatus.CANCELLED, NcStatus.REJECTED);

    private final CapaCaseRepository capaCases;
    private final NonConformityRepository nonConformities;

    EvaluationCaseAdapter(CapaCaseRepository capaCases, NonConformityRepository nonConformities) {
        this.capaCases = capaCases;
        this.nonConformities = nonConformities;
    }

    @Override
    public List<EvaluationCase> findInScope(UUID tenantId) {
        Map<UUID, Integer> openNc = openNonConformitiesPerCase(tenantId);
        return capaCases.findForEffectivenessBoard(tenantId, OUT_OF_SCOPE).stream()
                .map(c -> toPort(c, openNc.getOrDefault(c.getId(), 0)))
                .toList();
    }

    private Map<UUID, Integer> openNonConformitiesPerCase(UUID tenantId) {
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : nonConformities.countOpenPerCapaCase(tenantId, NC_SETTLED)) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    private static EvaluationCase toPort(CapaCase c, int openNcCount) {
        List<CapaAction> actions = c.getActions();
        int total = actions.size();
        int done = (int) actions.stream()
                .filter(a -> a.getStatus() == CapaActionStatus.DONE).count();
        return new EvaluationCase(
                c.getId(), c.getSourceRef(), c.getTitle(),
                c.getCriticity() == null ? null : c.getCriticity().name(),
                c.getOwnerName(), c.getEffectivenessCriterion(),
                c.getStatus() == CapaStatus.CLOSED,
                c.getStatus() == CapaStatus.RESOLVED,
                total, done,
                containmentOnly(actions), openNcCount,
                atEndOfDay(c.getDueDate()), c.getUpdatedAt());
    }

    /**
     * Le dossier ne porte-t-il QUE des mesures d'endiguement ?
     *
     * <p>Elles arrêtent l'effet sans supprimer la cause (ISO 9001 §10.2) : clore
     * là-dessus reviendrait à déclarer réglé un problème qui reviendra dès la
     * mesure levée. Un dossier sans aucune action n'est pas « endiguement
     * seul » — c'est un autre obstacle, que le décompte des actions dit déjà.
     */
    private static boolean containmentOnly(List<CapaAction> actions) {
        return !actions.isEmpty() && actions.stream()
                .allMatch(a -> a.getActionType() == CapaActionType.CONTAINMENT);
    }

    /**
     * L'échéance, ramenée à la fin de la journée en temps universel.
     *
     * <p>Une date d'échéance est tenue jusqu'au soir : la convertir à minuit
     * ferait apparaître en retard, dès son premier instant, un dossier dû le
     * jour même.
     */
    private static Instant atEndOfDay(LocalDate due) {
        return due == null ? null : due.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    }
}
