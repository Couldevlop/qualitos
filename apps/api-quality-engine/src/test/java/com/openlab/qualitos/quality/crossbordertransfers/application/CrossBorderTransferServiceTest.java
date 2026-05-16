package com.openlab.qualitos.quality.crossbordertransfers.application;

import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferNotFoundException;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferRepository;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStateException;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStatus;
import com.openlab.qualitos.quality.crossbordertransfers.domain.TransferMechanism;
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
class CrossBorderTransferServiceTest {

    @Mock CrossBorderTransferRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock CrossBorderTransferEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    CrossBorderTransferService service;

    @BeforeEach
    void setup() {
        service = new CrossBorderTransferService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            CrossBorderTransfer t = inv.getArgument(0);
            if (t.getId() == null) t.assignId(ID);
            return t;
        });
    }

    @Test
    void create_persistsAndPublishes() {
        CrossBorderTransferDto.View v = service.create(req("CBT-2026-001"));
        verify(events).publish(any(), eq(CrossBorderTransferEventPublisher.Action.CREATED));
        assertThat(v.status()).isEqualTo(CrossBorderTransferStatus.DRAFT);
    }

    @Test
    void create_duplicateRef_throws() {
        when(repo.existsByTenantAndReference(TENANT, "CBT-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("CBT-DUP")))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void create_missingActor_throws() {
        CrossBorderTransferDto.CreateRequest r = new CrossBorderTransferDto.CreateRequest(
                "CBT-1", "Acme", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), null);
        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void create_missingMechanism_throws() {
        CrossBorderTransferDto.CreateRequest r = new CrossBorderTransferDto.CreateRequest(
                "CBT-1", "Acme", null, null, Set.of("US"), null,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), USER);
        assertThatThrownBy(() -> service.create(r))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void edit_succeeds_onDraft() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        CrossBorderTransferDto.View v = service.edit(ID, editReq());
        assertThat(v.recipientName()).isEqualTo("Updated");
    }

    @Test
    void edit_missingMechanism_throws() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        CrossBorderTransferDto.EditRequest r = new CrossBorderTransferDto.EditRequest(
                "Updated", null, null, Set.of("US"), null,
                "SCC", null, null, Set.of(), Set.of(), Set.of());
        assertThatThrownBy(() -> service.edit(ID, r))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void activate_publishesActivated() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        CrossBorderTransferDto.View v = service.activate(ID);
        assertThat(v.status()).isEqualTo(CrossBorderTransferStatus.ACTIVE);
        verify(events).publish(any(), eq(CrossBorderTransferEventPublisher.Action.ACTIVATED));
    }

    @Test
    void activate_fromSuspended_publishesReactivated() {
        CrossBorderTransfer t = stored();
        t.activate(NOW);
        t.suspend("Pause", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(t));
        service.activate(ID);
        verify(events).publish(any(), eq(CrossBorderTransferEventPublisher.Action.REACTIVATED));
    }

    @Test
    void suspend_succeeds() {
        CrossBorderTransfer t = stored();
        t.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(t));
        CrossBorderTransferDto.View v = service.suspend(ID,
                new CrossBorderTransferDto.SuspendRequest("Audit"));
        assertThat(v.status()).isEqualTo(CrossBorderTransferStatus.SUSPENDED);
        verify(events).publish(any(), eq(CrossBorderTransferEventPublisher.Action.SUSPENDED));
    }

    @Test
    void terminate_succeeds() {
        CrossBorderTransfer t = stored();
        t.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(t));
        CrossBorderTransferDto.View v = service.terminate(ID,
                new CrossBorderTransferDto.TerminateRequest("Fin contrat"));
        assertThat(v.status()).isEqualTo(CrossBorderTransferStatus.TERMINATED);
    }

    @Test
    void delete_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.delete(ID);
        verify(repo).delete(ID);
    }

    @Test
    void delete_onActive_throws() {
        CrossBorderTransfer t = stored();
        t.activate(NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(CrossBorderTransferStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(CrossBorderTransferNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        CrossBorderTransfer other = CrossBorderTransfer.draft(UUID.randomUUID(), "CBT-X",
                "X", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(CrossBorderTransferNotFoundException.class);
    }

    @Test
    void list_byStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, CrossBorderTransferStatus.ACTIVE))
                .thenReturn(List.of(stored()));
        assertThat(service.list(CrossBorderTransferStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void getByReference_missing_404() {
        when(repo.findByTenantAndReference(TENANT, "CBT-X"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("CBT-X"))
                .isInstanceOf(CrossBorderTransferNotFoundException.class);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "CBT-1"))
                .thenReturn(Optional.of(stored()));
        assertThat(service.getByReference("CBT-1").reference()).isEqualTo("CBT-2026-001");
    }

    private CrossBorderTransferDto.CreateRequest req(String ref) {
        return new CrossBorderTransferDto.CreateRequest(ref,
                "Acme Cloud Inc", null, "ops@acme.com", Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC 2021", null, null,
                Set.of("identity"), Set.of(), Set.of(), USER);
    }

    private CrossBorderTransferDto.EditRequest editReq() {
        return new CrossBorderTransferDto.EditRequest("Updated", null, "ops@x.com",
                Set.of("US"), TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of());
    }

    private CrossBorderTransfer stored() {
        CrossBorderTransfer t = CrossBorderTransfer.draft(TENANT, "CBT-2026-001",
                "Acme Cloud Inc", null, "ops@acme.com",
                Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC 2021", null, null,
                Set.of(), Set.of(), Set.of(), USER, NOW);
        t.assignId(ID);
        return t;
    }
}
