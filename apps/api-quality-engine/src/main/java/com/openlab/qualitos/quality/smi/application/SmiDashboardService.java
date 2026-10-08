package com.openlab.qualitos.quality.smi.application;

import com.openlab.qualitos.quality.smi.domain.CoverageStatus;
import com.openlab.qualitos.quality.smi.domain.Deadline;
import com.openlab.qualitos.quality.smi.domain.HlsChapter;
import com.openlab.qualitos.quality.smi.domain.RiskGrid;
import com.openlab.qualitos.quality.smi.domain.StandardScope;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Le tableau de bord du système de management intégré (SMI).
 *
 * <p>Une page qui répond à « où en est-on, toutes normes confondues ? » :
 * conformité par norme, actions en retard, risques majeurs et leur matrice,
 * prochain audit, échéances de la semaine — puis la matrice des exigences, qui
 * montre qu'une même preuve compte pour plusieurs normes (§8.9).
 *
 * <p>Le filtre « une norme » restreint ce qui se rattache à une norme : la
 * conformité, les risques (par les exigences qu'ils couvrent), les audits (par
 * le référentiel qu'ils nomment). Les actions CAPA et les étalonnages, qui ne
 * portent aucune norme, restent comptés en entier — les filtrer les ferait
 * disparaître sans raison.
 */
public class SmiDashboardService {

    /** Le tableau de bord montre au plus huit barres de conformité (§6.1 : pas de paralysie). */
    static final int MAX_STANDARDS = 8;
    /** « Cette semaine » : aujourd'hui et les sept jours qui suivent, retards compris. */
    static final int WEEK_DAYS = 7;
    static final int MAX_UPCOMING = 8;
    /** Le prochain audit se cherche sur un an. */
    static final int AUDIT_HORIZON_DAYS = 365;
    static final int MAJOR_RISK_SCORE = 10;

    private final SmiPorts.Standards standards;
    private final SmiPorts.Actions actions;
    private final SmiPorts.Risks risks;
    private final SmiPorts.Audits audits;
    private final SmiPorts.Deadlines deadlines;
    private final Clock clock;

    public SmiDashboardService(SmiPorts.Standards standards, SmiPorts.Actions actions, SmiPorts.Risks risks,
                               SmiPorts.Audits audits, SmiPorts.Deadlines deadlines, Clock clock) {
        this.standards = standards;
        this.actions = actions;
        this.risks = risks;
        this.audits = audits;
        this.deadlines = deadlines;
        this.clock = clock;
    }

    public SmiDto.Dashboard dashboard(String standardCode) {
        LocalDate today = LocalDate.now(clock);
        List<SmiPorts.AdoptedStandard> adoptees = adopted();
        // Une norme que le client n'a pas adoptée ne filtre rien : on le dit en
        // rendant « selected » nul, plutôt que d'afficher un tableau vide.
        StandardScope scope = Optional.ofNullable(StandardScope.of(standardCode))
                .filter(s -> adoptees.stream().anyMatch(a -> a.code().equals(s.code())))
                .orElse(null);

        List<SmiPorts.OpenRisk> ouverts = risks.open().stream()
                .filter(r -> scope == null || scope.coversAny(r.requirements()))
                .toList();
        List<SmiPorts.PlannedAudit> planifies = audits.planned(AUDIT_HORIZON_DAYS).stream()
                .filter(a -> scope == null || scope.namedIn(a.standard()))
                .filter(a -> a.scheduledOn() != null)
                .sorted(Comparator.comparing(SmiPorts.PlannedAudit::scheduledOn))
                .toList();

        return new SmiDto.Dashboard(
                adoptees.stream().map(a -> new SmiDto.StandardRef(a.adoptionId(), a.code(), a.name())).toList(),
                scope == null ? null : scope.code(),
                compliance(adoptees, scope),
                overdue(today),
                majorRisks(ouverts),
                matrix(ouverts),
                nextAudit(planifies, today),
                thisWeek(today, ouverts, planifies));
    }

    /** Chapitres communs (4 à 10) × normes adoptées. */
    public SmiDto.RequirementsMatrix requirementsMatrix() {
        List<SmiPorts.AdoptedStandard> adoptees = adopted();
        Map<String, Map<String, SmiPorts.SectionCoverage>> parNorme = adoptees.stream()
                .collect(Collectors.toMap(SmiPorts.AdoptedStandard::code,
                        a -> standards.alignment(a.adoptionId())
                                .map(al -> al.sections().stream().collect(Collectors.toMap(
                                        SmiPorts.SectionCoverage::code, Function.identity(), (x, y) -> x)))
                                .orElse(Map.of()),
                        (x, y) -> x));

        List<SmiDto.ChapterRow> lignes = new ArrayList<>();
        for (HlsChapter chapitre : HlsChapter.values()) {
            List<SmiDto.Cell> cases = adoptees.stream().map(a -> {
                SmiPorts.SectionCoverage s = parNorme.getOrDefault(a.code(), Map.of()).get(chapitre.code());
                return s == null
                        ? new SmiDto.Cell(a.code(), CoverageStatus.NOT_APPLICABLE, 0, 0, null)
                        : new SmiDto.Cell(a.code(), CoverageStatus.of(s.covered(), s.total()),
                        s.covered(), s.total(), s.title());
            }).toList();
            lignes.add(new SmiDto.ChapterRow(chapitre.code(), chapitre.modules(), cases));
        }
        return new SmiDto.RequirementsMatrix(
                adoptees.stream().map(a -> new SmiDto.StandardRef(a.adoptionId(), a.code(), a.name())).toList(),
                lignes);
    }

    // ---------- blocs ----------

    private List<SmiPorts.AdoptedStandard> adopted() {
        return standards.adopted().stream()
                .sorted(Comparator.comparing(SmiPorts.AdoptedStandard::code))
                .limit(MAX_STANDARDS)
                .toList();
    }

    private SmiDto.Compliance compliance(List<SmiPorts.AdoptedStandard> adoptees, StandardScope scope) {
        if (adoptees.isEmpty()) {
            return null;
        }
        List<SmiDto.StandardScore> barres = adoptees.stream()
                .map(a -> standards.alignment(a.adoptionId())
                        .map(al -> new SmiDto.StandardScore(a.code(), a.name(), percent(al.overallScore())))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        Integer global = scope == null
                ? (barres.isEmpty() ? null
                : (int) Math.round(barres.stream().mapToInt(SmiDto.StandardScore::score).average().orElse(0)))
                : barres.stream().filter(b -> b.code().equals(scope.code()))
                .map(SmiDto.StandardScore::score).findFirst().orElse(null);
        return new SmiDto.Compliance(global, barres);
    }

    private SmiDto.OverdueActions overdue(LocalDate today) {
        SmiPorts.OverdueActions o = actions.overdue(today);
        return new SmiDto.OverdueActions(o.total(), o.critical());
    }

    private static SmiDto.MajorRisks majorRisks(List<SmiPorts.OpenRisk> ouverts) {
        int critiques = 0;
        int eleves = 0;
        for (SmiPorts.OpenRisk r : ouverts) {
            if (r.grossSeverity() * r.grossProbability() < MAJOR_RISK_SCORE) {
                continue;
            }
            if ("CRITICAL".equals(r.grossLevel())) {
                critiques++;
            } else {
                eleves++;
            }
        }
        return new SmiDto.MajorRisks(critiques + eleves, critiques, eleves);
    }

    private static SmiDto.RiskMatrix matrix(List<SmiPorts.OpenRisk> ouverts) {
        RiskGrid brute = RiskGrid.of(ouverts.stream()
                .map(r -> new RiskGrid.Rating(r.grossSeverity(), r.grossProbability())).toList());
        RiskGrid residuelle = RiskGrid.of(ouverts.stream()
                .map(r -> new RiskGrid.Rating(r.residualSeverity(), r.residualProbability())).toList());
        return new SmiDto.RiskMatrix(ouverts.size(), brute.counts(), residuelle.counts());
    }

    private static SmiDto.NextAudit nextAudit(List<SmiPorts.PlannedAudit> planifies, LocalDate today) {
        return planifies.stream()
                .filter(a -> !a.scheduledOn().isBefore(today))
                .findFirst()
                .map(a -> new SmiDto.NextAudit(a.id(), a.reference(), a.title(), a.type(), a.standard(),
                        a.scheduledOn(), ChronoUnit.DAYS.between(today, a.scheduledOn())))
                .orElse(null);
    }

    private List<SmiDto.Upcoming> thisWeek(LocalDate today, List<SmiPorts.OpenRisk> ouverts,
                                           List<SmiPorts.PlannedAudit> planifies) {
        LocalDate fin = today.plusDays(WEEK_DAYS);
        List<Deadline> tout = new ArrayList<>(actions.dueBy(fin, MAX_UPCOMING));
        tout.addAll(deadlines.dueBy(fin, MAX_UPCOMING));
        ouverts.stream()
                .filter(r -> r.nextReviewOn() != null && !r.nextReviewOn().isAfter(fin))
                .map(r -> new Deadline(Deadline.Kind.RISK_REVIEW, r.id(), r.reference(), r.title(),
                        r.nextReviewOn()))
                .forEach(tout::add);
        planifies.stream()
                .filter(a -> !a.scheduledOn().isAfter(fin))
                .map(a -> new Deadline(Deadline.Kind.AUDIT, a.id(), a.reference(), a.title(), a.scheduledOn()))
                .forEach(tout::add);
        return tout.stream()
                .filter(d -> d.dueOn() != null)
                .sorted(Comparator.comparing(Deadline::dueOn).thenComparing(Deadline::reference,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_UPCOMING)
                .map(d -> new SmiDto.Upcoming(d.kind(), d.targetId(), d.reference(), d.title(), d.dueOn(),
                        ChronoUnit.DAYS.between(today, d.dueOn())))
                .toList();
    }

    private static int percent(double score) {
        return (int) Math.round(Math.clamp(score, 0d, 100d));
    }
}
