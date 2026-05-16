package com.openlab.qualitos.quality.automateddecisions.application;

import com.openlab.qualitos.quality.automateddecisions.domain.Art22LawfulBasis;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionNotFoundException;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRepository;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStateException;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStatus;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionType;
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
class AutomatedDecisionServiceTest {

    @Mock AutomatedDecisionRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock AutomatedDecisionEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    AutomatedDecisionService service;

    @BeforeEach
    void setup() {
        service = new AutomatedDecisionService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            AutomatedDecisionRecord r = inv.getArgument(0);
            if (r.getId() == null) r.assignId(ID);
            return r;
        });
    }

    @Test
    void create_persistsDraft_andPublishes() {
        AutomatedDecisionDto.View v = service.create(req("ADM-1",
                AutomatedDecisionType.PROFILING_ONLY));
        verify(events).publish(any(), eq(AutomatedDecisionEventPublisher.Action.CREATED));
        assertThat(v.status()).isEqualTo(AutomatedDecisionStatus.DRAFT);
    }

    @Test
    void create_duplicateRef_throws() {
        when(repo.existsByTenantAndReference(TENANT, "ADM-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("ADM-DUP", AutomatedDecisionType.PROFILING_ONLY)))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    @Test
    void create_missingActor_throws() {
        AutomatedDecisionDto.CreateRequest r = new AutomatedDecisionDto.CreateRequest(
                "ADM-1", "Name", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null, null, null, null, null,
                null /* createdByUserId */);
        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    @Test
    void create_missingDecisionType_throws() {
        AutomatedDecisionDto.CreateRequest r = new AutomatedDecisionDto.CreateRequest(
                "ADM-1", "Name", null, null,
                null, null, Set.of(), Set.of(), null, null, null, null, null, USER);
        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    @Test
    void edit_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        AutomatedDecisionDto.View v = service.edit(ID, editReq());
        assertThat(v.name()).isEqualTo("Updated");
        verify(events).publish(any(), eq(AutomatedDecisionEventPublisher.Action.EDITED));
    }

    @Test
    void edit_onActive_throws() {
        AutomatedDecisionRecord r = stored();
        r.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.edit(ID, editReq()))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    @Test
    void edit_missingDecisionType_throws() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        AutomatedDecisionDto.EditRequest e = new AutomatedDecisionDto.EditRequest(
                "X", null, null, null, null, Set.of(), Set.of(), null,
                null, null, null, null);
        assertThatThrownBy(() -> service.edit(ID, e))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    @Test
    void activate_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        AutomatedDecisionDto.View v = service.activate(ID);
        assertThat(v.status()).isEqualTo(AutomatedDecisionStatus.ACTIVE);
        verify(events).publish(any(), eq(AutomatedDecisionEventPublisher.Action.ACTIVATED));
    }

    @Test
    void deprecate_succeeds() {
        AutomatedDecisionRecord r = stored();
        r.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        AutomatedDecisionDto.View v = service.deprecate(ID);
        assertThat(v.status()).isEqualTo(AutomatedDecisionStatus.DEPRECATED);
        verify(events).publish(any(), eq(AutomatedDecisionEventPublisher.Action.DEPRECATED));
    }

    @Test
    void archive_succeeds() {
        AutomatedDecisionRecord r = stored();
        r.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        AutomatedDecisionDto.View v = service.archive(ID);
        assertThat(v.status()).isEqualTo(AutomatedDecisionStatus.ARCHIVED);
        verify(events).publish(any(), eq(AutomatedDecisionEventPublisher.Action.ARCHIVED));
    }

    @Test
    void delete_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(AutomatedDecisionEventPublisher.Action.DELETED));
    }

    @Test
    void delete_onActive_throws() {
        AutomatedDecisionRecord r = stored();
        r.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(AutomatedDecisionStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(AutomatedDecisionNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        AutomatedDecisionRecord other = AutomatedDecisionRecord.draft(UUID.randomUUID(),
                "ADM-X", "N", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(AutomatedDecisionNotFoundException.class);
    }

    @Test
    void list_byStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, AutomatedDecisionStatus.ACTIVE))
                .thenReturn(List.of(stored()));
        assertThat(service.list(AutomatedDecisionStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void list_nullStatus_returnsAll() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(stored(), stored()));
        assertThat(service.list(null)).hasSize(2);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "ADM-1"))
                .thenReturn(Optional.of(stored()));
        assertThat(service.getByReference("ADM-1").reference()).isEqualTo("ADM-2026-001");
    }

    @Test
    void getByReference_missing_404() {
        when(repo.findByTenantAndReference(TENANT, "ADM-X"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("ADM-X"))
                .isInstanceOf(AutomatedDecisionNotFoundException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    private AutomatedDecisionDto.CreateRequest req(String ref, AutomatedDecisionType type) {
        return new AutomatedDecisionDto.CreateRequest(ref, "Name", null, type,
                null, null, Set.of(), Set.of(), null, null, null, null, null, USER);
    }

    private AutomatedDecisionDto.EditRequest editReq() {
        return new AutomatedDecisionDto.EditRequest("Updated", "desc",
                AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null, null, null, null, null);
    }

    private AutomatedDecisionRecord stored() {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(TENANT, "ADM-2026-001",
                "Recommandations", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, USER, NOW);
        r.assignId(ID);
        return r;
    }
}
