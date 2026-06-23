package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditPlanRepository planRepo;
    @Mock AuditChecklistItemRepository checklistRepo;
    @Mock AuditFindingRepository findingRepo;
    @Mock AiGatewayClient ai;
    @InjectMocks AuditService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID AUDITEE = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- create ---
    @Test
    void createPlan_success() {
        AuditDto.CreatePlanRequest req = new AuditDto.CreatePlanRequest(
                "Audit ISO 9001", "scope", AuditType.INTERNAL, "ISO_9001",
                LEAD, AUDITEE, LocalDate.now().plusDays(15));
        when(planRepo.save(any())).thenAnswer(inv -> {
            AuditPlan p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        AuditDto.PlanResponse r = service.createPlan(req);
        assertThat(r.status()).isEqualTo(AuditStatus.PLANNED);
        assertThat(r.standard()).isEqualTo("ISO_9001");
    }

    @Test
    void createPlan_missingTenant_throws() {
        TenantContext.clear();
        AuditDto.CreatePlanRequest req = new AuditDto.CreatePlanRequest(
                "X", null, AuditType.INTERNAL, null, LEAD, null, null);
        assertThatThrownBy(() -> service.createPlan(req))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // --- findAll ---
    @Test
    void findAll_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(planRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(plan(TENANT, AuditStatus.PLANNED))));
        Page<AuditDto.PlanResponse> r = service.findAll(null, null, p);
        assertThat(r.getContent()).hasSize(1);
        verify(planRepo, never()).findByTenantIdAndStatus(any(), any(), any());
        verify(planRepo, never()).findByTenantIdAndType(any(), any(), any());
    }

    @Test
    void findAll_byStatus() {
        Pageable p = PageRequest.of(0, 10);
        when(planRepo.findByTenantIdAndStatus(TENANT, AuditStatus.COMPLETED, p))
                .thenReturn(new PageImpl<>(List.of(plan(TENANT, AuditStatus.COMPLETED))));
        Page<AuditDto.PlanResponse> r = service.findAll(AuditStatus.COMPLETED, null, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(AuditStatus.COMPLETED);
    }

    @Test
    void findAll_byType() {
        Pageable p = PageRequest.of(0, 10);
        when(planRepo.findByTenantIdAndType(TENANT, AuditType.SUPPLIER, p))
                .thenReturn(new PageImpl<>(List.of(plan(TENANT, AuditStatus.PLANNED))));
        Page<AuditDto.PlanResponse> r = service.findAll(null, AuditType.SUPPLIER, p);
        assertThat(r.getContent()).hasSize(1);
    }

    @Test
    void findAll_statusTakesPriorityOverType() {
        Pageable p = PageRequest.of(0, 10);
        when(planRepo.findByTenantIdAndStatus(TENANT, AuditStatus.PLANNED, p))
                .thenReturn(new PageImpl<>(List.of(plan(TENANT, AuditStatus.PLANNED))));
        service.findAll(AuditStatus.PLANNED, AuditType.LPA, p);
        verify(planRepo).findByTenantIdAndStatus(TENANT, AuditStatus.PLANNED, p);
        verify(planRepo, never()).findByTenantIdAndType(any(), any(), any());
    }

    // --- findById ---
    @Test
    void findById_found() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThat(service.findById(p.getId()).id()).isEqualTo(p.getId());
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(planRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(AuditPlanNotFoundException.class);
    }

    // --- update ---
    @Test
    void updatePlan_success() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(planRepo.save(p)).thenReturn(p);
        UUID newLead = UUID.randomUUID();
        LocalDate when = LocalDate.now().plusDays(7);
        service.updatePlan(p.getId(),
                new AuditDto.UpdatePlanRequest("T2", "s2", AuditType.LPA, "IATF_16949",
                        newLead, AUDITEE, when));
        assertThat(p.getTitle()).isEqualTo("T2");
        assertThat(p.getType()).isEqualTo(AuditType.LPA);
        assertThat(p.getLeadAuditorId()).isEqualTo(newLead);
        assertThat(p.getScheduledDate()).isEqualTo(when);
    }

    @Test
    void updatePlan_completed_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.COMPLETED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.updatePlan(p.getId(),
                new AuditDto.UpdatePlanRequest("X", null, null, null, null, null, null)))
                .isInstanceOf(AuditStateException.class);
    }

    // --- start ---
    @Test
    void startPlan_success() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        p.getChecklist().add(item(p, 0));
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(planRepo.save(p)).thenReturn(p);
        service.startPlan(p.getId());
        assertThat(p.getStatus()).isEqualTo(AuditStatus.IN_PROGRESS);
        assertThat(p.getStartedAt()).isNotNull();
    }

    @Test
    void startPlan_noChecklist_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.startPlan(p.getId()))
                .isInstanceOf(AuditStateException.class)
                .hasMessageContaining("checklist");
    }

    @Test
    void startPlan_notPlanned_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.startPlan(p.getId()))
                .isInstanceOf(AuditStateException.class);
    }

    // --- complete ---
    @Test
    void completePlan_success() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditChecklistItem it = item(p, 0);
        it.setConformant(true);
        p.getChecklist().add(it);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(planRepo.save(p)).thenReturn(p);
        service.completePlan(p.getId(), new AuditDto.CompleteRequest("summary"));
        assertThat(p.getStatus()).isEqualTo(AuditStatus.COMPLETED);
        assertThat(p.getCompletedAt()).isNotNull();
        assertThat(p.getReportSummary()).isEqualTo("summary");
    }

    @Test
    void completePlan_nullRequest_works() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditChecklistItem it = item(p, 0);
        it.setConformant(false);
        p.getChecklist().add(it);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(planRepo.save(p)).thenReturn(p);
        service.completePlan(p.getId(), null);
        assertThat(p.getStatus()).isEqualTo(AuditStatus.COMPLETED);
        assertThat(p.getReportSummary()).isNull();
    }

    @Test
    void completePlan_someUnanswered_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditChecklistItem unanswered = item(p, 0);
        p.getChecklist().add(unanswered);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.completePlan(p.getId(), null))
                .isInstanceOf(AuditStateException.class)
                .hasMessageContaining("answered");
    }

    @Test
    void completePlan_notInProgress_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.completePlan(p.getId(), null))
                .isInstanceOf(AuditStateException.class);
    }

    // --- cancel ---
    @Test
    void cancelPlan_success() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(planRepo.save(p)).thenReturn(p);
        service.cancelPlan(p.getId());
        assertThat(p.getStatus()).isEqualTo(AuditStatus.CANCELLED);
    }

    @Test
    void cancelPlan_completed_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.COMPLETED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.cancelPlan(p.getId()))
                .isInstanceOf(AuditStateException.class);
    }

    @Test
    void cancelPlan_alreadyCancelled_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.CANCELLED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.cancelPlan(p.getId()))
                .isInstanceOf(AuditStateException.class);
    }

    // --- delete ---
    @Test
    void deletePlan_success() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        service.deletePlan(p.getId());
        verify(planRepo).delete(p);
    }

    @Test
    void deletePlan_completed_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.COMPLETED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.deletePlan(p.getId()))
                .isInstanceOf(AuditStateException.class);
    }

    // --- checklist add ---
    @Test
    void addChecklistItem_success_defaultsWeightAndOrder() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(checklistRepo.save(any())).thenAnswer(inv -> {
            AuditChecklistItem i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });
        AuditDto.ChecklistItemResponse r = service.addChecklistItem(p.getId(),
                new AuditDto.ChecklistItemRequest("Q?", "8.5.1", "evidence", null, null));
        assertThat(r.weight()).isEqualTo(1);
        assertThat(r.orderIndex()).isEqualTo(0);
    }

    @Test
    void addChecklistItem_explicitValues() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(checklistRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AuditDto.ChecklistItemResponse r = service.addChecklistItem(p.getId(),
                new AuditDto.ChecklistItemRequest("Q?", null, null, 3, 5));
        assertThat(r.weight()).isEqualTo(3);
        assertThat(r.orderIndex()).isEqualTo(5);
    }

    @Test
    void addChecklistItem_completedPlan_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.COMPLETED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.addChecklistItem(p.getId(),
                new AuditDto.ChecklistItemRequest("Q", null, null, null, null)))
                .isInstanceOf(AuditStateException.class);
    }

    // --- checklist update ---
    @Test
    void updateChecklistItem_success() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditChecklistItem i = item(p, 0);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(checklistRepo.findByIdAndPlanId(i.getId(), p.getId())).thenReturn(Optional.of(i));
        when(checklistRepo.save(i)).thenReturn(i);
        service.updateChecklistItem(p.getId(), i.getId(),
                new AuditDto.ChecklistItemRequest("Q2", "8.1", "ev2", 5, 7));
        assertThat(i.getQuestion()).isEqualTo("Q2");
        assertThat(i.getWeight()).isEqualTo(5);
        assertThat(i.getOrderIndex()).isEqualTo(7);
    }

    @Test
    void updateChecklistItem_notFound_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        UUID iid = UUID.randomUUID();
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(checklistRepo.findByIdAndPlanId(iid, p.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateChecklistItem(p.getId(), iid,
                new AuditDto.ChecklistItemRequest("Q", null, null, null, null)))
                .isInstanceOf(AuditChecklistItemNotFoundException.class);
    }

    // --- checklist response ---
    @Test
    void respondToChecklistItem_success() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditChecklistItem i = item(p, 0);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(checklistRepo.findByIdAndPlanId(i.getId(), p.getId())).thenReturn(Optional.of(i));
        when(checklistRepo.save(i)).thenReturn(i);
        service.respondToChecklistItem(p.getId(), i.getId(),
                new AuditDto.ChecklistResponseRequest("vu sur site", true));
        assertThat(i.getResponse()).isEqualTo("vu sur site");
        assertThat(i.getConformant()).isTrue();
    }

    @Test
    void respondToChecklistItem_notInProgress_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.respondToChecklistItem(p.getId(), UUID.randomUUID(),
                new AuditDto.ChecklistResponseRequest("x", true)))
                .isInstanceOf(AuditStateException.class);
    }

    // --- checklist delete ---
    @Test
    void deleteChecklistItem_planned_success() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        AuditChecklistItem i = item(p, 0);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(checklistRepo.findByIdAndPlanId(i.getId(), p.getId())).thenReturn(Optional.of(i));
        service.deleteChecklistItem(p.getId(), i.getId());
        verify(checklistRepo).delete(i);
    }

    @Test
    void deleteChecklistItem_inProgress_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.deleteChecklistItem(p.getId(), UUID.randomUUID()))
                .isInstanceOf(AuditStateException.class);
    }

    // --- finding add ---
    @Test
    void addFinding_success_noChecklistLink() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(findingRepo.save(any())).thenAnswer(inv -> {
            AuditFinding f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        AuditDto.FindingResponse r = service.addFinding(p.getId(),
                new AuditDto.FindingRequest(FindingType.MINOR_NC, "desc", "8.5", null,
                        null, null, LEAD));
        assertThat(r.type()).isEqualTo(FindingType.MINOR_NC);
        assertThat(r.checklistItemId()).isNull();
    }

    @Test
    void addFinding_withChecklistLink() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditChecklistItem i = item(p, 0);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(checklistRepo.findByIdAndPlanId(i.getId(), p.getId())).thenReturn(Optional.of(i));
        when(findingRepo.save(any())).thenAnswer(inv -> {
            AuditFinding f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        AuditDto.FindingResponse r = service.addFinding(p.getId(),
                new AuditDto.FindingRequest(FindingType.MAJOR_NC, "d", null, null,
                        i.getId(), null, LEAD));
        assertThat(r.checklistItemId()).isEqualTo(i.getId());
    }

    @Test
    void addFinding_checklistNotFound_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        UUID itemId = UUID.randomUUID();
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(checklistRepo.findByIdAndPlanId(itemId, p.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addFinding(p.getId(),
                new AuditDto.FindingRequest(FindingType.OBSERVATION, "d", null, null,
                        itemId, null, LEAD)))
                .isInstanceOf(AuditChecklistItemNotFoundException.class);
    }

    @Test
    void addFinding_notInProgress_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.addFinding(p.getId(),
                new AuditDto.FindingRequest(FindingType.OBSERVATION, "d", null, null,
                        null, null, LEAD)))
                .isInstanceOf(AuditStateException.class);
    }

    // --- finding update ---
    @Test
    void updateFinding_success() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditFinding f = finding(p, FindingType.OBSERVATION);
        UUID capa = UUID.randomUUID();
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(findingRepo.findByIdAndPlanId(f.getId(), p.getId())).thenReturn(Optional.of(f));
        when(findingRepo.save(f)).thenReturn(f);
        service.updateFinding(p.getId(), f.getId(),
                new AuditDto.UpdateFindingRequest(FindingType.MAJOR_NC, "d2", "8.5", "url", capa));
        assertThat(f.getType()).isEqualTo(FindingType.MAJOR_NC);
        assertThat(f.getDescription()).isEqualTo("d2");
        assertThat(f.getCapaId()).isEqualTo(capa);
    }

    @Test
    void updateFinding_cancelledAudit_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.CANCELLED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.updateFinding(p.getId(), UUID.randomUUID(),
                new AuditDto.UpdateFindingRequest(null, "x", null, null, null)))
                .isInstanceOf(AuditStateException.class);
    }

    @Test
    void updateFinding_notFound_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        UUID fid = UUID.randomUUID();
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(findingRepo.findByIdAndPlanId(fid, p.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateFinding(p.getId(), fid,
                new AuditDto.UpdateFindingRequest(null, "x", null, null, null)))
                .isInstanceOf(AuditFindingNotFoundException.class);
    }

    // --- finding delete ---
    @Test
    void deleteFinding_success() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditFinding f = finding(p, FindingType.MINOR_NC);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(findingRepo.findByIdAndPlanId(f.getId(), p.getId())).thenReturn(Optional.of(f));
        service.deleteFinding(p.getId(), f.getId());
        verify(findingRepo).delete(f);
    }

    @Test
    void deleteFinding_completed_throws() {
        AuditPlan p = plan(TENANT, AuditStatus.COMPLETED);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.deleteFinding(p.getId(), UUID.randomUUID()))
                .isInstanceOf(AuditStateException.class);
    }

    // --- conformity score ---
    @Test
    void conformityScore_emptyChecklist_isNull() {
        AuditPlan p = plan(TENANT, AuditStatus.PLANNED);
        assertThat(AuditService.computeConformityScore(p)).isNull();
    }

    @Test
    void conformityScore_unansweredOnly_isNull() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        p.getChecklist().add(item(p, 0));
        assertThat(AuditService.computeConformityScore(p)).isNull();
    }

    @Test
    void conformityScore_weightedAverage() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditChecklistItem a = item(p, 0); a.setWeight(2); a.setConformant(true);
        AuditChecklistItem b = item(p, 1); b.setWeight(3); b.setConformant(false);
        p.getChecklist().addAll(List.of(a, b));
        // (2*100 + 3*0) / (2+3) = 40
        assertThat(AuditService.computeConformityScore(p)).isEqualTo(40d);
    }

    @Test
    void conformityScore_nullWeight_treatedAsOne() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        AuditChecklistItem a = item(p, 0); a.setWeight(null); a.setConformant(true);
        p.getChecklist().add(a);
        assertThat(AuditService.computeConformityScore(p)).isEqualTo(100d);
    }

    // --- helpers ---
    // --- generateReport (ANO-012, §1.4/§4.4) ---
    @Test
    void generateReport_callsLlmAndPersistsSummary() {
        AuditPlan p = plan(TENANT, AuditStatus.IN_PROGRESS);
        when(planRepo.findByIdAndTenantId(p.getId(), TENANT)).thenReturn(Optional.of(p));
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult("Rapport : 1 NC mineure sur la clause 7.5.", "mistral", 120, 40));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuditDto.PlanResponse r = service.generateReport(p.getId());

        assertThat(r.reportSummary()).contains("NC mineure");
        verify(ai).complete(any(), any(), anyInt());
    }

    @Test
    void generateReport_unknownPlan_throws() {
        UUID id = UUID.randomUUID();
        when(planRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.generateReport(id))
                .isInstanceOf(AuditPlanNotFoundException.class);
        verifyNoInteractions(ai);
    }

    private AuditPlan plan(UUID tenant, AuditStatus status) {
        AuditPlan p = new AuditPlan();
        p.setId(UUID.randomUUID());
        p.setTenantId(tenant);
        p.setTitle("T");
        p.setType(AuditType.INTERNAL);
        p.setStatus(status);
        p.setLeadAuditorId(LEAD);
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private AuditChecklistItem item(AuditPlan p, int order) {
        AuditChecklistItem i = new AuditChecklistItem();
        i.setId(UUID.randomUUID());
        i.setPlan(p);
        i.setQuestion("Q");
        i.setWeight(1);
        i.setOrderIndex(order);
        i.setCreatedAt(Instant.now());
        i.setUpdatedAt(Instant.now());
        return i;
    }

    private AuditFinding finding(AuditPlan p, FindingType type) {
        AuditFinding f = new AuditFinding();
        f.setId(UUID.randomUUID());
        f.setPlan(p);
        f.setType(type);
        f.setDescription("d");
        f.setRaisedBy(LEAD);
        f.setRaisedAt(Instant.now());
        f.setCreatedAt(Instant.now());
        f.setUpdatedAt(Instant.now());
        return f;
    }
}
