package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcDto;
import com.openlab.qualitos.quality.nonconformity.NcOrigin;
import com.openlab.qualitos.quality.nonconformity.NcService;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Le constat d'audit qui devient un écart au registre.
 *
 * <p>Ce que ce banc protège, dans l'ordre d'importance.
 *
 * <p><b>Rien n'est recopié.</b> La description, la date de relevé, la clause, la
 * gravité et l'origine viennent du constat et du plan, jamais de l'écran. C'est
 * le seul intérêt du pont : deux versions du même fait ne peuvent plus exister.
 *
 * <p><b>Seul un écart devient un écart.</b> Une observation ou une piste de
 * progrès n'affirme aucun manquement ; les inscrire au registre gonflerait les
 * chiffres de la revue de direction de constats que personne n'a relevés comme
 * tels.
 *
 * <p><b>L'origine se déduit du type d'audit.</b> Un écart relevé par un
 * certificateur n'a ni les mêmes délais ni les mêmes destinataires qu'un écart
 * trouvé par l'équipe, et la revue les compte séparément.
 */
@ExtendWith(MockitoExtension.class)
class AuditFindingNcServiceTest {

    @Mock AuditPlanRepository plans;
    @Mock AuditFindingRepository findings;
    @Mock NcService ncService;
    @InjectMocks AuditFindingNcService service;

    static final UUID TENANT = UUID.randomUUID();
    static final Instant RELEVE = Instant.parse("2026-11-04T08:30:00Z");

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach void clr() { TenantContext.clear(); }

    @Test
    void declareLEcartDuConstat_toutEstDeduitDuConstat_etLeLienEstGarde() {
        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-2026-0003", AuditType.INTERNAL);
        AuditFinding premier = finding(plan, FindingType.OBSERVATION);
        AuditFinding second = finding(plan, FindingType.MAJOR_NC);
        second.setDescription("Les enregistrements de formation ne sont pas signés.");
        second.setClauseRef("7.2");
        second.setPhotoUrl("https://preuves.test/p1.jpg");
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findForUpdateByIdAndPlanId(second.getId(), plan.getId())).thenReturn(Optional.of(second));
        UUID ncId = UUID.randomUUID();
        when(ncService.create(any())).thenReturn(reponse(ncId, "NC-2026-0042"));

        AuditFindingNcService.FindingNc r =
                service.open(plan.getId(), second.getId(), demande());

        ArgumentCaptor<NcDto.CreateRequest> capte = ArgumentCaptor.forClass(NcDto.CreateRequest.class);
        verify(ncService).create(capte.capture());
        NcDto.CreateRequest envoye = capte.getValue();
        assertThat(envoye.title()).isEqualTo("Enregistrements de formation non signés");
        assertThat(envoye.category()).isEqualTo(NcCategory.DOCUMENTATION);
        assertThat(envoye.subcategoryCode()).isEqualTo("DOC-SIGN");
        // La citation de l'audit précède la description du constat : six mois plus
        // tard, le registre dit encore d'où vient l'écart.
        assertThat(envoye.description())
                .isEqualTo("Constat d'audit AUD-2026-0003 / constat 2 (clause 7.2)"
                        + " — Les enregistrements de formation ne sont pas signés.");
        assertThat(envoye.severity()).isEqualTo(NcSeverity.MAJOR);
        assertThat(envoye.detectedAt()).isEqualTo(RELEVE);
        assertThat(envoye.zone()).isEqualTo("Clause 7.2");
        assertThat(envoye.photoUrls()).isEqualTo("https://preuves.test/p1.jpg");
        assertThat(envoye.origin()).isEqualTo(NcOrigin.INTERNAL);

