package com.openlab.qualitos.quality.spc.capability.application;

import com.openlab.qualitos.quality.spc.capability.domain.CapabilityCalculation;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStudy;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStudyDetails;
import com.openlab.qualitos.quality.spc.capability.domain.CapabilityThresholds;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.capability.domain.SpcRepositories;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Les études de capabilité (Cp / Cpk) : le registre, la fiche, la saisie, et
 * les seuils de Cpk du client.
 *
 * <p>Sans Spring ni JPA. Les transactions sont posées par le contrôleur : une
 * étude et sa trace d'audit sont validées ensemble ou pas du tout.
 */
public class SpcStudyService {

    private final SpcRepositories.Studies studies;
    private final SpcRepositories.Settings settings;
    private final SpcContext context;
    private final SpcAuditPublisher audit;
    private final Clock clock;

    public SpcStudyService(SpcRepositories.Studies studies, SpcRepositories.Settings settings,
                           SpcContext context, SpcAuditPublisher audit, Clock clock) {
        this.studies = studies;
        this.settings = settings;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------- lecture ----------

    /** Le registre, les études les plus récentes d'abord. */
    public List<SpcStudyDto.StudySummary> list() {
        UUID tenant = context.requireTenantId();
        return studies.findByTenant(tenant).stream()
                .sorted(Comparator.comparing(CapabilityStudy::getStudyDate).reversed()
                        .thenComparing(CapabilityStudy::getUpdatedAt, Comparator.reverseOrder()))
                .map(SpcStudyService::ligne)
                .toList();
    }

    public SpcStudyDto.StudySheet get(UUID id) {
        return fiche(charger(id, context.requireTenantId()));
    }

    // ---------- écriture ----------

    public SpcStudyDto.StudySheet create(SpcStudyDto.StudyCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        CapabilityStudy etude = studies.save(CapabilityStudy.record(tenant, details(commande),
                seuils(tenant), acteur, clock.instant()));
        audit.recorded(etude, acteur);
        return fiche(etude);
    }

    public SpcStudyDto.StudySheet revise(UUID id, SpcStudyDto.StudyCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        CapabilityStudy etude = charger(id, tenant);
        etude.revise(details(commande), clock.instant());
        CapabilityStudy enregistree = studies.save(etude);
        audit.revised(enregistree, acteur);
        return fiche(enregistree);
    }

    /** Le plan d'actions se coche depuis le registre : la grille n'est ni relue ni recalculée. */
    public SpcStudyDto.StudySummary setActionPlan(UUID id, boolean actionPlan) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        CapabilityStudy etude = charger(id, tenant);
        etude.setActionPlan(actionPlan, clock.instant());
        CapabilityStudy enregistree = studies.save(etude);
        audit.actionPlanChanged(enregistree, acteur);
        return ligne(enregistree);
    }

    public void delete(UUID id) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        CapabilityStudy etude = charger(id, tenant);
        studies.delete(etude);
        audit.deleted(etude, acteur);
    }

    // ---------- paramétrage ----------

    /** Les seuils en vigueur : ceux du client, ou ceux de la plateforme s'il n'a rien réglé. */
    public SpcStudyDto.SettingsView settings() {
        UUID tenant = context.requireTenantId();
        Optional<CapabilityThresholds> regles = settings.thresholds(tenant);
        CapabilityThresholds s = regles.orElse(CapabilityThresholds.DEFAULT);
        return new SpcStudyDto.SettingsView(s.capable(), s.watch(), regles.isEmpty());
    }

    /** Ne touche que les études FUTURES : les études enregistrées gardent leurs seuils. */
    public SpcStudyDto.SettingsView updateSettings(SpcStudyDto.SettingsCommand commande) {
        UUID tenant = context.requireTenantId();
        UUID acteur = context.requireActorId();
        CapabilityThresholds s = new CapabilityThresholds(commande.capableThreshold(), commande.watchThreshold());
        settings.saveThresholds(tenant, s, acteur);
        audit.thresholdsChanged(tenant, s, acteur);
        return new SpcStudyDto.SettingsView(s.capable(), s.watch(), false);
    }

    // ---------- interne ----------

    private CapabilityThresholds seuils(UUID tenant) {
        return settings.thresholds(tenant).orElse(CapabilityThresholds.DEFAULT);
    }

    private CapabilityStudy charger(UUID id, UUID tenant) {
        return studies.findByIdAndTenant(id, tenant).orElseThrow(() -> new SpcNotFoundException(id));
    }

    private static CapabilityStudyDetails details(SpcStudyDto.StudyCommand c) {
        return new CapabilityStudyDetails(c.partReference(), c.characteristic(), c.specification(), c.machine(),
                c.leaderUserId(), c.leaderName(), c.studyDate(), c.actionPlan(), c.lsl(), c.usl(),
                c.subgroupSize(), c.measurements());
    }

    private static SpcStudyDto.StudySummary ligne(CapabilityStudy e) {
        return new SpcStudyDto.StudySummary(e.getId(), e.getPartReference(), e.getCharacteristic(),
                e.getSpecification(), e.getMachine(), e.getLeaderUserId(), e.getLeaderName(), e.getStudyDate(),
                e.isActionPlan(), e.getCp(), e.getCpk(), e.status(),
                e.getThresholds().capable(), e.getThresholds().watch(), e.getUpdatedAt());
    }

    private static SpcStudyDto.StudySheet fiche(CapabilityStudy e) {
        CapabilityCalculation c = e.calculation();
        SpcStudyDto.CalculationView calcul = new SpcStudyDto.CalculationView(
                c.subgroupMeans(), c.subgroupRanges(), c.completeSubgroups(),
                c.sumOfMeans(), c.grandMean(), c.meanRange(), c.sigma(),
                c.cp(), c.cpu(), c.cpl(), c.cpk(),
                c.meanUcl(), c.meanLcl(), c.rangeUcl(), c.rangeLcl(),
                c.constants().d2(), c.constants().a2(), c.constants().d3(), c.constants().d4(),
                c.dispersionPercent(), c.centerOffset(), c.centeringPercent());
        return new SpcStudyDto.StudySheet(ligne(e), e.getLsl(), e.getUsl(), e.getSubgroupSize(),
                e.getMeasurements(), calcul);
    }
}
