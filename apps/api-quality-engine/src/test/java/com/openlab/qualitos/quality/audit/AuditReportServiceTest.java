package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Rapport d'audit : brouillon, puis émission qui fige.
 *
 * <p>Ce qui se teste : la synthèse reste celle du plan (aucune seconde copie
 * vivante), l'émission exige un audit clôturé, une synthèse et une conclusion,
 * elle fige chiffres clés et texte, prend l'émetteur dans le jeton, calcule une
 * empreinte, s'inscrit au journal — et plus rien ne bouge ensuite.
 */
@ExtendWith(MockitoExtension.class)
class AuditReportServiceTest {

    @Mock AuditPlanRepository plans;
    @Mock AuditReportRepository reports;
    @Mock AuditEventService auditEvents;

    private AuditReportService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID ME = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-14T09:30:00Z");

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        service = new AuditReportService(plans, reports, auditEvents, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private static void connecte() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(ME.toString())
                .claim("name", "Alice Martin").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private AuditPlan plan(AuditStatus status, FindingType... findings) {
        AuditPlan p = new AuditPlan();
        p.setId(PLAN);
        p.setTenantId(TENANT);
        p.setReference("AUD-2026-0007");
        p.setStatus(status);
        for (FindingType t : findings) {
            AuditFinding f = new AuditFinding();
            f.setPlan(p);
            f.setType(t);
            p.getFindings().add(f);
        }
        when(plans.findByIdAndTenantId(PLAN, TENANT)).thenReturn(Optional.of(p));
        return p;
    }

    private AuditReport draft(AuditConclusion conclusion) {
        AuditReport r = new AuditReport();
        r.setId(UUID.randomUUID());
        r.setTenantId(TENANT);
        r.setPlanId(PLAN);
        r.setConclusion(conclusion);
        when(reports.findByTenantIdAndPlanId(TENANT, PLAN)).thenReturn(Optional.of(r));
        return r;
    }

    private void saveEchoes() {
        when(reports.save(any(AuditReport.class))).thenAnswer(inv -> {
            AuditReport r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
    }

    // --- lecture ---

    @Test
    void sansBrouillon_rapportVierge_avecChiffresEnDirect() {
        AuditPlan p = plan(AuditStatus.IN_PROGRESS, FindingType.MAJOR_NC, FindingType.MINOR_NC,
                FindingType.MINOR_NC, FindingType.OBSERVATION);
        p.setReportSummary("Synthèse IA");
        when(reports.findByTenantIdAndPlanId(TENANT, PLAN)).thenReturn(Optional.empty());

        AuditReportDto.View v = service.get(PLAN);

        assertThat(v.id()).isNull();
        assertThat(v.status()).isEqualTo(AuditReportStatus.DRAFT);
        assertThat(v.summary()).isEqualTo("Synthèse IA");
        assertThat(v.keyFigures().majorNc()).isEqualTo(1);
        assertThat(v.keyFigures().minorNc()).isEqualTo(2);
        assertThat(v.keyFigures().observation()).isEqualTo(1);
        assertThat(v.keyFigures().total()).isEqualTo(4);
    }

    @Test
    void autreTenant_introuvable() {
        when(plans.findByIdAndTenantId(PLAN, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(PLAN)).isInstanceOf(AuditPlanNotFoundException.class);
    }

    @Test
    void sansTenant_refuse() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.get(PLAN)).isInstanceOf(MissingTenantContextException.class);
    }

    // --- brouillon ---

    @Test
    void brouillon_ecritLaSyntheseDansLePlan_etLeResteDansLeRapport() {
        connecte();
        AuditPlan p = plan(AuditStatus.COMPLETED);
        when(reports.findByTenantIdAndPlanId(TENANT, PLAN)).thenReturn(Optional.empty());
        saveEchoes();

        AuditReportDto.View v = service.saveDraft(PLAN, new AuditReportDto.DraftRequest(
                "  Synthèse rédigée  ", "Pilotage solide", "   ", AuditConclusion.FAVORABLE_WITH_RESERVATIONS));

        assertThat(p.getReportSummary()).isEqualTo("Synthèse rédigée");
        assertThat(v.summary()).isEqualTo("Synthèse rédigée");
        assertThat(v.strengths()).isEqualTo("Pilotage solide");
        assertThat(v.improvements()).isNull();
        assertThat(v.conclusion()).isEqualTo(AuditConclusion.FAVORABLE_WITH_RESERVATIONS);
        verify(plans).save(p);
        ArgumentCaptor<AuditReport> saved = ArgumentCaptor.forClass(AuditReport.class);
        verify(reports).save(saved.capture());
        assertThat(saved.getValue().getTenantId()).isEqualTo(TENANT);
        assertThat(saved.getValue().getUpdatedBy()).isEqualTo(ME);
    }

    @Test
    void brouillon_auditAnnule_refuse() {
        plan(AuditStatus.CANCELLED);
        assertThatThrownBy(() -> service.saveDraft(PLAN,
                new AuditReportDto.DraftRequest("s", null, null, null)))
                .isInstanceOf(AuditStateException.class);
    }

    @Test
    void brouillon_rapportEmis_refuse() {
        AuditPlan p = plan(AuditStatus.COMPLETED);
        p.setReportSummary("figée");
        AuditReport r = draft(AuditConclusion.FAVORABLE);
        r.setStatus(AuditReportStatus.ISSUED);

        assertThatThrownBy(() -> service.saveDraft(PLAN,
                new AuditReportDto.DraftRequest("réécrite", null, null, null)))
                .isInstanceOf(AuditStateException.class);
        assertThat(p.getReportSummary()).isEqualTo("figée");
        verify(reports, never()).save(any());
    }

    // --- émission ---

    @Test
    void emission_figeLeContenu_prendLEmetteurDansLeJeton_etJournalise() {
        connecte();
        AuditPlan p = plan(AuditStatus.COMPLETED, FindingType.MAJOR_NC, FindingType.CONFORMITY,
                FindingType.OPPORTUNITY);
        p.setReportSummary("Synthèse finale");
        AuditReport r = draft(AuditConclusion.FAVORABLE_WITH_RESERVATIONS);
        r.setStrengths("Forces");
        saveEchoes();

        AuditReportDto.View v = service.issue(PLAN);

        assertThat(v.status()).isEqualTo(AuditReportStatus.ISSUED);
        assertThat(v.issuedAt()).isEqualTo(NOW);
        assertThat(v.issuedBy()).isEqualTo(ME);
        assertThat(v.issuedByName()).isEqualTo("Alice Martin");
        assertThat(v.contentSha256()).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(v.summary()).isEqualTo("Synthèse finale");
        assertThat(v.keyFigures().majorNc()).isEqualTo(1);
        assertThat(v.keyFigures().opportunity()).isEqualTo(1);
        assertThat(v.keyFigures().conformity()).isEqualTo(1);
        assertThat(v.keyFigures().total()).isEqualTo(3);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> evt =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(eq(TENANT), evt.capture());
        assertThat(evt.getValue().action()).isEqualTo(AuditReportService.ACTION_ISSUED);
        assertThat(evt.getValue().actorType()).isEqualTo(ActorType.USER);
        assertThat(evt.getValue().actorUserId()).isEqualTo(ME);
        assertThat(evt.getValue().payloadJson()).contains(v.contentSha256()).doesNotContain("Synthèse finale");
    }

    @Test
    void apresEmission_lesChiffresRestentFiges_memeSiUnConstatChange() {
        connecte();
        AuditPlan p = plan(AuditStatus.COMPLETED, FindingType.MINOR_NC);
        p.setReportSummary("S");
        draft(AuditConclusion.FAVORABLE);
        saveEchoes();
        service.issue(PLAN);

        // Un constat ajouté ensuite ne réécrit pas ce qui a été remis.
        AuditFinding tardif = new AuditFinding();
        tardif.setPlan(p);
        tardif.setType(FindingType.MAJOR_NC);
        p.getFindings().add(tardif);
        p.setReportSummary("modifiée après coup");

        AuditReportDto.View relu = service.get(PLAN);
        assertThat(relu.keyFigures().majorNc()).isZero();
        assertThat(relu.keyFigures().minorNc()).isEqualTo(1);
        assertThat(relu.summary()).isEqualTo("S");
    }

    @Test
    void emission_auditNonCloture_refusee() {
        plan(AuditStatus.IN_PROGRESS);
        assertThatThrownBy(() -> service.issue(PLAN)).isInstanceOf(AuditStateException.class);
        verifyNoInteractions(auditEvents);
    }

    @Test
    void emission_sansSynthese_refusee() {
        connecte();
        plan(AuditStatus.COMPLETED);
        draft(AuditConclusion.FAVORABLE);
        assertThatThrownBy(() -> service.issue(PLAN)).isInstanceOf(AuditReportIncompleteException.class);
        verify(reports, never()).save(any());
    }

    @Test
    void emission_sansConclusion_refusee() {
        connecte();
        AuditPlan p = plan(AuditStatus.COMPLETED);
        p.setReportSummary("S");
        draft(null);
        assertThatThrownBy(() -> service.issue(PLAN)).isInstanceOf(AuditReportIncompleteException.class);
    }

    @Test
    void emission_sansBrouillon_etSansConclusion_refusee() {
        AuditPlan p = plan(AuditStatus.COMPLETED);
        p.setReportSummary("S");
        when(reports.findByTenantIdAndPlanId(TENANT, PLAN)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.issue(PLAN)).isInstanceOf(AuditReportIncompleteException.class);
    }

    @Test
    void emission_deuxFois_refusee() {
        plan(AuditStatus.COMPLETED);
        AuditReport r = draft(AuditConclusion.FAVORABLE);
        r.setStatus(AuditReportStatus.ISSUED);
        assertThatThrownBy(() -> service.issue(PLAN)).isInstanceOf(AuditStateException.class);
    }

    @Test
    void emission_sansIdentiteAuthentifiee_refusee() {
        AuditPlan p = plan(AuditStatus.COMPLETED);
        p.setReportSummary("S");
        draft(AuditConclusion.UNFAVORABLE);
        assertThatThrownBy(() -> service.issue(PLAN)).isInstanceOf(MissingTenantContextException.class);
        verify(reports, never()).save(any());
    }

    @Test
    void empreinte_changeAvecLeContenu() {
        AuditPlan p = new AuditPlan();
        p.setId(PLAN);
        p.setReference("AUD-1");
        AuditReport r = new AuditReport();
        r.setConclusion(AuditConclusion.FAVORABLE);
        r.setIssuedSummary("A");
        AuditReportDto.KeyFigures f = new AuditReportDto.KeyFigures(0, 0, 0, 0, 0, 0, null);

        String a = AuditReportService.fingerprint(p, r, f, NOW, ME);
        r.setIssuedSummary("B");
        String b = AuditReportService.fingerprint(p, r, f, NOW, ME);

        assertThat(a).isNotEqualTo(b);
        assertThat(AuditReportService.fingerprint(p, r, f, NOW, ME)).isEqualTo(b);
    }
}
