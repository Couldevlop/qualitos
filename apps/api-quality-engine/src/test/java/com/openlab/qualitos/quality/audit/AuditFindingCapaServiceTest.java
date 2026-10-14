package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaDto;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditFindingCapaServiceTest {

    @Mock AuditPlanRepository plans;
    @Mock AuditFindingRepository findings;
    @Mock CapaService capaService;
    @InjectMocks AuditFindingCapaService service;

    static final UUID TENANT = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach void clr() { TenantContext.clear(); }

    @Test
    void ouvreLaCapaDuConstat_sourceAuditEtRangDuConstat_etGardeLeLien() {
        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-2026-0003");
        AuditFinding premier = finding(plan, FindingType.OBSERVATION);
        AuditFinding second = finding(plan, FindingType.MAJOR_NC);
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findByIdAndPlanId(second.getId(), plan.getId())).thenReturn(Optional.of(second));
        UUID capaId = UUID.randomUUID();
        CapaDto.CaseResponse ouvert = new CapaDto.CaseResponse(capaId, TENANT, "t", null, CapaType.CORRECTIVE,
                CapaCriticity.HIGH, com.openlab.qualitos.quality.capa.CapaStatus.OPEN, CapaSourceType.AUDIT,
                null, UUID.randomUUID(), null, null, null, null, null, null, null, null, null, null, null,
                null, null, java.util.List.of(), null, java.util.List.of());
        CapaDto.OpenCaseRequest demande = demande();
        when(capaService.openCase(demande, CapaSourceType.AUDIT, "AUD-2026-0003 / constat 2")).thenReturn(ouvert);

        AuditFindingCapaService.FindingCapa r = service.open(plan.getId(), second.getId(), demande);

        assertThat(r.capaId()).isEqualTo(capaId);
        assertThat(r.findingId()).isEqualTo(second.getId());
        assertThat(r.sourceRef()).isEqualTo("AUD-2026-0003 / constat 2");
        assertThat(second.getCapaId()).isEqualTo(capaId);
        assertThat(premier.getCapaId()).isNull();
        verify(findings).save(second);
    }

    @Test
    void unPlanSansReferenceSeCiteParSonTitre_etUnConstatHorsListeSansRang() {
        AuditPlan plan = plan(AuditStatus.COMPLETED, "  ");
        plan.setTitle("Audit interne magasin");
        AuditFinding orphelin = new AuditFinding();
        orphelin.setId(UUID.randomUUID());
        assertThat(AuditFindingCapaService.sourceRef(plan, orphelin)).isEqualTo("Audit interne magasin");
        plan.setReference(null);
        finding(plan, FindingType.MINOR_NC);
        AuditFinding f = finding(plan, FindingType.MINOR_NC);
        assertThat(AuditFindingCapaService.sourceRef(plan, f)).isEqualTo("Audit interne magasin / constat 2");
        plan.setTitle("x".repeat(300));
        assertThat(AuditFindingCapaService.sourceRef(plan, f)).hasSize(255);
    }

    @Test
    void seulUnEcartOuvreUneCapa() {
        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-1");
        AuditFinding obs = finding(plan, FindingType.OPPORTUNITY);
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findByIdAndPlanId(obs.getId(), plan.getId())).thenReturn(Optional.of(obs));

        assertThatThrownBy(() -> service.open(plan.getId(), obs.getId(), demande()))
                .isInstanceOf(AuditStateException.class)
                .hasMessageContaining("non-conforming");
        verifyNoInteractions(capaService);
    }

    @Test
    void unConstatDejaTraiteNOuvrePasUneSecondeCapa() {
        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-1");
        AuditFinding f = finding(plan, FindingType.MINOR_NC);
        f.setCapaId(UUID.randomUUID());
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findByIdAndPlanId(f.getId(), plan.getId())).thenReturn(Optional.of(f));

        assertThatThrownBy(() -> service.open(plan.getId(), f.getId(), demande()))
                .isInstanceOf(AuditStateException.class)
                .hasMessageContaining("already");
        verifyNoInteractions(capaService);
        verify(findings, never()).save(any());
    }

    @Test
    void unAuditAnnuleNOuvreRien() {
        AuditPlan plan = plan(AuditStatus.CANCELLED, "AUD-1");
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.open(plan.getId(), UUID.randomUUID(), demande()))
                .isInstanceOf(AuditStateException.class);
        verifyNoInteractions(findings, capaService);
    }

    @Test
    void unAuditOuUnConstatIntrouvableRend404() {
        UUID planId = UUID.randomUUID();
        when(plans.findByIdAndTenantId(planId, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.open(planId, UUID.randomUUID(), demande()))
                .isInstanceOf(AuditPlanNotFoundException.class);

        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-1");
        UUID findingId = UUID.randomUUID();
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findByIdAndPlanId(findingId, plan.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.open(plan.getId(), findingId, demande()))
                .isInstanceOf(AuditFindingNotFoundException.class);
        verifyNoInteractions(capaService);
    }

    @Test
    void sansTenantRienNeSeLit() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.open(UUID.randomUUID(), UUID.randomUUID(), demande()))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(plans, findings, capaService);
    }

    private static CapaDto.OpenCaseRequest demande() {
        return new CapaDto.OpenCaseRequest("Reprendre la procédure", null, CapaType.CORRECTIVE,
                CapaCriticity.HIGH, null, null, UUID.randomUUID(), "a.diallo@client.test",
                LocalDate.now().plusDays(10));
    }

    private static AuditPlan plan(AuditStatus status, String reference) {
        AuditPlan p = new AuditPlan();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setTitle("Audit interne");
        p.setReference(reference);
        p.setStatus(status);
        return p;
    }

    private static AuditFinding finding(AuditPlan plan, FindingType type) {
        AuditFinding f = new AuditFinding();
        f.setId(UUID.randomUUID());
        f.setPlan(plan);
        f.setType(type);
        plan.getFindings().add(f);
        return f;
    }
}
