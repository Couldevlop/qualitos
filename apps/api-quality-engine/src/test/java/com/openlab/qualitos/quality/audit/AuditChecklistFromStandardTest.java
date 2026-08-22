package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.Standard;
import com.openlab.qualitos.quality.standards.StandardClause;
import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.StandardRepository;
import com.openlab.qualitos.quality.standards.StandardRequirement;
import com.openlab.qualitos.quality.standards.StandardSection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La checklist d'un audit se tire des exigences d'un référentiel — la procédure
 * interne du tenant comme une norme livrée.
 *
 * <p>LA PHOTO : les items créés sont des lignes AUTONOMES, et non des renvois
 * vers les exigences. Une clause corrigée en mars ne doit pas réécrire le rapport
 * de janvier ; c'est cette photo qu'un auditeur externe relira.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditChecklistFromStandardTest {

    @Mock AuditPlanRepository plans;
    @Mock AuditChecklistItemRepository checklist;
    @Mock AuditFindingRepository findings;
    @Mock StandardRepository standards;
    @Mock AiGatewayClient ai;

    static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-14T10:00:00Z"), ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();

    AuditService service;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT.toString());
        service = new AuditService(plans, checklist, findings, standards, ai, FIXED_CLOCK);
        when(checklist.save(any(AuditChecklistItem.class))).thenAnswer(i -> {
            AuditChecklistItem item = i.getArgument(0);
            if (item.getId() == null) item.setId(UUID.randomUUID());
            return item;
        });
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private AuditPlan plannedPlan() {
        AuditPlan p = new AuditPlan();
        p.setId(UUID.randomUUID());
        p.setTenantId(TENANT);
        p.setReference("AUD-2026-0001");
        p.setTitle("Audit de la procédure d'audit interne");
        p.setType(AuditType.INTERNAL);
        p.setStatus(AuditStatus.PLANNED);
        p.setLeadAuditorId(UUID.randomUUID());
        when(plans.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        return p;
    }

    private Standard standardWith(StandardRequirement... requirements) {
        Standard s = new Standard();
        s.setId(UUID.randomUUID());
        s.setCode("PRO-002");
        StandardSection section = new StandardSection();
        section.setCode("1");
        section.setStandard(s);
        StandardClause clause = new StandardClause();
        clause.setCode("1.1");
        clause.setSection(section);
        clause.getRequirements().addAll(List.of(requirements));
        for (StandardRequirement r : requirements) {
            r.setClause(clause);
        }
        section.getClauses().add(clause);
        s.getSections().add(section);
        when(standards.findVisibleById(s.getId(), TENANT)).thenReturn(Optional.of(s));
        return s;
    }

    private StandardRequirement requirement(String code, String text, ObligationLevel obligation) {
        StandardRequirement r = new StandardRequirement();
        r.setId(UUID.randomUUID());
        r.setCode(code);
        r.setText(text);
        r.setObligation(obligation);
        r.setEvidenceTypes("programme d'audit signé");
        return r;
    }

    @Test
    void copiesEveryRequirementIntoTheChecklist() {
        AuditPlan plan = plannedPlan();
        Standard s = standardWith(
                requirement("1.1.1", "Le programme d'audit est revu chaque année", ObligationLevel.MUST),
                requirement("1.1.2", "Les auditeurs sont formés", ObligationLevel.SHOULD));

        List<AuditDto.ChecklistItemResponse> items =
                service.generateChecklistFromStandard(plan.getId(), s.getId());

        assertThat(items).hasSize(2);
        assertThat(items).extracting(AuditDto.ChecklistItemResponse::question)
                .containsExactly("Le programme d'audit est revu chaque année",
                        "Les auditeurs sont formés");
        // Et non « 1.1.1.1.1.1 » : les codes en cascade ne se répètent pas, sans
        // quoi la référence ne se rapprocherait d'aucun texte de la procédure.
        assertThat(items).extracting(AuditDto.ChecklistItemResponse::clauseRef)
                .containsExactly("1.1.1", "1.1.2");
        assertThat(items).extracting(AuditDto.ChecklistItemResponse::orderIndex)
                .containsExactly(0, 1);
        assertThat(items).extracting(AuditDto.ChecklistItemResponse::expectedEvidence)
                .containsOnly("programme d'audit signé");
    }

    /**
     * L'autre convention de numérotation : des codes LOCAUX, qui ne répètent pas
     * celui du parent. La référence se compose alors, au lieu de se contenter du
     * code de l'exigence — « 2 » seul ne désignerait rien dans un rapport.
     */
    @Test
    void composesTheReferenceWhenCodesAreNumberedLocally() {
        AuditPlan plan = plannedPlan();
        Standard s = new Standard();
        s.setId(UUID.randomUUID());
        StandardSection section = new StandardSection();
        section.setCode("1");
        section.setStandard(s);
        StandardClause clause = new StandardClause();
        clause.setCode("1.1");
        clause.setSection(section);
        StandardRequirement r = requirement("2", "Deuxième exigence", ObligationLevel.MUST);
        r.setClause(clause);
        clause.getRequirements().add(r);
        section.getClauses().add(clause);
        s.getSections().add(section);
        when(standards.findVisibleById(s.getId(), TENANT)).thenReturn(Optional.of(s));

        List<AuditDto.ChecklistItemResponse> items =
                service.generateChecklistFromStandard(plan.getId(), s.getId());

        assertThat(items).extracting(AuditDto.ChecklistItemResponse::clauseRef)
                .containsExactly("1.1.2");
    }

    /**
     * Une exigence obligatoire pèse plus qu'une recommandation dans le score de
     * conformité, que le poids sert de diviseur.
     */
    @Test
    void weighsAnObligationMoreThanARecommendation() {
        AuditPlan plan = plannedPlan();
        Standard s = standardWith(
                requirement("1.1.1", "Obligatoire", ObligationLevel.MUST),
                requirement("1.1.2", "Recommandé", ObligationLevel.SHOULD),
                requirement("1.1.3", "Informatif", ObligationLevel.MAY));

        List<AuditDto.ChecklistItemResponse> items =
                service.generateChecklistFromStandard(plan.getId(), s.getId());

        assertThat(items).extracting(AuditDto.ChecklistItemResponse::weight)
                .containsExactly(1, 2, 3);
    }

    /** L'audit retient le référentiel qu'il vise : l'écran doit pouvoir le citer. */
    @Test
    void remembersWhichReferentialTheAuditTargets() {
        AuditPlan plan = plannedPlan();
        Standard s = standardWith(requirement("1.1.1", "Texte", ObligationLevel.MUST));

        service.generateChecklistFromStandard(plan.getId(), s.getId());

        assertThat(plan.getStandardId()).isEqualTo(s.getId());
        verify(plans).save(plan);
    }

    @Test
    void refusesToGenerateOverAnExistingChecklist() {
        // Deux jeux de questions mêlés, et plus personne ne sait lequel fait foi.
        AuditPlan plan = plannedPlan();
        plan.getChecklist().add(new AuditChecklistItem());

        assertThatThrownBy(() -> service.generateChecklistFromStandard(plan.getId(), UUID.randomUUID()))
                .isInstanceOf(AuditStateException.class);

        verify(checklist, never()).save(any());
    }

    @Test
    void refusesOnAnAuditThatIsNoLongerPlanned() {
        AuditPlan plan = plannedPlan();
        plan.setStatus(AuditStatus.IN_PROGRESS);

        assertThatThrownBy(() -> service.generateChecklistFromStandard(plan.getId(), UUID.randomUUID()))
                .isInstanceOf(AuditStateException.class);

        verify(checklist, never()).save(any());
    }

    /**
     * 404 sur le référentiel d'un autre tenant : {@code findVisibleById} ne le rend
     * pas, et l'audit ne doit surtout pas se tirer des exigences du voisin.
     */
    @Test
    void refusesAReferentialTheTenantCannotSee() {
        AuditPlan plan = plannedPlan();
        UUID other = UUID.randomUUID();
        when(standards.findVisibleById(other, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateChecklistFromStandard(plan.getId(), other))
                .isInstanceOf(StandardNotFoundException.class);

        verify(checklist, never()).save(any());
    }

    /**
     * Un référentiel encore vide ne produit aucune question — et ce n'est pas une
     * erreur : c'est l'état normal d'un référentiel qu'on vient de créer.
     */
    @Test
    void generatesNothingFromAnEmptyReferential() {
        AuditPlan plan = plannedPlan();
        Standard empty = new Standard();
        empty.setId(UUID.randomUUID());
        when(standards.findVisibleById(empty.getId(), TENANT)).thenReturn(Optional.of(empty));

        assertThat(service.generateChecklistFromStandard(plan.getId(), empty.getId())).isEmpty();

        verify(checklist, never()).save(any());
        assertThat(plan.getStandardId()).isEqualTo(empty.getId());
    }

    @Test
    void refusesAnAuditOfAnotherTenant() {
        UUID ghost = UUID.randomUUID();
        when(plans.findByIdAndTenantId(ghost, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateChecklistFromStandard(ghost, UUID.randomUUID()))
                .isInstanceOf(AuditPlanNotFoundException.class);
    }
}
