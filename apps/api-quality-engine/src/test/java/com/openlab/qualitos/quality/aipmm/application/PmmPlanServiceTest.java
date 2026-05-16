package com.openlab.qualitos.quality.aipmm.application;

import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanNotFoundException;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanRepository;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStateException;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStatus;
import com.openlab.qualitos.quality.aipmm.domain.PmmReviewFrequency;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PmmPlanServiceTest {

    @Mock PmmPlanRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock PmmPlanEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID REVIEWER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();

    PmmPlanService service;

    @BeforeEach
    void setup() {
        service = new PmmPlanService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            PmmPlan p = inv.getArgument(0);
            if (p.getId() == null) p.assignId(ID);
            return p;
        });
    }

    @Test
    void draft_publishes() {
        PmmPlanDto.View v = service.draft(req("REF-1"));
        verify(events).publish(any(), eq(PmmPlanEventPublisher.Action.DRAFTED));
        assertThat(v.status()).isEqualTo(PmmPlanStatus.DRAFT);
    }

    @Test
    void draft_duplicate_throws() {
        when(repo.existsByTenantAndReference(TENANT, "REF-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.draft(req("REF-DUP")))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void draft_missingActor_throws() {
        PmmPlanDto.DraftRequest r = new PmmPlanDto.DraftRequest("REF-1", SYS, "name",
                null, "m", "c", PmmReviewFrequency.MONTHLY, null, null, null, null);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void draft_missingSystem_throws() {
        PmmPlanDto.DraftRequest r = new PmmPlanDto.DraftRequest("REF-1", null, "name",
                null, "m", "c", PmmReviewFrequency.MONTHLY, null, null, null, USER);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void edit_ok() {
        PmmPlan p = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        PmmPlanDto.View v = service.edit(ID, new PmmPlanDto.EditRequest(
                "new", null, "m2", "c2", PmmReviewFrequency.WEEKLY, null, null, null));
        assertThat(v.name()).isEqualTo("new");
        verify(events).publish(any(), eq(PmmPlanEventPublisher.Action.EDITED));
    }

    @Test
    void edit_crossTenant_404() {
        PmmPlan foreign = PmmPlan.draft(OTHER, "REF-1", SYS, "n", null,
                "m", "c", PmmReviewFrequency.MONTHLY, null, null, null, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.edit(ID, new PmmPlanDto.EditRequest(
                "x", null, "m", "c", PmmReviewFrequency.MONTHLY, null, null, null)))
                .isInstanceOf(PmmPlanNotFoundException.class);
    }

    @Test
    void activate_ok() {
        PmmPlan p = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        PmmPlanDto.View v = service.activate(ID);
        assertThat(v.status()).isEqualTo(PmmPlanStatus.ACTIVE);
        verify(events).publish(any(), eq(PmmPlanEventPublisher.Action.ACTIVATED));
    }

    @Test
    void recordReview_ok() {
        PmmPlan p = ready();
        p.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        PmmPlanDto.View v = service.recordReview(ID,
                new PmmPlanDto.ReviewRequest(REVIEWER));
        assertThat(v.lastReviewedByUserId()).isEqualTo(REVIEWER);
        verify(events).publish(any(), eq(PmmPlanEventPublisher.Action.REVIEWED));
    }

    @Test
    void recordReview_missingReviewer_throws() {
        PmmPlan p = ready();
        p.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.recordReview(ID,
                new PmmPlanDto.ReviewRequest(null)))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void suspend_ok() {
        PmmPlan p = ready();
        p.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        PmmPlanDto.View v = service.suspend(ID, new PmmPlanDto.SuspendRequest("maint"));
        assertThat(v.status()).isEqualTo(PmmPlanStatus.SUSPENDED);
        verify(events).publish(any(), eq(PmmPlanEventPublisher.Action.SUSPENDED));
    }

    @Test
    void close_ok() {
        PmmPlan p = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        PmmPlanDto.View v = service.close(ID, new PmmPlanDto.CloseRequest("done"));
        assertThat(v.status()).isEqualTo(PmmPlanStatus.CLOSED);
        verify(events).publish(any(), eq(PmmPlanEventPublisher.Action.CLOSED));
    }

    @Test
    void delete_draftOnly() {
        PmmPlan p = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(PmmPlanEventPublisher.Action.DELETED));
    }

    @Test
    void delete_nonDraft_throws() {
        PmmPlan p = ready();
        p.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(PmmPlanStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_notFound_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(PmmPlanNotFoundException.class);
    }

    @Test
    void get_crossTenant_404() {
        PmmPlan foreign = PmmPlan.draft(OTHER, "REF-1", SYS, "n", null,
                "m", "c", PmmReviewFrequency.MONTHLY, null, null, null, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(PmmPlanNotFoundException.class);
    }

    @Test
    void list_all() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(ready()));
        assertThat(service.list(null)).hasSize(1);
    }

    @Test
    void list_byStatus() {
        when(repo.findByTenantAndStatus(TENANT, PmmPlanStatus.DRAFT))
                .thenReturn(List.of(ready()));
        assertThat(service.list(PmmPlanStatus.DRAFT)).hasSize(1);
    }

    @Test
    void listByAiSystem_ok() {
        when(repo.findByTenantAndAiSystemId(TENANT, SYS))
                .thenReturn(List.of(ready()));
        assertThat(service.listByAiSystem(SYS)).hasSize(1);
    }

    @Test
    void listByAiSystem_null_throws() {
        assertThatThrownBy(() -> service.listByAiSystem(null))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void listOverdueReviews_ok() {
        when(repo.findOverdueReviews(eq(TENANT), any(), anyInt()))
                .thenReturn(List.of(ready()));
        assertThat(service.listOverdueReviews(100)).hasSize(1);
    }

    @Test
    void listOverdueReviews_outOfRange_throws() {
        assertThatThrownBy(() -> service.listOverdueReviews(0))
                .isInstanceOf(PmmPlanStateException.class);
        assertThatThrownBy(() -> service.listOverdueReviews(1001))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void getByReference_notFound_throws() {
        when(repo.findByTenantAndReference(TENANT, "REF-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("REF-X"))
                .isInstanceOf(PmmPlanNotFoundException.class);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "REF-1"))
                .thenReturn(Optional.of(ready()));
        assertThat(service.getByReference("REF-1").reference()).isEqualTo("REF-1");
    }

    @Test
    void constructor_withoutEvents_usesNoOp() {
        PmmPlanService s2 = new PmmPlanService(repo, tenantProvider, CLOCK);
        s2.draft(req("REF-N"));
    }

    private PmmPlanDto.DraftRequest req(String ref) {
        return new PmmPlanDto.DraftRequest(ref, SYS, "Name", "desc",
                "metrics", "method", PmmReviewFrequency.MONTHLY,
                "resp", "trigger", "qms-1", USER);
    }

    private PmmPlan ready() {
        PmmPlan p = PmmPlan.draft(TENANT, "REF-1", SYS, "Name", "desc",
                "metrics", "method", PmmReviewFrequency.MONTHLY,
                "resp", "trigger", "qms-1", USER, NOW);
        p.assignId(ID);
        return p;
    }
}
