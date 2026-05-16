package com.openlab.qualitos.quality.aiactfria.application;

import com.openlab.qualitos.quality.aiactfria.domain.Fria;
import com.openlab.qualitos.quality.aiactfria.domain.FriaNotFoundException;
import com.openlab.qualitos.quality.aiactfria.domain.FriaRepository;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStateException;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FriaServiceTest {

    @Mock FriaRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock FriaEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();

    FriaService service;

    @BeforeEach
    void setup() {
        service = new FriaService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            Fria f = inv.getArgument(0);
            if (f.getId() == null) f.assignId(ID);
            return f;
        });
    }

    @Test
    void draft_publishes() {
        FriaDto.View v = service.draft(req("REF-1"));
        verify(events).publish(any(), eq(FriaEventPublisher.Action.DRAFTED));
        assertThat(v.status()).isEqualTo(FriaStatus.DRAFT);
    }

    @Test
    void draft_duplicate_throws() {
        when(repo.existsByTenantAndReference(TENANT, "REF-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.draft(req("REF-DUP")))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void draft_missingActor_throws() {
        FriaDto.DraftRequest r = new FriaDto.DraftRequest("REF-1", SYS,
                "process", null, "cat", "risks", null, null, null, null);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void draft_missingSystem_throws() {
        FriaDto.DraftRequest r = new FriaDto.DraftRequest("REF-1", null,
                "process", null, "cat", "risks", null, null, null, USER);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void edit_ok() {
        Fria f = approvable();
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        FriaDto.View v = service.edit(ID, new FriaDto.EditRequest(
                "new", null, "cat", "risks", "m", "o", "c"));
        assertThat(v.processDescription()).isEqualTo("new");
        verify(events).publish(any(), eq(FriaEventPublisher.Action.EDITED));
    }

    @Test
    void edit_crossTenant_404() {
        Fria foreign = Fria.draft(OTHER, "REF-1", SYS, "p", null, "c", "r",
                null, null, null, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.edit(ID, new FriaDto.EditRequest(
                "x", null, "c", "r", "m", "o", "comp")))
                .isInstanceOf(FriaNotFoundException.class);
    }

    @Test
    void submit_ok() {
        Fria f = approvable();
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        FriaDto.View v = service.submit(ID, new FriaDto.SubmitRequest(USER));
        assertThat(v.status()).isEqualTo(FriaStatus.SUBMITTED);
        verify(events).publish(any(), eq(FriaEventPublisher.Action.SUBMITTED));
    }

    @Test
    void submit_missingSubmitter_throws() {
        Fria f = approvable();
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        assertThatThrownBy(() -> service.submit(ID, new FriaDto.SubmitRequest(null)))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void approve_ok() {
        Fria f = approvable();
        f.submit(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        FriaDto.View v = service.approve(ID, new FriaDto.ApproveRequest(APPROVER, "ok"));
        assertThat(v.status()).isEqualTo(FriaStatus.APPROVED);
        verify(events).publish(any(), eq(FriaEventPublisher.Action.APPROVED));
    }

    @Test
    void approve_missingApprover_throws() {
        Fria f = approvable();
        f.submit(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        assertThatThrownBy(() -> service.approve(ID, new FriaDto.ApproveRequest(null, "x")))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void returnToDraft_ok() {
        Fria f = approvable();
        f.submit(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        FriaDto.View v = service.returnToDraft(ID, new FriaDto.ReturnRequest("more detail"));
        assertThat(v.status()).isEqualTo(FriaStatus.DRAFT);
        verify(events).publish(any(), eq(FriaEventPublisher.Action.RETURNED_TO_DRAFT));
    }

    @Test
    void archive_ok() {
        Fria f = approvable();
        f.submit(USER, NOW);
        f.approve(APPROVER, null, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        FriaDto.View v = service.archive(ID, new FriaDto.ArchiveRequest("done"));
        assertThat(v.status()).isEqualTo(FriaStatus.ARCHIVED);
        verify(events).publish(any(), eq(FriaEventPublisher.Action.ARCHIVED));
    }

    @Test
    void delete_draftOnly() {
        Fria f = approvable();
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(FriaEventPublisher.Action.DELETED));
    }

    @Test
    void delete_nonDraft_throws() {
        Fria f = approvable();
        f.submit(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(f));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(FriaStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_notFound_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(FriaNotFoundException.class);
    }

    @Test
    void get_crossTenant_404() {
        Fria foreign = Fria.draft(OTHER, "REF-1", SYS, "p", null, "c", "r",
                null, null, null, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(FriaNotFoundException.class);
    }

    @Test
    void list_all() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(approvable()));
        assertThat(service.list(null)).hasSize(1);
    }

    @Test
    void list_byStatus() {
        when(repo.findByTenantAndStatus(TENANT, FriaStatus.DRAFT))
                .thenReturn(List.of(approvable()));
        assertThat(service.list(FriaStatus.DRAFT)).hasSize(1);
    }

    @Test
    void listByAiSystem_ok() {
        when(repo.findByTenantAndAiSystemId(TENANT, SYS))
                .thenReturn(List.of(approvable()));
        assertThat(service.listByAiSystem(SYS)).hasSize(1);
    }

    @Test
    void listByAiSystem_null_throws() {
        assertThatThrownBy(() -> service.listByAiSystem(null))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void getByReference_notFound_throws() {
        when(repo.findByTenantAndReference(TENANT, "REF-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("REF-X"))
                .isInstanceOf(FriaNotFoundException.class);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "REF-1"))
                .thenReturn(Optional.of(approvable()));
        assertThat(service.getByReference("REF-1").reference()).isEqualTo("REF-1");
    }

    @Test
    void constructor_withoutEvents_usesNoOp() {
        FriaService s2 = new FriaService(repo, tenantProvider, CLOCK);
        s2.draft(req("REF-N"));
    }

    private FriaDto.DraftRequest req(String ref) {
        return new FriaDto.DraftRequest(ref, SYS,
                "process", "1y", "categories", "risks",
                "mitigation", "oversight", "complaint", USER);
    }

    private Fria approvable() {
        Fria f = Fria.draft(TENANT, "REF-1", SYS, "process", "1y", "cat", "risks",
                "mitigation", "oversight", "complaint", USER, NOW);
        f.assignId(ID);
        return f;
    }
}
