package com.openlab.qualitos.quality.spc.gagerr.application;

import com.openlab.qualitos.quality.spc.capability.application.SpcContext;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrAnova;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrCalculation;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrRepository;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudy;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrStudyDetails;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Les études Gage R&amp;R : le registre, la fiche et la saisie.
 *
 * <p>Sans Spring ni JPA. Les transactions sont posées par le contrôleur : une
 * étude et sa trace d'audit sont validées ensemble ou pas du tout. Le tenant
 * et l'acteur viennent du jeton, par le port {@link SpcContext} partagé avec
 * les études de capabilité.
 */
public class GageRrStudyService {

    private final GageRrRepository studies;
    private final SpcContext context;
    private final GageRrAuditPublisher audit;
    private final Clock clock;

    public GageRrStudyService(GageRrRepository studies, SpcContext context, GageRrAuditPublisher audit,
                              Clock clock) {
        this.studies = studies;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    /** Le registre, les études les plus récentes d'abord. */
    public List<GageRrStudyDto.StudySummary> list() {
        UUID tenant = context.requireTenantId();
        return studies.findByTenant(tenant).stream()
                .sorted(Comparator.comparing(GageRrStudy::getStudyDate).reversed()
                        .thenComparing(GageRrStudy::getUpdatedAt, Comparator.reverseOrder()))
                .map(GageRrStudyService::ligne)
                .toList();
    }

    public GageRrStudyDto.StudySheet get(UUID id) {
        return sheetOf(charger(id, context.requireTenantId()));
    }

    public GageRrStudyDto.StudySheet create(GageRrStudyDto.StudyCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        GageRrStudy etude = studies.save(GageRrStudy.record(tenant, details(commande), acteur, clock.instant()));
        audit.recorded(etude, acteur);
        return sheetOf(etude);
    }

    public GageRrStudyDto.StudySheet revise(UUID id, GageRrStudyDto.StudyCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        GageRrStudy etude = charger(id, tenant);
        etude.revise(details(commande), clock.instant());
        GageRrStudy enregistree = studies.save(etude);
        audit.revised(enregistree, acteur);
        return sheetOf(enregistree);
    }

    public void delete(UUID id) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        GageRrStudy etude = charger(id, tenant);
        studies.delete(etude);
        audit.deleted(etude, acteur);
    }

    private GageRrStudy charger(UUID id, UUID tenant) {
        return studies.findByIdAndTenant(id, tenant).orElseThrow(() -> new SpcNotFoundException(id));
    }

    private static GageRrStudyDetails details(GageRrStudyDto.StudyCommand c) {
        return new GageRrStudyDetails(c.partReference(), c.characteristic(), c.instrument(), c.lsl(), c.usl(),
                c.operatorCount(), c.partCount(), c.trialCount(), c.sigmaMultiplier(), c.studyDate(),
                c.responsibleUserId(), c.responsibleName(), c.operators(), c.measurements());
    }

    private static GageRrStudyDto.StudySummary ligne(GageRrStudy e) {
        return new GageRrStudyDto.StudySummary(e.getId(), e.getPartReference(), e.getCharacteristic(),
                e.getInstrument(), e.getStudyDate(), e.getResponsibleUserId(), e.getResponsibleName(),
                e.getOperators(), e.getOperatorCount(), e.getPartCount(), e.getTrialCount(),
                e.getSigmaMultiplier(), e.getGrrPercent(), e.getGrrTolerancePercent(), e.getNdc(),
                e.getDecision(), e.getAnovaGrrPercent(), e.getAnovaNdc(), e.getUpdatedAt());
    }

    /** La fiche complète d'une étude : identification, grille, et les deux calculs refaits. */
    public static GageRrStudyDto.StudySheet sheetOf(GageRrStudy e) {
        GageRrCalculation c = e.calculation();
        GageRrAnova a = c.anova();
        GageRrStudyDto.AnovaView anova = new GageRrStudyDto.AnovaView(a.fullTable(), a.pooledTable(),
                a.interactionPooled(), a.variance(), a.ev(), a.av(), a.operator(), a.interaction(), a.grr(),
                a.pv(), a.tv(), a.ndc(), a.ndc() >= GageRrCalculation.NDC_SUFFICIENT, a.decision());
        GageRrStudyDto.CalculationView calcul = new GageRrStudyDto.CalculationView(
                c.cellMeans(), c.cellRanges(), c.trialMeans(), c.operatorMeans(), c.operatorRanges(),
                c.partMeans(), c.grandMean(), c.xDiff(), c.meanRange(), c.partRange(), c.rangeUcl(),
                c.maxRange(), c.rangesBeyondLimit(), c.tolerance(),
                c.constants().k1(), c.constants().k2(), c.constants().k3(), c.constants().d4(), c.multiplier(),
                c.ev(), c.av(), c.grr(), c.pv(), c.tv(), c.ndc(), c.ndcSufficient(), c.decision(),
                c.repeatabilityDominant(), anova);
        return new GageRrStudyDto.StudySheet(ligne(e), e.getLsl(), e.getUsl(), e.getMeasurements(), calcul);
    }
}
