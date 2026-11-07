package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.capa.CapaNotFoundException;
import com.openlab.qualitos.quality.capa.CapaScope;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EvaluationStage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cas d'usage — le plan de travail des évaluations d'efficacité (ADR 0108).
 *
 * <p>Pendant du service de mesure par récidive, et son complément : celui-là dit
 * ce que le terrain a fait, celui-ci dit ce que quelqu'un a jugé, et sur quoi.
 * L'écart entre les deux est l'information qu'un auditeur cherche.
 *
 * <p>Aucune écriture : l'enregistrement d'un verdict passe par le service du
 * dossier CAPA, qui seul détient la machine à états. Deux endroits capables de
 * changer le statut d'un dossier finiraient par en changer différemment.
 *
 * <p><b>Le tri place en tête ce qui appelle un geste</b> : les dossiers prêts à
 * être jugés, puis ceux qui attendent une action complémentaire, puis les
 * rouverts, puis le travail en cours, et les dossiers clos en dernier. Un
 * tableau trié par date met en haut ce qui vient d'arriver, c'est-à-dire ce dont
 * personne n'a rien à faire aujourd'hui.
 */
public class EffectivenessBoardService {

    /**
     * Nombre maximal de lignes AFFICHÉES.
     *
     * <p>Les indicateurs, eux, portent sur l'ensemble des dossiers : c'est la
     * raison d'être du drapeau {@code truncated}. Borner l'affichage protège
     * l'écran et la bande passante ; borner le calcul produirait une synthèse
     * fausse présentée comme vraie.
     */
    static final int MAX_ROWS = 200;

    /**
     * L'ordre d'urgence. Écrit une fois, ici, plutôt que dispersé dans un
     * comparateur : la question « qu'est-ce qui passe devant quoi ? » se lit.
     */
    private static final List<EvaluationStage> URGENCY = List.of(
            EvaluationStage.AWAITING_EVALUATION,
            EvaluationStage.COMPLEMENTARY_ACTION_EXPECTED,
            EvaluationStage.REOPENED,
            EvaluationStage.ACTIONS_IN_PROGRESS,
            EvaluationStage.CLOSED_EFFECTIVE);

    private final EvaluationCasePort cases;
    private final EffectivenessReviewPort reviews;
    private final TenantProvider tenants;

    /**
     * Ce que l'utilisateur courant voit du registre CAPA (ADR 0081).
     *
     * <p>Le plan de travail n'échappe pas à la règle du module. Sans
     * « voir tous les dossiers », on ne voit que les dossiers qu'on pilote, dont
     * on vérifie l'efficacité, ou dont une action nous est confiée — ici comme
     * sur le registre. Un écran de synthèse qui en montrerait plus que la liste
     * dont il est la synthèse serait le chemin détourné qui vide la règle.
     */
    private final RecordScope scope;

    public EffectivenessBoardService(EvaluationCasePort cases, EffectivenessReviewPort reviews,
                                     TenantProvider tenants, RecordScope scope) {
        this.cases = cases;
        this.reviews = reviews;
        this.tenants = tenants;
        this.scope = scope;
    }

    public EffectivenessBoardDto.Summary board() {
        UUID tenantId = tenants.requireTenantId();
        Optional<UUID> concerning = CapaScope.restriction(scope);
        List<EvaluationCasePort.EvaluationCase> inScope = cases.findInScope(tenantId, concerning);
        Map<UUID, EffectivenessReview> last = reviews.findLastPerCase(tenantId);
        Map<UUID, Integer> counts = reviews.countPerCase(tenantId);

        List<EffectivenessBoardDto.Row> rows = new ArrayList<>(inScope.size());
        for (EvaluationCasePort.EvaluationCase c : inScope) {
            EffectivenessReview review = last.get(c.id());
            EvaluationStage stage = EvaluationStage.of(c.closed(), c.resolved(),
                    review == null ? null : review.verdict(),
                    c.actionsTotal(), c.actionsDone());
            rows.add(row(c, review, stage, counts.getOrDefault(c.id(), 0)));
        }

        EffectivenessBoardDto.Summary counted = count(rows);
        rows.sort(byUrgencyThenRecency());
        boolean truncated = rows.size() > MAX_ROWS;
        List<EffectivenessBoardDto.Row> shown = truncated
                ? List.copyOf(rows.subList(0, MAX_ROWS))
                : List.copyOf(rows);

        return new EffectivenessBoardDto.Summary(
                counted.effectiveRatePercent(), counted.evaluated(),
                counted.awaitingEvaluation(), counted.stillOpen(),
                counted.reopened(), counted.partial(), counted.missingCriterion(),
                rows.size(), truncated, shown);
    }

