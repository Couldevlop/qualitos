package com.openlab.qualitos.quality.smi.infrastructure;

import com.openlab.qualitos.quality.audit.AuditDto;
import com.openlab.qualitos.quality.audit.AuditService;
import com.openlab.qualitos.quality.audit.AuditStatus;
import com.openlab.qualitos.quality.audit.AuditType;
import com.openlab.qualitos.quality.calibration.CalibrationEquipment;
import com.openlab.qualitos.quality.calibration.CalibrationEquipmentRepository;
import com.openlab.qualitos.quality.calibration.CalibrationPlan;
import com.openlab.qualitos.quality.calibration.CalibrationPlanRepository;
import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionRepository;
import com.openlab.qualitos.quality.capa.CapaActionStatus;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.change.ChangeRequest;
import com.openlab.qualitos.quality.change.ChangeRequestRepository;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterDto;
import com.openlab.qualitos.quality.riskregister.application.RiskRegisterService;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.riskregister.domain.RiskDecision;
import com.openlab.qualitos.quality.riskregister.domain.RiskLevel;
import com.openlab.qualitos.quality.riskregister.domain.RiskStatus;
import com.openlab.qualitos.quality.smi.application.SmiPorts;
import com.openlab.qualitos.quality.smi.domain.Deadline;
import com.openlab.qualitos.quality.standards.AdoptionStatus;
import com.openlab.qualitos.quality.standards.StandardsDto;
import com.openlab.qualitos.quality.standards.StandardsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmiAdaptersTest {

    static final UUID TENANT = UUID.randomUUID();
    static final LocalDate J = LocalDate.of(2026, 10, 8);

    @BeforeEach
    void tenant() {
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    static StandardsDto.AdoptionResponse adoption(String code, AdoptionStatus status) {
        return new StandardsDto.AdoptionResponse(UUID.randomUUID(), TENANT, UUID.randomUUID(), code,
                code.toUpperCase(), status, null, null, null, null, null, null, Instant.now(), Instant.now());
    }

    @Test
    void lesNormesRetireesOuExpireesNeSePilotentPlus() {
        StandardsService service = mock(StandardsService.class);
        when(service.listAdoptions(isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                adoption("iso-9001", AdoptionStatus.CERTIFIED), adoption("iso-14001", AdoptionStatus.WITHDRAWN),
                adoption("iso-45001", AdoptionStatus.IN_PROGRESS), adoption("iso-50001", AdoptionStatus.EXPIRED))));

        assertThat(new SmiAdapters.Standards(service).adopted()).extracting(SmiPorts.AdoptedStandard::code)
                .containsExactly("iso-9001", "iso-45001");
    }

    @Test
    void lAlignementReprendLesChapitresDeLaNorme() {
        StandardsService service = mock(StandardsService.class);
        UUID id = UUID.randomUUID();
        when(service.computeAlignment(id)).thenReturn(new StandardsDto.AlignmentReport(id, UUID.randomUUID(),
                "iso-9001", 75d, 4, 3, 2, 2, List.of(new StandardsDto.SectionAlignment(UUID.randomUUID(), "6",
                "Planification", 75d, 4, 3, List.of()))));

        SmiPorts.Alignment a = new SmiAdapters.Standards(service).alignment(id).orElseThrow();

        assertThat(a.overallScore()).isEqualTo(75d);
        assertThat(a.sections()).containsExactly(new SmiPorts.SectionCoverage("6", "Planification", 3, 4));
    }

    @Test
    void lesActionsEnRetardSeComptentEnBaseDansLeClientDuJeton() {
        CapaActionRepository repo = mock(CapaActionRepository.class);
        when(repo.countOverdue(eq(TENANT), eq(CapaActionStatus.DONE), eq(J), anyCollection())).thenReturn(12L);
        when(repo.countOverdueWithCriticity(eq(TENANT), eq(CapaActionStatus.DONE), eq(J), anyCollection(),
                eq(CapaCriticity.CRITICAL))).thenReturn(3L);

        assertThat(new SmiAdapters.Actions(repo).overdue(J)).isEqualTo(new SmiPorts.OverdueActions(12, 3));
    }

    @Test
    void uneActionDueRenvoieASonDossier() {
        CapaActionRepository repo = mock(CapaActionRepository.class);
        CapaCase dossier = new CapaCase();
        dossier.setId(UUID.randomUUID());
        dossier.setSourceRef("R-014");
        CapaAction a = new CapaAction();
        a.setCapa(dossier);
        a.setTitle("Clôture CAPA");
        a.setDueDate(J.plusDays(2));
        when(repo.findDueBy(eq(TENANT), eq(CapaActionStatus.DONE), eq(J.plusDays(7)), anyCollection(),
                any(Pageable.class))).thenReturn(List.of(a));

        assertThat(new SmiAdapters.Actions(repo).dueBy(J.plusDays(7), 8)).containsExactly(
                new Deadline(Deadline.Kind.CAPA_ACTION, dossier.getId(), "R-014", "Clôture CAPA", J.plusDays(2)));
    }

    static RiskRegisterDto.RiskView risque(String ref, RiskStatus statut) {
        return new RiskRegisterDto.RiskView(UUID.randomUUID(), ref, "Dérive", RegisterType.QUALITY, "Production",
                null, "M. Kone", "c", "e", RegisterOrigin.DIRECT, null, null, 4, 3, 12, RiskLevel.HIGH, 4, 2, 8,
                RiskLevel.MEDIUM, RiskDecision.REDUCE, statut, List.of(RegisterRequirement.ISO_45001_6_1),
                J.plusDays(3), null, Instant.now(), Instant.now());
    }

    @Test
    void unRisqueOuvertEstUnRisqueNonClosAcceptesCompris() {
        RiskRegisterService service = mock(RiskRegisterService.class);
        when(service.risks()).thenReturn(List.of(risque("R-1", RiskStatus.TO_TREAT),
                risque("R-2", RiskStatus.CLOSED), risque("R-3", RiskStatus.ACCEPTED)));

        List<SmiPorts.OpenRisk> ouverts = new SmiAdapters.Risks(service).open();

        assertThat(ouverts).extracting(SmiPorts.OpenRisk::reference).containsExactly("R-1", "R-3");
        assertThat(ouverts.get(0)).satisfies(r -> {
            assertThat(r.grossLevel()).isEqualTo("HIGH");
            assertThat(r.requirements()).containsExactly("ISO_45001_6_1");
            assertThat(r.residualProbability()).isEqualTo(2);
            assertThat(r.nextReviewOn()).isEqualTo(J.plusDays(3));
        });
    }

    @Test
    void lesAuditsPlanifiesViennentDuPlanning() {
        AuditService service = mock(AuditService.class);
        UUID id = UUID.randomUUID();
        when(service.planning(null, 365)).thenReturn(List.of(
                new AuditDto.PlanningEntry(id, "AUD-1", "Interne", AuditType.INTERNAL, AuditStatus.PLANNED,
                        "ISO 9001", null, J.plusDays(20), 20, false, false),
                new AuditDto.PlanningEntry(UUID.randomUUID(), "AUD-2", "Sans type", null, AuditStatus.PLANNED,
                        null, null, J.plusDays(30), 30, false, false)));

        List<SmiPorts.PlannedAudit> audits = new SmiAdapters.Audits(service).planned(365);

        assertThat(audits.get(0)).isEqualTo(new SmiPorts.PlannedAudit(id, "AUD-1", "Interne", "INTERNAL",
                "ISO 9001", J.plusDays(20)));
        assertThat(audits.get(1).type()).isNull();
    }

    @Test
    void etalonnagesEtChangementsEnAttenteFontDesEcheancesNommees() {
        CalibrationPlanRepository plans = mock(CalibrationPlanRepository.class);
        CalibrationEquipmentRepository equipements = mock(CalibrationEquipmentRepository.class);
        ChangeRequestRepository changes = mock(ChangeRequestRepository.class);

        CalibrationEquipment pied = equipement("PC-118", "Pied à coulisse", TENANT);
        CalibrationEquipment etranger = equipement("X-1", "Autre client", UUID.randomUUID());
        when(plans.findByTenantIdAndNextDueOnBefore(eq(TENANT), eq(J.plusDays(8)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(plan(pied, J.plusDays(2)), plan(etranger, J))));
        when(equipements.findAllById(any())).thenReturn(List.of(pied, etranger));

        ChangeRequest moc = new ChangeRequest();
        moc.setId(UUID.randomUUID());
        moc.setCode("MOC-7");
        moc.setTitle("Changement de gamme");
        moc.setPlannedFor(J.plusDays(3));
        when(changes.findByTenantIdAndStatusInAndPlannedForLessThanEqualOrderByPlannedForAsc(
                eq(TENANT), anyCollection(), eq(J.plusDays(7)), any(Pageable.class))).thenReturn(List.of(moc));

        List<Deadline> echeances = new SmiAdapters.Deadlines(plans, equipements, changes).dueBy(J.plusDays(7), 8);

        assertThat(echeances).containsExactly(
                new Deadline(Deadline.Kind.CALIBRATION, pied.getId(), "PC-118", "Pied à coulisse", J.plusDays(2)),
                new Deadline(Deadline.Kind.CHANGE, moc.getId(), "MOC-7", "Changement de gamme", J.plusDays(3)));
    }

    @Test
    void sansClientDansLeJetonRienNeSeLit() {
        TenantContext.clear();
        assertThatThrownBy(() -> new SmiAdapters.Actions(mock(CapaActionRepository.class)).overdue(J))
                .isInstanceOf(MissingTenantContextException.class);
    }

    static CalibrationEquipment equipement(String code, String nom, UUID tenant) {
        CalibrationEquipment e = new CalibrationEquipment();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenant);
        e.setCode(code);
        e.setName(nom);
        return e;
    }

    static CalibrationPlan plan(CalibrationEquipment e, LocalDate due) {
        CalibrationPlan p = new CalibrationPlan();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setEquipmentId(e.getId());
        p.setNextDueOn(due);
        return p;
    }
}
