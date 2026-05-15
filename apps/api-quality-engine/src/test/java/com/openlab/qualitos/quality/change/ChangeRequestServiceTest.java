package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceTest {

    @Mock ChangeRequestRepository requestRepo;
    @Mock ChangeImpactRepository impactRepo;
    @Mock ChangeApprovalRepository approvalRepo;
    ChangeRequestService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID APPROVER_A = UUID.randomUUID();
    static final UUID APPROVER_B = UUID.randomUUID();
    static final UUID CHG = UUID.randomUUID();
    static final UUID IMP = UUID.randomUUID();
    static final UUID TARGET = UUID.randomUUID();
    static final Clock CLOCK = Clock.fixed(
            LocalDate.parse("2026-05-15").atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new ChangeRequestService(requestRepo, impactRepo, approvalRepo, CLOCK);
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    // ---- CRUD ----

    @Test
    void create_defaultsPriorityMedium() {
        when(requestRepo.findByTenantIdAndCode(TENANT, "CR-1")).thenReturn(Optional.empty());
        when(requestRepo.save(any())).thenAnswer(inv -> {
            ChangeRequest c = inv.getArgument(0);
            c.setId(CHG); c.setCreatedAt(Instant.now()); c.setUpdatedAt(Instant.now());
            return c;
        });
        ChangeDto.ChangeResponse out = service.create(new ChangeDto.CreateChangeRequest(
                "CR-1", "Update QMS", null, ChangeRequestType.DOCUMENT, null,
                USER, null, null, null, null));
        assertThat(out.priority()).isEqualTo(ChangeRequestPriority.MEDIUM);
        assertThat(out.status()).isEqualTo(ChangeRequestStatus.DRAFT);
    }

    @Test
    void create_duplicateCode_throws() {
        when(requestRepo.findByTenantIdAndCode(TENANT, "dup")).thenReturn(Optional.of(change()));
        assertThatThrownBy(() -> service.create(new ChangeDto.CreateChangeRequest(
                "dup", "t", null, ChangeRequestType.PROCESS, null, USER, null, null, null, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void create_noTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.create(new ChangeDto.CreateChangeRequest(
                "CR-9", "x", null, ChangeRequestType.PROCESS, null, USER, null, null, null, null)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void update_terminalStatus_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.IMPLEMENTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.update(CHG, new ChangeDto.UpdateChangeRequest(
                "x", null, null, null, null, null, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void update_appliesPatches() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.update(CHG, new ChangeDto.UpdateChangeRequest(
                "Renamed", "new desc", ChangeRequestPriority.HIGH,
                APPROVER_A, LocalDate.parse("2026-06-01"), "impact", "risk"));
        assertThat(c.getTitle()).isEqualTo("Renamed");
        assertThat(c.getPriority()).isEqualTo(ChangeRequestPriority.HIGH);
        assertThat(c.getOwnerUserId()).isEqualTo(APPROVER_A);
    }

    @Test
    void delete_inProgress_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.UNDER_REVIEW);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.delete(CHG))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void delete_draft_cascades() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        service.delete(CHG);
        verify(impactRepo).deleteByChangeId(CHG);
        verify(approvalRepo).deleteByChangeId(CHG);
        verify(requestRepo).delete(c);
    }

    @Test
    void delete_cancelled_allowed() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.CANCELLED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        service.delete(CHG);
        verify(requestRepo).delete(c);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        ChangeRequest c = change(); c.setTenantId(UUID.randomUUID());
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.get(CHG))
                .isInstanceOf(ChangeRequestNotFoundException.class);
    }

    @Test
    void list_filterPaths() {
        when(requestRepo.findByTenantIdAndStatus(eq(TENANT), eq(ChangeRequestStatus.DRAFT), any()))
                .thenReturn(new PageImpl<>(List.of(change())));
        assertThat(service.list(ChangeRequestStatus.DRAFT, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(requestRepo.findByTenantIdAndType(eq(TENANT), eq(ChangeRequestType.SUPPLIER), any()))
                .thenReturn(new PageImpl<>(List.of(change())));
        assertThat(service.list(null, ChangeRequestType.SUPPLIER, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
        when(requestRepo.findByTenantId(eq(TENANT), any()))
                .thenReturn(new PageImpl<>(List.of(change())));
        assertThat(service.list(null, null, PageRequest.of(0, 10))
                .getTotalElements()).isOne();
    }

    // ---- Workflow ----

    @Test
    void submit_withoutApprovers_rejected() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(approvalRepo.countByChangeId(CHG)).thenReturn(0L);
        assertThatThrownBy(() -> service.submit(CHG))
                .isInstanceOf(ChangeStateException.class)
                .hasMessageContaining("approver");
    }

    @Test
    void submit_withApprovers_ok() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(approvalRepo.countByChangeId(CHG)).thenReturn(2L);
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.submit(CHG).status()).isEqualTo(ChangeRequestStatus.SUBMITTED);
    }

    @Test
    void submit_nonDraft_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.SUBMITTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.submit(CHG))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void cancel_active_ok() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.UNDER_REVIEW);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ChangeDto.ChangeResponse out = service.cancel(CHG, "duplicate of CR-99");
        assertThat(out.status()).isEqualTo(ChangeRequestStatus.CANCELLED);
        assertThat(out.rejectionReason()).isEqualTo("duplicate of CR-99");
    }

    @Test
    void cancel_terminal_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.IMPLEMENTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.cancel(CHG, null))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void implement_fromApproved_ok() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.APPROVED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LocalDate when = LocalDate.parse("2026-06-15");
        ChangeDto.ChangeResponse out = service.implement(CHG, new ChangeDto.ImplementRequest(when));
        assertThat(out.status()).isEqualTo(ChangeRequestStatus.IMPLEMENTED);
        assertThat(out.implementedAt()).isEqualTo(when);
    }

    @Test
    void implement_nonApproved_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.SUBMITTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.implement(CHG,
                new ChangeDto.ImplementRequest(LocalDate.now(CLOCK))))
                .isInstanceOf(ChangeStateException.class);
    }

    // ---- Approvers ----

    @Test
    void addApprover_inDraft_ok() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_A))
                .thenReturn(Optional.empty());
        when(approvalRepo.save(any())).thenAnswer(inv -> {
            ChangeApproval a = inv.getArgument(0);
            a.setId(UUID.randomUUID()); a.setCreatedAt(Instant.now()); return a;
        });
        ChangeDto.ApprovalResponse out = service.addApprover(CHG,
                new ChangeDto.AddApproverRequest(APPROVER_A, 2));
        assertThat(out.approvalLevel()).isEqualTo(2);
        assertThat(out.decision()).isEqualTo(ApprovalDecision.PENDING);
    }

    @Test
    void addApprover_duplicate_rejected() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_A))
                .thenReturn(Optional.of(new ChangeApproval()));
        assertThatThrownBy(() -> service.addApprover(CHG,
                new ChangeDto.AddApproverRequest(APPROVER_A, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void addApprover_nonDraft_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.SUBMITTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addApprover(CHG,
                new ChangeDto.AddApproverRequest(APPROVER_A, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void removeApprover_inDraft_ok() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        ChangeApproval a = approval(APPROVER_A, ApprovalDecision.PENDING);
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_A))
                .thenReturn(Optional.of(a));
        service.removeApprover(CHG, APPROVER_A);
        verify(approvalRepo).delete(a);
    }

    @Test
    void removeApprover_missing_throws() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_A))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeApprover(CHG, APPROVER_A))
                .isInstanceOf(ChangeChildNotFoundException.class);
    }

    // ---- Decisions ----

    @Test
    void decide_singleApproved_movesToApproved() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.SUBMITTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        ChangeApproval a = approval(APPROVER_A, ApprovalDecision.PENDING);
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_A))
                .thenReturn(Optional.of(a));
        when(approvalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepo.countByChangeId(CHG)).thenReturn(1L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.APPROVED)).thenReturn(1L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.REJECTED)).thenReturn(0L);
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.decide(CHG, new ChangeDto.DecisionRequest(
                APPROVER_A, ApprovalDecision.APPROVED, "ok"));
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.APPROVED);
    }

    @Test
    void decide_partialApproval_movesToUnderReview() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.SUBMITTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        ChangeApproval a = approval(APPROVER_A, ApprovalDecision.PENDING);
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_A))
                .thenReturn(Optional.of(a));
        when(approvalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepo.countByChangeId(CHG)).thenReturn(2L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.APPROVED)).thenReturn(1L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.REJECTED)).thenReturn(0L);
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.decide(CHG, new ChangeDto.DecisionRequest(
                APPROVER_A, ApprovalDecision.APPROVED, null));
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.UNDER_REVIEW);
    }

    @Test
    void decide_anyRejection_movesToRejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.UNDER_REVIEW);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        ChangeApproval a = approval(APPROVER_A, ApprovalDecision.PENDING);
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_A))
                .thenReturn(Optional.of(a));
        when(approvalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(approvalRepo.countByChangeId(CHG)).thenReturn(3L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.APPROVED)).thenReturn(2L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.REJECTED)).thenReturn(1L);
        when(requestRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.decide(CHG, new ChangeDto.DecisionRequest(
                APPROVER_A, ApprovalDecision.REJECTED, "compliance issue"));
        assertThat(c.getStatus()).isEqualTo(ChangeRequestStatus.REJECTED);
        assertThat(c.getRejectionReason()).isEqualTo("compliance issue");
    }

    @Test
    void decide_nonReviewableStatus_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.DRAFT);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.decide(CHG, new ChangeDto.DecisionRequest(
                APPROVER_A, ApprovalDecision.APPROVED, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void decide_pendingValue_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.SUBMITTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.decide(CHG, new ChangeDto.DecisionRequest(
                APPROVER_A, ApprovalDecision.PENDING, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void decide_alreadyDecided_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.UNDER_REVIEW);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        ChangeApproval a = approval(APPROVER_A, ApprovalDecision.APPROVED);
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_A))
                .thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.decide(CHG, new ChangeDto.DecisionRequest(
                APPROVER_A, ApprovalDecision.REJECTED, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void decide_unknownApprover_throws() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.SUBMITTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(approvalRepo.findByChangeIdAndApproverUserId(CHG, APPROVER_B))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.decide(CHG, new ChangeDto.DecisionRequest(
                APPROVER_B, ApprovalDecision.APPROVED, null)))
                .isInstanceOf(ChangeChildNotFoundException.class);
    }

    // ---- Impacts ----

    @Test
    void addImpact_persistsUnique() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(impactRepo.findByChangeIdAndTargetTypeAndTargetId(
                CHG, ChangeImpactTargetType.DOCUMENT, TARGET)).thenReturn(Optional.empty());
        when(impactRepo.save(any())).thenAnswer(inv -> {
            ChangeImpact i = inv.getArgument(0);
            i.setId(IMP); i.setCreatedAt(Instant.now()); return i;
        });
        ChangeDto.ImpactResponse out = service.addImpact(CHG, new ChangeDto.AddImpactRequest(
                ChangeImpactTargetType.DOCUMENT, TARGET, "QMS doc rev"));
        assertThat(out.targetType()).isEqualTo(ChangeImpactTargetType.DOCUMENT);
        assertThat(out.notes()).isEqualTo("QMS doc rev");
    }

    @Test
    void addImpact_duplicate_rejected() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(impactRepo.findByChangeIdAndTargetTypeAndTargetId(
                CHG, ChangeImpactTargetType.DOCUMENT, TARGET))
                .thenReturn(Optional.of(new ChangeImpact()));
        assertThatThrownBy(() -> service.addImpact(CHG, new ChangeDto.AddImpactRequest(
                ChangeImpactTargetType.DOCUMENT, TARGET, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void addImpact_terminal_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.IMPLEMENTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addImpact(CHG, new ChangeDto.AddImpactRequest(
                ChangeImpactTargetType.DOCUMENT, TARGET, null)))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void removeImpact_crossChange_appearsNotFound() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        ChangeImpact i = new ChangeImpact();
        i.setId(IMP); i.setChangeId(UUID.randomUUID()); i.setTenantId(TENANT);
        when(impactRepo.findById(IMP)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.removeImpact(CHG, IMP))
                .isInstanceOf(ChangeChildNotFoundException.class);
    }

    @Test
    void removeImpact_terminal_rejected() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.REJECTED);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.removeImpact(CHG, IMP))
                .isInstanceOf(ChangeStateException.class);
    }

    @Test
    void listImpacts_returnsAll() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        ChangeImpact i = new ChangeImpact();
        i.setId(IMP); i.setChangeId(CHG); i.setTenantId(TENANT);
        i.setTargetType(ChangeImpactTargetType.SUPPLIER); i.setTargetId(TARGET);
        i.setCreatedAt(Instant.now());
        when(impactRepo.findByChangeId(CHG)).thenReturn(List.of(i));
        assertThat(service.listImpacts(CHG)).hasSize(1);
    }

    // ---- Summary ----

    @Test
    void summary_aggregatesCounts() {
        ChangeRequest c = change(); c.setStatus(ChangeRequestStatus.UNDER_REVIEW);
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(approvalRepo.countByChangeId(CHG)).thenReturn(3L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.APPROVED)).thenReturn(1L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.REJECTED)).thenReturn(0L);
        when(approvalRepo.countByChangeIdAndDecision(CHG, ApprovalDecision.PENDING)).thenReturn(2L);
        when(approvalRepo.findByChangeIdOrderByApprovalLevelAsc(CHG)).thenReturn(List.of());
        when(impactRepo.findByChangeId(CHG)).thenReturn(List.of());
        ChangeDto.ChangeSummary s = service.summary(CHG);
        assertThat(s.totalApprovers()).isEqualTo(3L);
        assertThat(s.approved()).isOne();
        assertThat(s.pending()).isEqualTo(2L);
    }

    @Test
    void listApprovals_returnsOrdered() {
        ChangeRequest c = change();
        when(requestRepo.findById(CHG)).thenReturn(Optional.of(c));
        when(approvalRepo.findByChangeIdOrderByApprovalLevelAsc(CHG))
                .thenReturn(List.of(approval(APPROVER_A, ApprovalDecision.PENDING)));
        assertThat(service.listApprovals(CHG)).hasSize(1);
    }

    // ---- helpers ----

    private ChangeRequest change() {
        ChangeRequest c = new ChangeRequest();
        c.setId(CHG); c.setTenantId(TENANT);
        c.setCode("CR-1"); c.setTitle("Update QMS");
        c.setType(ChangeRequestType.DOCUMENT);
        c.setPriority(ChangeRequestPriority.MEDIUM);
        c.setStatus(ChangeRequestStatus.DRAFT);
        c.setRequesterUserId(USER);
        c.setCreatedAt(Instant.now()); c.setUpdatedAt(Instant.now());
        return c;
    }

    private ChangeApproval approval(UUID approverUserId, ApprovalDecision decision) {
        ChangeApproval a = new ChangeApproval();
        a.setId(UUID.randomUUID()); a.setTenantId(TENANT);
        a.setChangeId(CHG); a.setApproverUserId(approverUserId);
        a.setApprovalLevel(1); a.setDecision(decision);
        a.setCreatedAt(Instant.now());
        return a;
    }
}
