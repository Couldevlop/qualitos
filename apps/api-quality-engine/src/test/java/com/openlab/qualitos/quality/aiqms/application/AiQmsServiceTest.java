package com.openlab.qualitos.quality.aiqms.application;

import com.openlab.qualitos.quality.aiqms.domain.AiQms;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsNotFoundException;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsRepository;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStateException;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStatus;
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
import java.util.Set;
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
class AiQmsServiceTest {

    @Mock AiQmsRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock AiQmsEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();

    AiQmsService service;

    @BeforeEach
    void setup() {
        service = new AiQmsService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReferenceAndVersion(any(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            AiQms q = inv.getArgument(0);
            if (q.getId() == null) q.assignId(ID);
            return q;
        });
    }

    @Test
    void draft_publishes() {
        AiQmsDto.View v = service.draft(req("REF-1", "1.0"));
        verify(events).publish(any(), eq(AiQmsEventPublisher.Action.DRAFTED));
        assertThat(v.status()).isEqualTo(AiQmsStatus.DRAFT);
    }

    @Test
    void draft_duplicate_throws() {
        when(repo.existsByTenantAndReferenceAndVersion(TENANT, "REF-DUP", "1.0")).thenReturn(true);
        assertThatThrownBy(() -> service.draft(req("REF-DUP", "1.0")))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void draft_missingActor_throws() {
        AiQmsDto.DraftRequest r = new AiQmsDto.DraftRequest("REF-1", "1.0", "n", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), null);
        assertThatThrownBy(() -> service.draft(r))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void edit_ok() {
        AiQms q = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        AiQmsDto.View v = service.edit(ID, new AiQmsDto.EditRequest(
                "new", null, "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(SYS)));
        assertThat(v.name()).isEqualTo("new");
        verify(events).publish(any(), eq(AiQmsEventPublisher.Action.EDITED));
    }

    @Test
    void edit_crossTenant_404() {
        AiQms foreign = AiQms.draft(OTHER, "REF-1", "1.0", "n", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.edit(ID, new AiQmsDto.EditRequest(
                "x", null, "x", "x", "x", "x", "x", "x", "x", null, null, null)))
                .isInstanceOf(AiQmsNotFoundException.class);
    }

    @Test
    void approve_ok() {
        AiQms q = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        AiQmsDto.View v = service.approve(ID,
                new AiQmsDto.ApproveRequest(USER, APPROVER, "ok"));
        assertThat(v.status()).isEqualTo(AiQmsStatus.APPROVED);
        verify(events).publish(any(), eq(AiQmsEventPublisher.Action.APPROVED));
    }

    @Test
    void approve_missingSubmitter_throws() {
        AiQms q = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        assertThatThrownBy(() -> service.approve(ID,
                new AiQmsDto.ApproveRequest(null, APPROVER, null)))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void approve_missingApprover_throws() {
        AiQms q = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        assertThatThrownBy(() -> service.approve(ID,
                new AiQmsDto.ApproveRequest(USER, null, null)))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void putInForce_ok() {
        AiQms q = ready();
        q.approve(USER, APPROVER, "ok", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        AiQmsDto.View v = service.putInForce(ID);
        assertThat(v.status()).isEqualTo(AiQmsStatus.IN_FORCE);
        verify(events).publish(any(), eq(AiQmsEventPublisher.Action.IN_FORCE));
    }

    @Test
    void supersede_ok() {
        AiQms q = ready();
        q.approve(USER, APPROVER, "ok", NOW);
        q.putInForce(NOW);
        UUID newQmsId = UUID.randomUUID();
        AiQms newQms = AiQms.draft(TENANT, "REF-2", "2.0", "n", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), USER, NOW);
        newQms.assignId(newQmsId);
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        when(repo.findById(newQmsId)).thenReturn(Optional.of(newQms));
        AiQmsDto.View v = service.supersede(ID,
                new AiQmsDto.SupersedeRequest(newQmsId));
        assertThat(v.status()).isEqualTo(AiQmsStatus.SUPERSEDED);
        verify(events).publish(any(), eq(AiQmsEventPublisher.Action.SUPERSEDED));
    }

    @Test
    void supersede_missingId_throws() {
        AiQms q = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        assertThatThrownBy(() -> service.supersede(ID,
                new AiQmsDto.SupersedeRequest(null)))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void archive_ok() {
        AiQms q = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        AiQmsDto.View v = service.archive(ID, new AiQmsDto.ArchiveRequest("reason"));
        assertThat(v.status()).isEqualTo(AiQmsStatus.ARCHIVED);
        verify(events).publish(any(), eq(AiQmsEventPublisher.Action.ARCHIVED));
    }

    @Test
    void delete_draftOnly() {
        AiQms q = ready();
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(AiQmsEventPublisher.Action.DELETED));
    }

    @Test
    void delete_nonDraft_throws() {
        AiQms q = ready();
        q.approve(USER, APPROVER, "ok", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(q));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(AiQmsStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_notFound_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(AiQmsNotFoundException.class);
    }

    @Test
    void get_crossTenant_404() {
        AiQms foreign = AiQms.draft(OTHER, "REF-1", "1.0", "n", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(AiQmsNotFoundException.class);
    }

    @Test
    void list_all() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(ready()));
        assertThat(service.list(null)).hasSize(1);
    }

    @Test
    void list_byStatus() {
        when(repo.findByTenantAndStatus(TENANT, AiQmsStatus.DRAFT))
                .thenReturn(List.of(ready()));
        assertThat(service.list(AiQmsStatus.DRAFT)).hasSize(1);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void getByReference_notFound_throws() {
        when(repo.findByTenantAndReference(TENANT, "REF-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("REF-X"))
                .isInstanceOf(AiQmsNotFoundException.class);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "REF-1"))
                .thenReturn(Optional.of(ready()));
        assertThat(service.getByReference("REF-1").reference()).isEqualTo("REF-1");
    }

    @Test
    void constructor_withoutEvents_usesNoOp() {
        AiQmsService s2 = new AiQmsService(repo, tenantProvider, CLOCK);
        s2.draft(req("REF-N", "1.0"));
    }

    private AiQmsDto.DraftRequest req(String ref, String ver) {
        return new AiQmsDto.DraftRequest(ref, ver, "Name", "desc",
                "compliance", "design", "quality", "data", "risk",
                "pmm", "comm", "resource", "supplier", Set.of(), USER);
    }

    private AiQms ready() {
        AiQms q = AiQms.draft(TENANT, "REF-1", "1.0", "Name", "desc",
                "compliance", "design", "quality", "data", "risk",
                "pmm", "comm", "resource", "supplier", Set.of(), USER, NOW);
        q.assignId(ID);
        return q;
    }
}
