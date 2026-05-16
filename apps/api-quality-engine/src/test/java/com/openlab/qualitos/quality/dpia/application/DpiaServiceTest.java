package com.openlab.qualitos.quality.dpia.application;

import com.openlab.qualitos.quality.dpia.domain.Dpia;
import com.openlab.qualitos.quality.dpia.domain.DpiaNotFoundException;
import com.openlab.qualitos.quality.dpia.domain.DpiaRepository;
import com.openlab.qualitos.quality.dpia.domain.DpiaStateException;
import com.openlab.qualitos.quality.dpia.domain.DpiaStatus;
import com.openlab.qualitos.quality.dpia.domain.RiskLevel;
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
class DpiaServiceTest {

    @Mock DpiaRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock DpiaEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID DPO = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    DpiaService service;

    @BeforeEach
    void setup() {
        service = new DpiaService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            Dpia d = inv.getArgument(0);
            if (d.getId() == null) d.assignId(ID);
            return d;
        });
    }

    @Test
    void create_persistsDraft_andPublishes() {
        DpiaDto.View v = service.create(req("DPIA-2026-001"));
        verify(events).publish(any(), eq(DpiaEventPublisher.Action.CREATED));
        assertThat(v.status()).isEqualTo(DpiaStatus.DRAFT);
    }

    @Test
    void create_duplicateReference_throws() {
        when(repo.existsByTenantAndReference(TENANT, "DPIA-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.create(req("DPIA-DUP")))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void create_missingActor_throws() {
        assertThatThrownBy(() -> service.create(new DpiaDto.CreateRequest(
                "DPIA-1", "t", null, Set.of(), RiskLevel.LOW, null)))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void create_missingInitialRisk_throws() {
        assertThatThrownBy(() -> service.create(new DpiaDto.CreateRequest(
                "DPIA-1", "t", null, Set.of(), null, USER)))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void edit_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        DpiaDto.View v = service.edit(ID, editReq(RiskLevel.LOW, false, null));
        verify(events).publish(any(), eq(DpiaEventPublisher.Action.EDITED));
        assertThat(v.overallRiskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void edit_nullRisk_throws() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        assertThatThrownBy(() -> service.edit(ID, new DpiaDto.EditRequest(
                "t", null, Set.of(), "n", "r", "m", null, false, null)))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void edit_onActive_throws() {
        Dpia d = readyForReview(RiskLevel.LOW);
        d.approve(DPO, "ok", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.edit(ID, editReq(RiskLevel.LOW, false, null)))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void start_moves_andPublishes() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        DpiaDto.View v = service.start(ID, new DpiaDto.StartRequest(USER));
        assertThat(v.status()).isEqualTo(DpiaStatus.IN_PROGRESS);
        verify(events).publish(any(), eq(DpiaEventPublisher.Action.STARTED));
    }

    @Test
    void returnToDraft_succeeds() {
        Dpia d = stored();
        d.start(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(d));
        DpiaDto.View v = service.returnToDraft(ID);
        assertThat(v.status()).isEqualTo(DpiaStatus.DRAFT);
    }

    @Test
    void submitToDpo_succeeds() {
        Dpia d = withAnalysis(RiskLevel.LOW);
        d.start(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(d));
        DpiaDto.View v = service.submitToDpo(ID);
        assertThat(v.status()).isEqualTo(DpiaStatus.DPO_REVIEW);
    }

    @Test
    void approve_lowRisk_succeeds() {
        Dpia d = readyForReview(RiskLevel.LOW);
        when(repo.findById(ID)).thenReturn(Optional.of(d));
        DpiaDto.View v = service.approve(ID, new DpiaDto.OpinionRequest(DPO, "conforme"));
        assertThat(v.status()).isEqualTo(DpiaStatus.APPROVED);
        verify(events).publish(any(), eq(DpiaEventPublisher.Action.APPROVED));
    }

    @Test
    void approve_highRisk_withoutConsultationFlag_throws() {
        // High risk but consultationRequired = false → l'approbation doit échouer.
        Dpia d = stored();
        d.editDraft(d.getTitle(), d.getDescription(),
                d.getLinkedProcessingActivityIds(),
                "necessity ok", "risks identified", "mitigations applied",
                RiskLevel.HIGH, false, null, NOW);
        d.start(USER, NOW.plusSeconds(60));
        d.submitToDpo(NOW.plusSeconds(120));
        when(repo.findById(ID)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.approve(ID,
                new DpiaDto.OpinionRequest(DPO, "ok")))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void reject_succeeds() {
        Dpia d = readyForReview(RiskLevel.MEDIUM);
        when(repo.findById(ID)).thenReturn(Optional.of(d));
        DpiaDto.View v = service.reject(ID,
                new DpiaDto.OpinionRequest(DPO, "non conforme"));
        assertThat(v.status()).isEqualTo(DpiaStatus.REJECTED);
        verify(events).publish(any(), eq(DpiaEventPublisher.Action.REJECTED));
    }

    @Test
    void archive_fromApproved_succeeds() {
        Dpia d = readyForReview(RiskLevel.LOW);
        d.approve(DPO, "ok", NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(d));
        DpiaDto.View v = service.archive(ID);
        assertThat(v.status()).isEqualTo(DpiaStatus.ARCHIVED);
    }

    @Test
    void delete_onDraft_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored()));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(DpiaEventPublisher.Action.DELETED));
    }

    @Test
    void delete_onInProgress_throws() {
        Dpia d = stored();
        d.start(USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(DpiaStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(DpiaNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        Dpia other = Dpia.draft(UUID.randomUUID(), "DPIA-X", "t", null,
                Set.of(), RiskLevel.LOW, USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(DpiaNotFoundException.class);
    }

    @Test
    void list_byStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, DpiaStatus.DRAFT))
                .thenReturn(List.of(stored()));
        assertThat(service.list(DpiaStatus.DRAFT)).hasSize(1);
    }

    @Test
    void requiringConsultation_filtersHighOrSevere_andNotTerminal() {
        Dpia low = stored();
        Dpia high = Dpia.draft(TENANT, "DPIA-H", "t", null,
                Set.of(), RiskLevel.HIGH, USER, NOW);
        high.assignId(UUID.randomUUID());
        Dpia archived = Dpia.draft(TENANT, "DPIA-A", "t", null,
                Set.of(), RiskLevel.SEVERE, USER, NOW);
        archived.assignId(UUID.randomUUID());
        // Force archived state
        archived.editDraft("t", null, Set.of(), "n", "r", "m",
                RiskLevel.SEVERE, true, "notes", NOW);
        archived.start(USER, NOW);
        archived.submitToDpo(NOW);
        archived.reject(DPO, "rejected", NOW); // terminal
        when(repo.findByTenant(TENANT)).thenReturn(List.of(low, high, archived));
        assertThat(service.requiringConsultation()).hasSize(1);
    }

    private DpiaDto.CreateRequest req(String ref) {
        return new DpiaDto.CreateRequest(ref, "Hiring check", null,
                Set.of(UUID.randomUUID()), RiskLevel.LOW, USER);
    }

    private DpiaDto.EditRequest editReq(RiskLevel risk, boolean consultation, String notes) {
        return new DpiaDto.EditRequest(
                "Updated title", "desc", Set.of(),
                "necessity ok", "risks identified", "mitigations applied",
                risk, consultation, notes);
    }

    private Dpia stored() {
        Dpia d = Dpia.draft(TENANT, "DPIA-2026-001", "Hiring check", null,
                Set.of(UUID.randomUUID()), RiskLevel.LOW, USER, NOW);
        d.assignId(ID);
        return d;
    }

    private Dpia withAnalysis(RiskLevel level) {
        Dpia d = stored();
        d.editDraft(d.getTitle(), d.getDescription(),
                d.getLinkedProcessingActivityIds(),
                "necessity ok", "risks identified", "mitigations applied",
                level, level.requiresPriorConsultation(),
                level.requiresPriorConsultation() ? "CNIL consultation initiated" : null,
                NOW);
        return d;
    }

    private Dpia readyForReview(RiskLevel level) {
        Dpia d = withAnalysis(level);
        d.start(USER, NOW.plusSeconds(60));
        d.submitToDpo(NOW.plusSeconds(120));
        return d;
    }
}