        assertThat(r.ncId()).isEqualTo(ncId);
        assertThat(r.ncReference()).isEqualTo("NC-2026-0042");
        assertThat(r.findingId()).isEqualTo(second.getId());
        assertThat(r.sourceRef()).isEqualTo("AUD-2026-0003 / constat 2");
        assertThat(second.getNcId()).isEqualTo(ncId);
        assertThat(premier.getNcId()).isNull();
        verify(findings).save(second);
    }

    @Test
    void unEcartDUnAuditDeCertificationEstDOrigineExterne() {
        for (AuditType type : List.of(AuditType.CERTIFICATION, AuditType.EXTERNAL,
                AuditType.SURVEILLANCE, AuditType.SUPPLIER)) {
            assertThat(AuditFindingNcService.EXTERNAL_AUDITS).contains(type);
        }
        // Les audits conduits par l'organisation elle-même restent internes.
        assertThat(AuditFindingNcService.EXTERNAL_AUDITS)
                .doesNotContain(AuditType.INTERNAL, AuditType.LPA);

        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-9", AuditType.CERTIFICATION);
        AuditFinding f = finding(plan, FindingType.MINOR_NC);
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findForUpdateByIdAndPlanId(f.getId(), plan.getId())).thenReturn(Optional.of(f));
        when(ncService.create(any())).thenReturn(reponse(UUID.randomUUID(), "NC-1"));

        service.open(plan.getId(), f.getId(), demande());

        ArgumentCaptor<NcDto.CreateRequest> capte = ArgumentCaptor.forClass(NcDto.CreateRequest.class);
        verify(ncService).create(capte.capture());
        assertThat(capte.getValue().origin()).isEqualTo(NcOrigin.EXTERNAL);
    }

    @Test
    void unEcartMineurDevientMineur_etLaCriticiteLaPlusHauteResteUneDecisionHumaine() {
        assertThat(AuditFindingNcService.severityOf(FindingType.MAJOR_NC)).isEqualTo(NcSeverity.MAJOR);
        assertThat(AuditFindingNcService.severityOf(FindingType.MINOR_NC)).isEqualTo(NcSeverity.MINOR);
        // Aucune conversion ne produit CRITICAL : ce niveau engage des délais et
        // des escalades, et il se pose sur la fiche de l'écart.
        for (FindingType type : FindingType.values()) {
            assertThat(AuditFindingNcService.severityOf(type)).isNotEqualTo(NcSeverity.CRITICAL);
        }
    }

    @Test
    void unConstatSansClauseNePorteNiZoneNiMentionDeClause() {
        AuditFinding f = new AuditFinding();
        f.setDescription("Étiquetage absent sur deux bacs.");
        assertThat(AuditFindingNcService.description("AUD-7 / constat 1", f))
                .isEqualTo("Constat d'audit AUD-7 / constat 1 — Étiquetage absent sur deux bacs.");
        f.setClauseRef("   ");
        assertThat(AuditFindingNcService.description("AUD-7 / constat 1", f))
                .doesNotContain("clause");
    }

    @Test
    void seulUnEcartDevientUneNonConformite() {
        for (FindingType type : List.of(FindingType.CONFORMITY, FindingType.OBSERVATION,
                FindingType.OPPORTUNITY)) {
            AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-1", AuditType.INTERNAL);
            AuditFinding f = finding(plan, type);
            when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
            when(findings.findForUpdateByIdAndPlanId(f.getId(), plan.getId())).thenReturn(Optional.of(f));

            assertThatThrownBy(() -> service.open(plan.getId(), f.getId(), demande()))
                    .isInstanceOf(AuditStateException.class)
                    .hasMessageContaining("non-conforming");
        }
        verifyNoInteractions(ncService);
    }

    @Test
    void unConstatDejaInstruitNOuvrePasUnSecondEcart() {
        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-1", AuditType.INTERNAL);
        AuditFinding f = finding(plan, FindingType.MINOR_NC);
        f.setNcId(UUID.randomUUID());
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findForUpdateByIdAndPlanId(f.getId(), plan.getId())).thenReturn(Optional.of(f));

        assertThatThrownBy(() -> service.open(plan.getId(), f.getId(), demande()))
                .isInstanceOf(AuditStateException.class)
                .hasMessageContaining("already");
        verifyNoInteractions(ncService);
        verify(findings, never()).save(any());
    }

    @Test
    void uneCapaDejaOuverteNEmpechePasDeDeclarerLEcart() {
        // Les deux ne se remplacent pas : la CAPA est le plan d'action, l'écart
        // est le fait constaté. Un rapport d'audit cite le second.
        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-1", AuditType.INTERNAL);
        AuditFinding f = finding(plan, FindingType.MAJOR_NC);
        f.setCapaId(UUID.randomUUID());
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findForUpdateByIdAndPlanId(f.getId(), plan.getId())).thenReturn(Optional.of(f));
        UUID ncId = UUID.randomUUID();
        when(ncService.create(any())).thenReturn(reponse(ncId, "NC-5"));

        assertThat(service.open(plan.getId(), f.getId(), demande()).ncId()).isEqualTo(ncId);
    }

    @Test
    void unAuditAnnuleNeLeveRien() {
        AuditPlan plan = plan(AuditStatus.CANCELLED, "AUD-1", AuditType.INTERNAL);
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.open(plan.getId(), UUID.randomUUID(), demande()))
                .isInstanceOf(AuditStateException.class);
        verifyNoInteractions(findings, ncService);
    }

    @Test
    void unAuditOuUnConstatIntrouvableRend404() {
        UUID planId = UUID.randomUUID();
        when(plans.findByIdAndTenantId(planId, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.open(planId, UUID.randomUUID(), demande()))
                .isInstanceOf(AuditPlanNotFoundException.class);

        AuditPlan plan = plan(AuditStatus.IN_PROGRESS, "AUD-1", AuditType.INTERNAL);
        UUID findingId = UUID.randomUUID();
        when(plans.findByIdAndTenantId(plan.getId(), TENANT)).thenReturn(Optional.of(plan));
        when(findings.findForUpdateByIdAndPlanId(findingId, plan.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.open(plan.getId(), findingId, demande()))
                .isInstanceOf(AuditFindingNotFoundException.class);
        verifyNoInteractions(ncService);
    }

    @Test
    void sansTenantRienNeSeLit() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.open(UUID.randomUUID(), UUID.randomUUID(), demande()))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(plans, findings, ncService);
    }

    private static AuditDto.DeclareNcRequest demande() {
        return new AuditDto.DeclareNcRequest(
                "Enregistrements de formation non signés", NcCategory.DOCUMENTATION, "DOC-SIGN");
    }

    private static NcDto.Response reponse(UUID id, String reference) {
        return new NcDto.Response(id, TENANT, reference, "t", "d", NcCategory.DOCUMENTATION,
                null, NcSeverity.MAJOR, NcStatus.OPEN, NcOrigin.INTERNAL, RELEVE, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                RELEVE, RELEVE);
    }

    private static AuditPlan plan(AuditStatus status, String reference, AuditType type) {
        AuditPlan p = new AuditPlan();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setTitle("Audit interne");
        p.setReference(reference);
        p.setStatus(status);
        p.setType(type);
        return p;
    }

    private static AuditFinding finding(AuditPlan plan, FindingType type) {
        AuditFinding f = new AuditFinding();
        f.setId(UUID.randomUUID());
        f.setPlan(plan);
        f.setType(type);
        f.setDescription("Écart relevé.");
        f.setRaisedAt(RELEVE);
        plan.getFindings().add(f);
        return f;
    }
}
