package com.openlab.qualitos.quality.dpoappointments.application;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentNotFoundException;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentRepository;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStateException;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStatus;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoType;
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
class DpoAppointmentServiceTest {

    @Mock DpoAppointmentRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock DpoAppointmentEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    DpoAppointmentService service;

    @BeforeEach
    void setup() {
        service = new DpoAppointmentService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            DpoAppointment a = inv.getArgument(0);
            if (a.getId() == null) a.assignId(ID);
            return a;
        });
    }

    @Test
    void propose_persistsAndPublishes() {
        DpoAppointmentDto.View v = service.propose(req("DPO-2026-001"));
        verify(events).publish(any(), eq(DpoAppointmentEventPublisher.Action.PROPOSED));
        assertThat(v.status()).isEqualTo(DpoAppointmentStatus.PROPOSED);
    }

    @Test
    void propose_duplicateReference_throws() {
        when(repo.existsByTenantAndReference(TENANT, "DPO-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.propose(req("DPO-DUP")))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void propose_missingActor_throws() {
        DpoAppointmentDto.ProposeRequest r = new DpoAppointmentDto.ProposeRequest(
                "DPO-1", "Jane", "dpo@x.com", null, DpoType.INTERNAL, null, null,
                "GROUP", Set.of(), null);
        assertThatThrownBy(() -> service.propose(r))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void propose_missingType_throws() {
        DpoAppointmentDto.ProposeRequest r = new DpoAppointmentDto.ProposeRequest(
                "DPO-1", "Jane", "dpo@x.com", null, null, null, null,
                "GROUP", Set.of(), USER);
        assertThatThrownBy(() -> service.propose(r))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void edit_succeeds_onProposed() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        DpoAppointmentDto.View v = service.edit(ID, editReq());
        assertThat(v.dpoFullName()).isEqualTo("Updated");
        verify(events).publish(any(), eq(DpoAppointmentEventPublisher.Action.EDITED));
    }

    @Test
    void edit_missingType_throws() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        DpoAppointmentDto.EditRequest r = new DpoAppointmentDto.EditRequest(
                "Jane", "dpo@x.com", null, null, null, null, Set.of());
        assertThatThrownBy(() -> service.edit(ID, r))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void activate_autoEndsPreviousActive() {
        DpoAppointment proposed = stored();
        UUID oldId = UUID.randomUUID();
        DpoAppointment oldActive = DpoAppointment.propose(TENANT, "DPO-OLD",
                "John", "old@x.com", null,
                DpoType.INTERNAL, null, null, "GROUP", Set.of(), USER, NOW.minusSeconds(86400));
        oldActive.assignId(oldId);
        oldActive.activate(NOW.minusSeconds(86400), NOW.minusSeconds(86400), "CNIL-OLD",
                NOW.minusSeconds(86400));

        when(repo.findById(ID)).thenReturn(Optional.of(proposed));
        when(repo.findActiveByScope(TENANT, "GROUP")).thenReturn(Optional.of(oldActive));

        DpoAppointmentDto.View v = service.activate(ID, new DpoAppointmentDto.ActivateRequest(
                NOW, NOW, "CNIL-2026-001"));
        assertThat(v.status()).isEqualTo(DpoAppointmentStatus.ACTIVE);
        verify(events).publish(any(), eq(DpoAppointmentEventPublisher.Action.ENDED));
        verify(events).publish(any(), eq(DpoAppointmentEventPublisher.Action.ACTIVATED));
    }

    @Test
    void activate_noPreviousActive_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        when(repo.findActiveByScope(TENANT, "GROUP")).thenReturn(Optional.empty());
        service.activate(ID, new DpoAppointmentDto.ActivateRequest(NOW, NOW, "CNIL-1"));
        verify(events, never()).publish(any(), eq(DpoAppointmentEventPublisher.Action.ENDED));
    }

    @Test
    void activate_notProposed_throws() {
        DpoAppointment a = stored();
        a.activate(NOW, NOW, "ref", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.activate(ID,
                new DpoAppointmentDto.ActivateRequest(NOW, NOW, "ref")))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void end_succeeds() {
        DpoAppointment a = stored();
        a.activate(NOW, NOW, "ref", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        DpoAppointmentDto.View v = service.end(ID,
                new DpoAppointmentDto.EndRequest("Fin mandat", NOW.plusSeconds(86400)));
        assertThat(v.status()).isEqualTo(DpoAppointmentStatus.ENDED);
    }

    @Test
    void cancel_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        DpoAppointmentDto.View v = service.cancel(ID,
                new DpoAppointmentDto.CancelRequest("Recrutement annulé"));
        assertThat(v.status()).isEqualTo(DpoAppointmentStatus.CANCELLED);
    }

    @Test
    void delete_onProposed_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(DpoAppointmentEventPublisher.Action.DELETED));
    }

    @Test
    void delete_onActive_throws() {
        DpoAppointment a = stored();
        a.activate(NOW, NOW, "ref", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(DpoAppointmentNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        DpoAppointment other = DpoAppointment.propose(UUID.randomUUID(), "DPO-X",
                "X", "x@x.com", null, DpoType.INTERNAL, null, null,
                "GROUP", Set.of(), USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(DpoAppointmentNotFoundException.class);
    }

    @Test
    void list_byStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, DpoAppointmentStatus.ACTIVE))
                .thenReturn(List.of(stored()));
        assertThat(service.list(DpoAppointmentStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void findActiveByScope_blank_returnsEmpty() {
        assertThat(service.findActiveByScope(" ")).isEmpty();
        verify(repo, never()).findActiveByScope(any(), any());
    }

    @Test
    void findActiveByScope_found() {
        DpoAppointment a = stored();
        a.activate(NOW, NOW, "ref", NOW);
        when(repo.findActiveByScope(TENANT, "GROUP")).thenReturn(Optional.of(a));
        assertThat(service.findActiveByScope("GROUP")).isPresent();
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void getByReference_missing_404() {
        when(repo.findByTenantAndReference(TENANT, "DPO-X"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("DPO-X"))
                .isInstanceOf(DpoAppointmentNotFoundException.class);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "DPO-2026-001"))
                .thenReturn(Optional.of(stored()));
        assertThat(service.getByReference("DPO-2026-001").reference()).isEqualTo("DPO-2026-001");
    }

    private DpoAppointmentDto.ProposeRequest req(String ref) {
        return new DpoAppointmentDto.ProposeRequest(ref,
                "Jane Doe", "dpo@example.com", null,
                DpoType.INTERNAL, null, null, "GROUP", Set.of(), USER);
    }

    private DpoAppointmentDto.EditRequest editReq() {
        return new DpoAppointmentDto.EditRequest("Updated", "updated@x.com", null,
                DpoType.INTERNAL, null, null, Set.of());
    }

    private DpoAppointment stored() {
        DpoAppointment a = DpoAppointment.propose(TENANT, "DPO-2026-001",
                "Jane Doe", "dpo@example.com", null,
                DpoType.INTERNAL, null, null, "GROUP", Set.of(), USER, NOW);
        a.assignId(ID);
        return a;
    }
}
