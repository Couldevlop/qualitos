package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.capa.ObjectiveRecoveryPort;
import com.openlab.qualitos.quality.kpi.KpiDefinition;
import com.openlab.qualitos.quality.kpi.KpiDefinitionRepository;
import com.openlab.qualitos.quality.kpi.KpiMeasurement;
import com.openlab.qualitos.quality.kpi.KpiMeasurementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Répond au module CAPA ce que vaut, aujourd'hui, le retour sous la cible de
 * l'objectif dont vient un dossier.
 *
 * <p><b>Ne dépend PAS de {@link QualityObjectiveService}</b>, et c'est la raison
 * de son existence : le service des objectifs appelle {@code CapaService} pour
 * créer les dossiers, et {@code CapaService} a besoin de cette réponse avant
 * d'accepter un verdict qui clôt. Passer par le service fermerait le cycle. Les
 * dépôts, eux, ne dépendent de personne.
 *
 * <p>Voir l'ADR 0133.
 */
@Component
public class ObjectiveRecoveryAdapter implements ObjectiveRecoveryPort {

    private static final int MAX_PERIODS = 24;

    private final QualityObjectiveProposalRepository proposals;
    private final QualityObjectiveRepository objectives;
    private final KpiDefinitionRepository kpis;
    private final KpiMeasurementRepository measurements;

    public ObjectiveRecoveryAdapter(QualityObjectiveProposalRepository proposals,
                                    QualityObjectiveRepository objectives,
                                    KpiDefinitionRepository kpis,
                                    KpiMeasurementRepository measurements) {
        this.proposals = proposals;
        this.objectives = objectives;
        this.kpis = kpis;
        this.measurements = measurements;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Recovery> forCase(UUID tenantId, UUID capaCaseId) {
        if (tenantId == null || capaCaseId == null) return Optional.empty();
        Optional<QualityObjectiveProposal> proposition =
                proposals.findByTenantIdAndCapaCaseId(tenantId, capaCaseId);
        if (proposition.isEmpty()) {
            // Le dossier ne vient pas d'un objectif qualité : la question ne se
            // pose pas. Vide ne veut pas dire « pas encore revenu », sans quoi
            // tout dossier ordinaire deviendrait impossible à clore.
            return Optional.empty();
        }
        QualityObjectiveProposal p = proposition.get();
        Optional<QualityObjective> cible =
                objectives.findByIdAndTenantId(p.getObjectiveId(), tenantId);
        if (cible.isEmpty()) return Optional.empty();
        QualityObjective o = cible.get();

        KpiDefinition kpi = kpis.findById(o.getKpiId())
                .filter(k -> k.getTenantId().equals(tenantId))
                .orElse(null);
        if (kpi == null || kpi.getDirection() == null) {
            // Sans KPI ni sens d'amélioration, « revenu sous la cible » n'a pas
            // de sens. On répond « pas revenu » plutôt que rien : laisser clore
            // reviendrait à déclarer l'efficacité acquise faute de pouvoir la
            // mesurer.
            return Optional.of(new Recovery(false, 0, o.getRecoveryPeriods(),
                    p.getEffectivenessCriterion()));
        }

        List<KpiMeasurement> releves = new ArrayList<>(
                measurements.findTop24ByKpiIdOrderByPeriodStartDesc(o.getKpiId()));
        releves.sort(Comparator.comparing(KpiMeasurement::getPeriodStart,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        ObjectiveAssessment a = ObjectiveEvaluator.evaluate(o, kpi.getDirection(),
                ObjectivePeriodSeries.values(
                        ObjectivePeriodSeries.split(releves, o.getPeriodMonths(), MAX_PERIODS)),
                null);

        return Optional.of(new Recovery(a.recoveryAchieved(), a.onTargetPeriods(),
                o.getRecoveryPeriods(), p.getEffectivenessCriterion()));
    }
}
