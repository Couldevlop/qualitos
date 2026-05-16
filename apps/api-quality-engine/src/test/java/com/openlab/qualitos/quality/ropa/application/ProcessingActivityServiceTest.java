package com.openlab.qualitos.quality.ropa.application;

import com.openlab.qualitos.quality.ropa.domain.LawfulBasis;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivity;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityNotFoundException;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityRepository;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStateException;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStatus;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessingActivityServiceTest {

    @Mock ProcessingActivityRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock ProcessingActivityEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    ProcessingActivityService service;

    @BeforeEach
    void setup() {
        service = new ProcessingActivityService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            ProcessingActivity a = inv.getArgument(0);
            if (a.getId() == null) a.assignId(ID);
            return a;
        });
    }

    @Test
    void create_persistsDraft_andPublishes() {
        ProcessingActivityDto.View v = service.create(req("ROPA-2026-001"));
        verify(events).publish(any(), eq(ProcessingActivityEventPublisher.Action.CREATED));
        assertThat(v.status()).isEqualTo(ProcessingActivityStatus.DRAFT);
        assertThat(v.reference()).isEqualTo("ROPA-2026-001");
    }

    @Test
    void create_duplicateReference_throws() {
        when(repo.existsByTenantAndReference(TENANT, "ROPA-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("ROPA-DUP")))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    @Test
    void create_missingActor_throws() {
        ProcessingActivityDto.CreateRequest r = new ProcessingActivityDto.CreateRequest(
                "ROPA-1", "n", "p", LawfulBasis.CONTRACT, null,
                "C", "c@x", null, null, null,
                Set.of(), Set.of(), false, null,
                Set.of(), Set.of(), null, Set.of(), null, null, null);
        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    @Test
    void edit_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        ProcessingActivityDto.View v = service.edit(ID, editReq("Updated"));
        verify(events).publish(any(), eq(ProcessingActivityEventPublisher.Action.EDITED));
        assertThat(v.name()).isEqualTo("Updated");
    }

    @Test
    void edit_onActive_throws() {
        ProcessingActivity a = stored();
        a.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.edit(ID, editReq("X")))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    @Test
    void activate_moves_andPublishes() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        ProcessingActivityDto.View v = service.activate(ID);
        assertThat(v.status()).isEqualTo(ProcessingActivityStatus.ACTIVE);
        verify(events).publish(any(), eq(ProcessingActivityEventPublisher.Action.ACTIVATED));
    }

    @Test
    void archive_fromActive_succeeds() {
        ProcessingActivity a = stored();
        a.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        ProcessingActivityDto.View v = service.archive(ID);
        assertThat(v.status()).isEqualTo(ProcessingActivityStatus.ARCHIVED);
    }

    @Test
    void delete_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(ProcessingActivityEventPublisher.Action.DELETED));
    }

    @Test
    void delete_onActive_throws() {
        ProcessingActivity a = stored();
        a.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(ProcessingActivityStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ProcessingActivityNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        ProcessingActivity other = ProcessingActivity.draft(UUID.randomUUID(),
                "ROPA-X", "n", "p", LawfulBasis.CONTRACT, null,
                "C", "c@x", null, null, null,
                Set.of(), Set.of(), false, null,
                Set.of(), Set.of(), null, Set.of(), null, null, USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(ProcessingActivityNotFoundException.class);
    }

    @Test
    void list_withStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, ProcessingActivityStatus.ACTIVE))
                .thenReturn(List.of(stored()));
        assertThat(service.list(ProcessingActivityStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void list_nullStatus_returnsAll() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(stored(), stored()));
        assertThat(service.list(null)).hasSize(2);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "ROPA-1"))
                .thenReturn(Optional.of(stored()));
        assertThat(service.getByReference("ROPA-1").reference()).isEqualTo("ROPA-2026-001");
    }

    @Test
    void getByReference_missing_throws() {
        when(repo.findByTenantAndReference(TENANT, "ROPA-X"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("ROPA-X"))
                .isInstanceOf(ProcessingActivityNotFoundException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    private ProcessingActivityDto.CreateRequest req(String ref) {
        return new ProcessingActivityDto.CreateRequest(
                ref, "Customer CRM", "Manage relationships",
                LawfulBasis.CONTRACT, null,
                "Acme Corp", "dpo@acme.com", null, null, null,
                Set.of("customers"), Set.of("identity"),
                false, null, Set.of("staff"), Set.of(), null,
                Set.of(), null, null, USER);
    }

    private ProcessingActivityDto.EditRequest editReq(String name) {
        return new ProcessingActivityDto.EditRequest(
                name, "Updated purposes",
                LawfulBasis.CONTRACT, null,
                "Acme Corp", "dpo@acme.com", null, null, null,
                Set.of("customers"), Set.of("identity"),
                false, null, Set.of("staff"), Set.of(), null,
                Set.of(), null, null);
    }

    private ProcessingActivity stored() {
        ProcessingActivity a = ProcessingActivity.draft(TENANT, "ROPA-2026-001",
                "Customer CRM", "Manage relationships",
                LawfulBasis.CONTRACT, null,
                "Acme Corp", "dpo@acme.com", null, null, null,
                Set.of("customers"), Set.of("identity"),
                false, null, Set.of("staff"), Set.of(), null,
                Set.of(), null, null, USER, NOW);
        a.assignId(ID);
        return a;
    }
}