    /**
     * L'historique complet d'un dossier, pour sa fiche.
     *
     * <p>La portée est vérifiée AVANT de lire les évaluations, et un dossier
     * hors de portée répond 404 comme sa propre fiche (ADR 0081) : sans cela,
     * l'historique serait lisible par identifiant — qui a jugé, contre quel
     * critère, avec quels relevés — sur un dossier que la personne n'a pas le
     * droit d'ouvrir. « Vide » n'aurait pas suffi : une liste vide dit qu'il n'y
     * a rien, pas qu'on n'a rien à y voir.
     */
    public List<EffectivenessBoardDto.Review> history(UUID capaCaseId) {
        UUID tenantId = tenants.requireTenantId();
        Optional<UUID> concerning = CapaScope.restriction(scope);
        if (!cases.sees(tenantId, capaCaseId, concerning)) {
            throw new CapaNotFoundException(capaCaseId);
        }
        return reviews.findByCase(tenantId, capaCaseId).stream()
                .map(EffectivenessBoardService::toReview)
                .toList();
    }

    private static EffectivenessBoardDto.Row row(EvaluationCasePort.EvaluationCase c,
                                                 EffectivenessReview review, EvaluationStage stage,
                                                 int reviewCount) {
        // La clôture peut rester empêchée alors même que le jugement est
        // possible : des mesures d'endiguement seules, ou une non-conformité
        // rattachée encore ouverte. L'écran doit pouvoir le dire AVANT le clic —
        // sans quoi la personne découvre l'obstacle au moment où elle croit en
        // avoir fini, ce qui est le défaut que ClosureBlockerCode a déjà corrigé
        // ailleurs.
        boolean blocked = c.containmentOnly() || c.openNcCount() > 0;
        return new EffectivenessBoardDto.Row(c.id(), c.reference(), c.title(), c.criticity(),
                c.ownerName(), stage, stage.allowsEvaluation(),
                c.actionsTotal(), c.actionsDone(),
                c.criterion(), toReview(review), reviewCount,
                blocked, c.containmentOnly(), c.openNcCount(), c.updatedAt());
    }

    private static EffectivenessBoardDto.Review toReview(EffectivenessReview r) {
        if (r == null) {
            return null;
        }
        return new EffectivenessBoardDto.Review(r.id(), r.verdict(), r.criterion(),
                r.baseline(), r.target(), r.observed(), r.comment(),
                r.reviewerName(), r.reviewedAt());
    }

    /**
     * Les six chiffres, comptés sur l'ensemble du périmètre.
     *
     * <p>Rendus dans un {@code Summary} sans lignes : ce n'en est pas un abus,
     * c'est le même enregistrement rempli en deux temps, et cela évite un type
     * de plus dont la seule raison d'être serait de porter six entiers.
     */
    private static EffectivenessBoardDto.Summary count(List<EffectivenessBoardDto.Row> rows) {
        int evaluated = 0;
        int effective = 0;
        int awaiting = 0;
        int stillOpen = 0;
        int reopened = 0;
        int partial = 0;
        int missingCriterion = 0;
        for (EffectivenessBoardDto.Row r : rows) {
            if (r.lastReview() != null) {
                evaluated++;
                if (r.lastReview().verdict() == EffectivenessVerdict.EFFECTIVE) {
                    effective++;
                }
            }
            switch (r.stage()) {
                case AWAITING_EVALUATION -> awaiting++;
                case REOPENED -> reopened++;
                case COMPLEMENTARY_ACTION_EXPECTED -> partial++;
                default -> { }
            }
            if (r.stage().stillOpen()) {
                stillOpen++;
            }
            // Un critère manquant n'est un problème que sur un dossier qu'on
            // peut juger MAINTENANT. Le signaler sur un dossier dont les actions
            // courent encore reviendrait à reprocher un retard à qui est dans les
            // temps.
            // Tous les dossiers ENCORE OUVERTS sans critère, pas seulement ceux
            // déjà prêts à juger. Ne compter que ces derniers ferait apparaître
            // l'alerte à l'instant exact où il est trop tard pour poser un
            // critère « à l'avance » — un indicateur qui reste à zéro tant que
            // le problème est réparable n'avertit de rien.
            if (r.stage().stillOpen() && (r.criterion() == null || r.criterion().isBlank())) {
                missingCriterion++;
            }
        }
        Integer rate = evaluated == 0 ? null : Math.round(100f * effective / evaluated);
        return new EffectivenessBoardDto.Summary(rate, evaluated, awaiting, stillOpen,
                reopened, partial, missingCriterion, rows.size(), false, List.of());
    }

    private static Comparator<EffectivenessBoardDto.Row> byUrgencyThenRecency() {
        return Comparator
                .comparingInt((EffectivenessBoardDto.Row r) -> URGENCY.indexOf(r.stage()))
                // À urgence égale, le dossier touché le plus récemment d'abord :
                // c'est celui dont le contexte est encore frais dans la tête de
                // la personne qui l'a traité.
                .thenComparing(EffectivenessBoardDto.Row::updatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(r -> r.capaId().toString());
    }
}
