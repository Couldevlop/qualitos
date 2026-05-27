package com.openlab.qualitos.quality.capa;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapaServiceTest {

    @Mock CapaCaseRepository caseRepo;
    @Mock CapaActionRepository actionRepo;
    @Mock AiGatewayClient ai;
    @InjectMocks CapaService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- suggestActions (IA) ---
    @Test
    void suggestActions_parsesLinesAsActions_ignoresPreamble_dedups() {
        UUID id = UUID.randomUUID();
        CapaCase c = new CapaCase();
        c.setTenantId(TENANT);
        c.setTitle("NC répétitive sur joint torique fournisseur Alpha");
        c.setType(CapaType.CORRECTIVE);
        c.setCriticity(CapaCriticity.HIGH);
        c.setStatus(CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(c));

        String llm = String.join("\n",
                "Voici les actions correctives :",
                "- Auditer le fournisseur Alpha sur site",
                "2. Renforcer le plan de contrôle réception",
                "- Auditer le fournisseur Alpha sur site");
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult(llm, "ollama", 80, 900));

        List<CapaDto.SuggestedAction> res = service.suggestActions(id);

        assertThat(res).extracting(CapaDto.SuggestedAction::title)
                .containsExactly("Auditer le fournisseur Alpha sur site",
                        "Renforcer le plan de contrôle réception");
    }

    // --- create ---
    @Test
    void create_success() {
        CapaDto.CreateCaseRequest req = req();
        CapaCase saved = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.save(any())).thenReturn(saved);

        CapaDto.CaseResponse r = service.createCase(req);

        assertThat(r.status()).isEqualTo(CapaStatus.OPEN);
        assertThat(r.tenantId()).isEqualTo(TENANT);
    }

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.createCase(req()))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(caseRepo);
    }

    // --- findAll ---
    @Test
    void findAll_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(caseRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.OPEN))));
        Page<CapaDto.CaseResponse> r = service.findAll(null, p);
        assertThat(r.getContent()).hasSize(1);
        verify(caseRepo, never()).findByTenantIdAndStatus(any(), any(), any());
    }

    @Test
    void findAll_withFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(caseRepo.findByTenantIdAndStatus(TENANT, CapaStatus.CLOSED, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.CLOSED))));
        Page<CapaDto.CaseResponse> r = service.findAll(CapaStatus.CLOSED, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(CapaStatus.CLOSED);
    }

    // --- findById ---
    @Test
    void findById_found() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThat(service.findById(c.getId()).id()).isEqualTo(c.getId());
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(caseRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(CapaNotFoundException.class);
    }

    @Test
    void findById_wrongTenant_notFound() {
        CapaCase c = capa(OTHER, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(c.getId()))
                .isInstanceOf(CapaNotFoundException.class);
    }

    // --- update ---
    @Test
    void update_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        UUID rc = UUID.randomUUID();
        LocalDate due = LocalDate.now().plusDays(10);
        CapaDto.UpdateCaseRequest req = new CapaDto.UpdateCaseRequest(
                "t2", "d2", CapaCriticity.CRITICAL, "ref2", rc, due);
        service.updateCase(c.getId(), req);
        assertThat(c.getTitle()).isEqualTo("t2");
        assertThat(c.getCriticity()).isEqualTo(CapaCriticity.CRITICAL);
        assertThat(c.getRootCauseId()).isEqualTo(rc);
        assertThat(c.getDueDate()).isEqualTo(due);
    }

    @Test
    void update_closed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateCase(c.getId(),
                new CapaDto.UpdateCaseRequest("x", null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    @Test
    void update_rejected_throws() {
        CapaCase c = capa(TENANT, CapaStatus.REJECTED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateCase(c.getId(),
                new CapaDto.UpdateCaseRequest("x", null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- start ---
    @Test
    void start_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.startCase(c.getId());
        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    @Test
    void start_notOpen_throws() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.startCase(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    // --- resolve ---
    @Test
    void resolve_success() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.DONE);
        c.getActions().add(a);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.resolveCase(c.getId());
        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        assertThat(c.getResolvedAt()).isNotNull();
    }

    @Test
    void resolve_actionsNotAllDone_throws() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.getActions().add(action(c, CapaActionStatus.PENDING));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.resolveCase(c.getId()))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("DONE");
    }

    @Test
    void resolve_noActions_throws() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.resolveCase(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    @Test
    void resolve_notInProgress_throws() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.resolveCase(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    // --- effectiveness ---
    @Test
    void effectiveness_effective_closesCapa() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(true));
        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
        assertThat(c.getEffectivenessVerified()).isTrue();
        assertThat(c.getClosedAt()).isNotNull();
    }

    @Test
    void effectiveness_notEffective_reopensInProgress() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));
        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
        assertThat(c.getEffectivenessVerified()).isFalse();
        assertThat(c.getResolvedAt()).isNull();
    }

    @Test
    void effectiveness_notResolved_throws() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- reject ---
    @Test
    void reject_open_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.rejectCase(c.getId());
        assertThat(c.getStatus()).isEqualTo(CapaStatus.REJECTED);
    }

    @Test
    void reject_inProgress_success() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.rejectCase(c.getId());
        assertThat(c.getStatus()).isEqualTo(CapaStatus.REJECTED);
    }

    @Test
    void reject_resolved_throws() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.rejectCase(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    // --- delete ---
    @Test
    void delete_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        service.deleteCase(c.getId());
        verify(caseRepo).delete(c);
    }

    @Test
    void delete_closed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.deleteCase(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    // --- addAction ---
    @Test
    void addAction_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> {
            CapaAction a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        CapaDto.ActionRequest req = new CapaDto.ActionRequest(
                "fix", "desc", null, UUID.randomUUID(), LocalDate.now().plusDays(5));
        CapaDto.ActionResponse r = service.addAction(c.getId(), req);
        assertThat(r.status()).isEqualTo(CapaActionStatus.PENDING);
    }

    @Test
    void addAction_explicitStatus_used() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CapaDto.ActionRequest req = new CapaDto.ActionRequest(
                "fix", null, CapaActionStatus.IN_PROGRESS, null, null);
        CapaDto.ActionResponse r = service.addAction(c.getId(), req);
        assertThat(r.status()).isEqualTo(CapaActionStatus.IN_PROGRESS);
    }

    @Test
    void addAction_closed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("x", null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- updateAction ---
    @Test
    void updateAction_markDone_setsCompletedAt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest("t", null, CapaActionStatus.DONE, null, null));

        assertThat(a.getStatus()).isEqualTo(CapaActionStatus.DONE);
        assertThat(a.getCompletedAt()).isNotNull();
    }

    @Test
    void updateAction_revertFromDone_clearsCompletedAt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.DONE);
        a.setCompletedAt(Instant.now());
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest(null, null, CapaActionStatus.IN_PROGRESS, null, null));

        assertThat(a.getStatus()).isEqualTo(CapaActionStatus.IN_PROGRESS);
        assertThat(a.getCompletedAt()).isNull();
    }

    @Test
    void updateAction_updatesFields() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        UUID assignee = UUID.randomUUID();
        LocalDate due = LocalDate.now().plusDays(3);
        service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest("nt", "nd", null, assignee, due));
        assertThat(a.getTitle()).isEqualTo("nt");
        assertThat(a.getDescription()).isEqualTo("nd");
        assertThat(a.getAssigneeId()).isEqualTo(assignee);
        assertThat(a.getDueDate()).isEqualTo(due);
    }

    @Test
    void updateAction_notFound_throws() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        UUID actionId = UUID.randomUUID();
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(actionId, c.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateAction(c.getId(), actionId,
                new CapaDto.ActionRequest("x", null, null, null, null)))
                .isInstanceOf(CapaActionNotFoundException.class);
    }

    @Test
    void updateAction_capaClosed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateAction(c.getId(), UUID.randomUUID(),
                new CapaDto.ActionRequest("x", null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- deleteAction ---
    @Test
    void deleteAction_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        service.deleteAction(c.getId(), a.getId());
        verify(actionRepo).delete(a);
    }

    @Test
    void deleteAction_capaRejected_throws() {
        CapaCase c = capa(TENANT, CapaStatus.REJECTED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.deleteAction(c.getId(), UUID.randomUUID()))
                .isInstanceOf(CapaStateException.class);
    }

    @Test
    void deleteAction_notFound_throws() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        UUID aid = UUID.randomUUID();
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(aid, c.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteAction(c.getId(), aid))
                .isInstanceOf(CapaActionNotFoundException.class);
    }

    // --- helpers ---
    private CapaDto.CreateCaseRequest req() {
        return new CapaDto.CreateCaseRequest(
                "Défaut soudure", "desc", CapaType.CORRECTIVE, CapaCriticity.HIGH,
                CapaSourceType.NON_CONFORMITY, "NC-001", OWNER, null, LocalDate.now().plusDays(30));
    }

    private CapaCase capa(UUID tenant, CapaStatus status) {
        CapaCase c = new CapaCase();
        c.setId(UUID.randomUUID());
        c.setTenantId(tenant);
        c.setTitle("t");
        c.setType(CapaType.CORRECTIVE);
        c.setCriticity(CapaCriticity.HIGH);
        c.setStatus(status);
        c.setSourceType(CapaSourceType.NON_CONFORMITY);
        c.setOwnerId(OWNER);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private CapaAction action(CapaCase c, CapaActionStatus status) {
        CapaAction a = new CapaAction();
        a.setId(UUID.randomUUID());
        a.setCapa(c);
        a.setTitle("act");
        a.setStatus(status);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        return a;
    }
}
