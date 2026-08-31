package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    @Mock CapaLifecycleJournal journal;
    @Mock com.openlab.qualitos.quality.nonconformity.NonConformityRepository ncRepo;
    @Mock CapaEvidenceRepository evidenceRepo;
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

    /**
     * Un dossier d'ENDIGUEMENT n'appelle pas les mêmes actions qu'un correctif :
     * on lui demande de protéger tout de suite, pas de remonter à la cause.
     * Sans cette branche, l'IA proposait des analyses là où il faut des gestes.
     */
    @Test
    void suggestActions_containment_asksForImmediateProtectionNotRootCause() {
        UUID id = UUID.randomUUID();
        CapaCase c = new CapaCase();
        c.setTenantId(TENANT);
        c.setTitle("Lot 4471 suspect en cours d'expédition");
        c.setType(CapaType.CONTAINMENT);
        c.setCriticity(CapaCriticity.CRITICAL);
        c.setStatus(CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(c));
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult("- Bloquer le lot 4471 en magasin",
                        "ollama", 40, 500));

        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);

        List<CapaDto.SuggestedAction> res = service.suggestActions(id);

        verify(ai).complete(system.capture(), user.capture(), anyInt());
        assertThat(system.getValue()).contains("endiguement");
        assertThat(system.getValue()).doesNotContain("cause racine");
        assertThat(user.getValue()).contains("CONTAINMENT");
        assertThat(res).extracting(CapaDto.SuggestedAction::title)
                .containsExactly("Bloquer le lot 4471 en magasin");
    }

    @Test
    void suggestActions_preventive_withDescription_skipsShort_splitsLong_andCaps() {
        UUID id = UUID.randomUUID();
        CapaCase c = new CapaCase();
        c.setTenantId(TENANT);
        c.setTitle("Risque de dérive du procédé de collage");
        c.setDescription("Contexte détaillé non vide");   // branche description != null && !blank
        c.setType(CapaType.PREVENTIVE);                    // branche PREVENTIVE
        c.setCriticity(CapaCriticity.LOW);
        c.setStatus(CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(c));

        String longLine = "Mettre en place une surveillance renforcée " + "x".repeat(300);
        StringBuilder sb = new StringBuilder();
        sb.append("ok\n");                                 // < 5 caractères -> ignorée
        sb.append(longLine).append("\n");                  // > 255 -> title tronqué, description = ligne complète
        for (int i = 0; i < 10; i++) {                     // dépasse le plafond (8)
            sb.append("Action préventive numéro ").append(i).append("\n");
        }
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult(sb.toString(), "ollama", 90, 1100));

        List<CapaDto.SuggestedAction> res = service.suggestActions(id);

        assertThat(res).hasSize(8);                        // plafonné
        assertThat(res.get(0).title()).hasSize(255);       // ligne longue tronquée
        assertThat(res.get(0).description()).isEqualTo(longLine);
        assertThat(res).noneMatch(a -> a.title().equals("ok"));
    }

    @Test
    void suggestActions_nullLlmText_returnsEmpty() {
        UUID id = UUID.randomUUID();
        CapaCase c = new CapaCase();
        c.setTenantId(TENANT);
        c.setTitle("Pb");
        c.setType(CapaType.CORRECTIVE);
        c.setCriticity(CapaCriticity.MEDIUM);
        c.setStatus(CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.of(c));
        when(ai.complete(any(), any(), anyInt()))
                .thenReturn(new AiCompletionResult(null, "ollama", 0, 10));

        assertThat(service.suggestActions(id)).isEmpty();
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
                "fix", "desc", null, null, UUID.randomUUID(), null, null, LocalDate.now().plusDays(5));
        CapaDto.ActionResponse r = service.addAction(c.getId(), req);
        assertThat(r.status()).isEqualTo(CapaActionStatus.PENDING);
    }

    @Test
    void addAction_explicitStatus_used() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CapaDto.ActionRequest req = new CapaDto.ActionRequest(
                "fix", null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null);
        CapaDto.ActionResponse r = service.addAction(c.getId(), req);
        assertThat(r.status()).isEqualTo(CapaActionStatus.IN_PROGRESS);
    }

    @Test
    void addAction_closed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null)))
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
                new CapaDto.ActionRequest("t", null, CapaActionStatus.DONE, null, null, null, null, null));

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
                new CapaDto.ActionRequest(null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null));

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
                new CapaDto.ActionRequest("nt", "nd", null, null, assignee, null, null, due));
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
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null)))
                .isInstanceOf(CapaActionNotFoundException.class);
    }

    @Test
    void updateAction_capaClosed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateAction(c.getId(), UUID.randomUUID(),
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null)))
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

    // --- verrou de clôture sur les non-conformités liées ------------------------
    // Clore une CAPA au-dessus d'un écart encore ouvert reviendrait à déclarer le
    // problème réglé pendant que le constat dit le contraire.

    @Test
    void refuse_deCloreQuandUneNcLieeResteOuverte() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(eq(TENANT), eq(c.getId()), any()))
                .thenReturn(2L);

        assertThatThrownBy(() -> service.verifyEffectiveness(
                c.getId(), new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("2");

        // Rien n'a bougé : ni le statut, ni la date de clôture, ni le journal.
        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        assertThat(c.getClosedAt()).isNull();
        verifyNoInteractions(journal);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void cloture_quandToutesLesNcLieesSontRefermees() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(eq(TENANT), eq(c.getId()), any()))
                .thenReturn(0L);
        when(caseRepo.save(any())).thenReturn(c);

        CapaDto.CaseResponse r = service.verifyEffectiveness(
                c.getId(), new CapaDto.EffectivenessRequest(true));

        assertThat(r.status()).isEqualTo(CapaStatus.CLOSED);
    }

    @Test
    void neVerrouillePas_uneEfficaciteNonDemontree() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        // Le dossier repart en traitement : il ne se referme pas, donc le verrou
        // ne s'applique pas — et c'est CELA que le test surveille.
        //
        // Il ne peut plus le surveiller en exigeant zéro appel au dépôt des NC :
        // le même décompte sert désormais à ÉNONCER les obstacles restants sur la
        // fiche renvoyée, ce qui est un usage tout différent du verrou. Compter
        // les appels confondrait « le verrou s'est appliqué » avec « l'écran sait
        // quoi afficher ». Le statut, lui, ne dit qu'une chose.
        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
        assertThat(c.getClosedAt()).isNull();
    }

    // --- journal du cycle de vie (§11.5) ---------------------------------------
    // Le dossier consignait le versement d'une preuve mais pas la décision qui
    // clôt le cas. Un dossier d'audit qui dit QUI a joint une pièce sans dire QUI
    // l'a clos raconte l'accessoire et tait l'essentiel.

    @Test
    void ouverture_estConsignee() {
        CapaCase saved = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.save(any())).thenReturn(saved);

        service.createCase(req());

        verify(journal).record(saved, CapaTransition.OPENED);
    }

    @Test
    void demarrage_estConsigne() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.startCase(c.getId());

        verify(journal).record(c, CapaTransition.STARTED);
    }

    @Test
    void resolution_estConsignee() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.getActions().add(action(c, CapaActionStatus.DONE));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.resolveCase(c.getId());

        verify(journal).record(c, CapaTransition.RESOLVED);
    }

    @Test
    void cloture_surEfficaciteDemontree_estConsignee() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(true));

        verify(journal).record(c, CapaTransition.CLOSED);
    }

    @Test
    void efficaciteNonDemontree_estConsigneeCommeTelle() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        // Le dossier repart en traitement : dire « clos » ici serait faux, et
        // taire l'événement masquerait une action corrective sans effet.
        verify(journal).record(c, CapaTransition.EFFECTIVENESS_REJECTED);
    }

    @Test
    void rejet_estConsigne() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.rejectCase(c.getId());

        verify(journal).record(c, CapaTransition.REJECTED);
    }

    @Test
    void suppression_estConsigneeAvantEffacement() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        service.deleteCase(c.getId());

        // L'ordre compte : après l'effacement, il ne reste rien à décrire.
        InOrder order = inOrder(journal, caseRepo);
        order.verify(journal).record(c, CapaTransition.DELETED);
        order.verify(caseRepo).delete(c);
    }

    @Test
    void unRefusNeLaisseAucuneTrace() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.startCase(c.getId()))
                .isInstanceOf(CapaStateException.class);

        // Une transition refusée n'a pas eu lieu : la consigner mentirait.
        verifyNoInteractions(journal);
    }

    // --- colonnes du tableau des actions (ADR 0052) ----------------------------
    // Ce qui se teste ici n'est pas « le champ est-il stocké » — c'est que chaque
    // colonne dit ce qu'elle prétend dire : la date est celle de la DÉCISION et
    // non de la saisie, le nom du porteur est celui figé à la décision, et le
    // libellé ne peut pas être vidé par une édition en ligne.

    @Test
    void addAction_dateDeDecisionSaisie_estConservee() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LocalDate comite = LocalDate.of(2026, 3, 12);

        CapaDto.ActionResponse r = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Réviser le plan de contrôle", null, null, null, null, "Amina Dridi", comite, null));

        // Une action décidée en comité le 12 mars et saisie plus tard porte le
        // 12 mars : c'est toute la raison d'être de la colonne.
        assertThat(r.decidedOn()).isEqualTo(comite);
        assertThat(r.assigneeName()).isEqualTo("Amina Dridi");
    }

    @Test
    void addAction_sansDateDeDecision_deduitLeJour_maisPasDepuisCreatedAt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(),
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null));

        // Déduction EXPLICITE au jour de l'enregistrement, et corrigeable ensuite.
        // Le champ existe indépendamment de createdAt : l'entité l'a reçu avant
        // toute persistance, là où createdAt n'est posé qu'au @PrePersist.
        assertThat(r.decidedOn()).isEqualTo(LocalDate.now());
        assertThat(r.createdAt()).isNull();
    }

    @Test
    void addAction_nomDePorteurVide_estRamèneANull_plutotQueDAfficherDuBlanc() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(),
                new CapaDto.ActionRequest("x", null, null, null, null, "   ", null, null));

        // Sinon la colonne « Responsable » serait vide sans le « — » qui dit
        // qu'elle l'est : l'écran n'aurait plus qu'une seule chose à tester.
        assertThat(r.assigneeName()).isNull();
    }

    @Test
    void addAction_titreFaitDEspaces_estRefuse() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("   ", null, null, null, null, null, null, null)))
                .isInstanceOf(CapaValidationException.class);
        verify(actionRepo, never()).save(any());
    }

    @Test
    void addAction_titreTropLong_estRefuseSansTronquer() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("x".repeat(256), null, null, null, null, null, null, null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("255");
        // Tronquer serait pire : l'utilisateur croirait avoir enregistré ce
        // qu'il a tapé.
        verify(actionRepo, never()).save(any());
    }

    @Test
    void updateAction_editionEnLigne_changeLeLibelleEtLeStatut() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest("  Libellé corrigé  ", null,
                        CapaActionStatus.IN_PROGRESS, null, null, null, null, null));

        // Les espaces de bordure d'un champ de tableau viennent du copier-coller,
        // pas de l'intention : les garder ferait diverger deux libellés identiques.
        assertThat(r.title()).isEqualTo("Libellé corrigé");
        assertThat(r.status()).isEqualTo(CapaActionStatus.IN_PROGRESS);
    }

    @Test
    void updateAction_libelleVide_estRefuse_etNeVidePasLAction() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest(" ", null, null, null, null, null, null, null)))
                .isInstanceOf(CapaValidationException.class);

        // Le PATCH n'est pas validé par Jakarta (un champ absent doit rester
        // intouché) : sans le garde-fou du service, l'édition en ligne effacerait
        // le libellé sur un simple espace.
        assertThat(a.getTitle()).isEqualTo("act");
        verify(actionRepo, never()).save(any());
    }

    @Test
    void updateAction_neTouchePasCeQuiNEstPasEnvoye() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        a.setAssigneeName("Amina Dridi");
        a.setDecidedOn(LocalDate.of(2026, 3, 12));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, CapaActionStatus.DONE, null, null, null, null, null));

        assertThat(a.getAssigneeName()).isEqualTo("Amina Dridi");
        assertThat(a.getDecidedOn()).isEqualTo(LocalDate.of(2026, 3, 12));
    }

    @Test
    void deleteAction_refuseDEffacerUneActionQuiPorteUnePreuve() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(evidenceRepo.countByTenantIdAndActionId(TENANT, a.getId())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteAction(c.getId(), a.getId()))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("remove it before deleting the action");

        // La cascade de base effacerait la ligne mais laisserait le binaire
        // orphelin — et surtout, une preuve d'audit disparaîtrait sans trace.
        verify(actionRepo, never()).delete(any());
    }

    @Test
    void deleteAction_effaceUneActionSansPreuve() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(evidenceRepo.countByTenantIdAndActionId(TENANT, a.getId())).thenReturn(0L);

        service.deleteAction(c.getId(), a.getId());

        verify(actionRepo).delete(a);
    }

    // --- non-conformité d'origine ------------------------------------------------

    @Test
    void findById_porteLaNonConformiteDOrigine_parSonLienReel() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(TENANT, c.getId()))
                .thenReturn(Optional.of(nc("NC-2026-0018", "Étiquetage lot 4471 illisible")));

        CapaDto.CaseResponse r = service.findById(c.getId());

        assertThat(r.sourceNonConformity()).isNotNull();
        assertThat(r.sourceNonConformity().reference()).isEqualTo("NC-2026-0018");
        assertThat(r.sourceNonConformity().title()).isEqualTo("Étiquetage lot 4471 illisible");
    }

    @Test
    void findById_retombeSurLaReferenceSaisie_quandAucuneNcNePointeVersLeDossier() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setSourceRef(" NC-2026-0018 ");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(TENANT, c.getId()))
                .thenReturn(Optional.empty());
        when(ncRepo.findByTenantIdAndReference(TENANT, "NC-2026-0018"))
                .thenReturn(Optional.of(nc("NC-2026-0018", "Étiquetage lot 4471 illisible")));

        assertThat(service.findById(c.getId()).sourceNonConformity().title())
                .isEqualTo("Étiquetage lot 4471 illisible");
    }

    @Test
    void findById_neMontreRien_quandLaReferenceSaisieNeDesigneAucunEcart() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setSourceRef("NC-INEXISTANTE");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(TENANT, c.getId()))
                .thenReturn(Optional.empty());
        when(ncRepo.findByTenantIdAndReference(TENANT, "NC-INEXISTANTE")).thenReturn(Optional.empty());

        // Une référence tapée à la main peut désigner un écart qui n'existe pas :
        // ne rien montrer vaut mieux qu'afficher un nom inventé.
        assertThat(service.findById(c.getId()).sourceNonConformity()).isNull();
    }

    @Test
    void findById_neChercheAucunEcart_quandLaSourceNEnEstPasUn() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setSourceType(CapaSourceType.AUDIT);
        c.setSourceRef("AUD-2026-Q2");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(TENANT, c.getId()))
                .thenReturn(Optional.empty());

        assertThat(service.findById(c.getId()).sourceNonConformity()).isNull();
        verify(ncRepo, never()).findByTenantIdAndReference(any(), any());
    }

    @Test
    void findAll_neResoutAucunEcart_pourNePasFaireUneRequeteParLigne() {
        Pageable p = PageRequest.of(0, 20);
        when(caseRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.OPEN))));

        Page<CapaDto.CaseResponse> page = service.findAll(null, p);

        assertThat(page.getContent().get(0).sourceNonConformity()).isNull();
        // Vingt dossiers vaudraient vingt requêtes pour une colonne que la liste
        // n'affiche même pas.
        verifyNoInteractions(ncRepo);
    }

    private com.openlab.qualitos.quality.nonconformity.NonConformity nc(String ref, String title) {
        var n = new com.openlab.qualitos.quality.nonconformity.NonConformity();
        n.setId(UUID.randomUUID());
        n.setTenantId(TENANT);
        n.setReference(ref);
        n.setTitle(title);
        return n;
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
        return action(c, status, CapaActionType.CORRECTIVE);
    }

    private CapaAction action(CapaCase c, CapaActionStatus status, CapaActionType type) {
        CapaAction a = new CapaAction();
        a.setId(UUID.randomUUID());
        a.setCapa(c);
        a.setTitle("act");
        a.setStatus(status);
        a.setActionType(type);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        return a;
    }

    // ============================================================================
    // Nature des actions : endiguement / correction / prévention (§4.2)
    // ============================================================================

    @Test
    void addAction_withoutType_isCorrective() {
        // C'est ce que « une action de CAPA » veut dire par défaut ; exiger la
        // qualification à chaque saisie ferait payer tout le monde pour le cas rare.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(),
                new CapaDto.ActionRequest("tri du lot", null, null, null, null, null, null, null));

        assertThat(r.actionType()).isEqualTo(CapaActionType.CORRECTIVE);
    }

    @Test
    void addAction_withContainment_keepsIt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "tri du lot suspect", null, null, CapaActionType.CONTAINMENT,
                null, null, null, null));

        assertThat(r.actionType()).isEqualTo(CapaActionType.CONTAINMENT);
    }

    @Test
    void updateAction_absentType_leavesItUntouched() {
        // PATCH : un champ absent ne se touche pas. Sans ce comportement, corriger
        // une échéance requalifierait l'action en corrective.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, null, null, null, null, null, LocalDate.now().plusDays(3)));

        assertThat(a.getActionType()).isEqualTo(CapaActionType.CONTAINMENT);
    }

    @Test
    void updateAction_newType_requalifiesTheAction() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, null, CapaActionType.PREVENTIVE, null, null, null, null));

        assertThat(a.getActionType()).isEqualTo(CapaActionType.PREVENTIVE);
    }

    @Test
    void verifyEffectiveness_actionAddedAfterResolution_blocksClosure() {
        // Rien n'interdit d'ajouter une action à un dossier RESOLVED. Sans ce
        // contrôle, le dossier se cloturait au-dessus d'une action jamais menée —
        // et l'écran, qui annonce l'obstacle, interdisait un geste que l'API
        // accordait quand même.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.getActions().add(action(c, CapaActionStatus.DONE));
        c.getActions().add(action(c, CapaActionStatus.PENDING));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("not DONE");

        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void verifyEffectiveness_pendingAction_stillAllowsAnIneffectiveVerdict() {
        // Constater l'échec ne clôt rien : l'interdire empêcherait de consigner
        // que les actions n'ont pas produit leur effet.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.getActions().add(action(c, CapaActionStatus.PENDING));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    @Test
    void verifyEffectiveness_containmentOnly_isRefused() {
        // Endiguer n'est pas corriger : la cause reste, et le problème reviendra
        // dès la mesure levée. Clore là-dessus inscrirait au registre le contraire
        // de ce qui s'est passé.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.getActions().add(action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT));
        c.getActions().add(action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("containment");

        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void verifyEffectiveness_oneCorrectiveAmongContainments_closes() {
        // Une seule action qui s'attaque à la cause suffit : le dossier n'a pas
        // QUE contenu, et rien ne justifie d'exiger davantage.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.getActions().add(action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT));
        c.getActions().add(action(c, CapaActionStatus.DONE, CapaActionType.CORRECTIVE));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(true));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
    }

    @Test
    void verifyEffectiveness_containmentOnly_butIneffective_isAllowed() {
        // Déclarer l'action INEFFICACE ne clôt rien : c'est un constat d'échec, et
        // le refuser interdirait de consigner que l'endiguement n'a pas suffi.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.getActions().add(action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    @Test
    void isContainmentOnly_emptyList_isFalse() {
        // Un dossier sans action a son propre motif ; un allMatch sur une liste
        // vide répondrait « oui » à une question qui ne se pose pas.
        assertThat(CapaService.isContainmentOnly(List.of())).isFalse();
    }

    // ============================================================================
    // Motifs de blocage, énoncés AVANT le clic (§4.2)
    // ============================================================================

    @Test
    void findById_caseWithoutAction_reportsNoActionBlocker() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .extracting(CapaDto.ClosureBlocker::code)
                .containsExactly(ClosureBlockerCode.NO_ACTION);
    }

    @Test
    void findById_pendingActions_areCounted() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.getActions().add(action(c, CapaActionStatus.DONE));
        c.getActions().add(action(c, CapaActionStatus.PENDING));
        c.getActions().add(action(c, CapaActionStatus.IN_PROGRESS));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .containsExactly(new CapaDto.ClosureBlocker(ClosureBlockerCode.ACTIONS_NOT_DONE, 2));
    }

    @Test
    void findById_containmentOnly_isAnnouncedBeforeTheClick() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.getActions().add(action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT));
        c.getActions().add(action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .containsExactly(new CapaDto.ClosureBlocker(ClosureBlockerCode.CONTAINMENT_ONLY, 2));
    }

    @Test
    void findById_openNonConformities_areCounted() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.getActions().add(action(c, CapaActionStatus.DONE));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(3L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .containsExactly(new CapaDto.ClosureBlocker(ClosureBlockerCode.OPEN_NON_CONFORMITIES, 3));
    }

    @Test
    void findById_severalObstacles_areAllListed() {
        // L'écran doit pouvoir tout dire d'un coup : ne montrer que le premier
        // obstacle enverrait l'utilisateur en corriger un pour en découvrir un autre.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.getActions().add(action(c, CapaActionStatus.PENDING, CapaActionType.CONTAINMENT));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(1L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .extracting(CapaDto.ClosureBlocker::code)
                .containsExactly(ClosureBlockerCode.ACTIONS_NOT_DONE,
                        ClosureBlockerCode.CONTAINMENT_ONLY,
                        ClosureBlockerCode.OPEN_NON_CONFORMITIES);
    }

    @Test
    void findById_nothingInTheWay_returnsEmptyRatherThanNull() {
        // Une liste vide DIT quelque chose : « rien ne s'y oppose ». Un null
        // obligerait l'écran à distinguer « pas calculé » de « pas d'obstacle ».
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.getActions().add(action(c, CapaActionStatus.DONE));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers()).isEmpty();
    }

    @Test
    void findById_closedCase_announcesNoObstacle() {
        // Afficher des obstacles sur un dossier terminé donnerait à croire qu'il
        // reste quelque chose à faire.
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThat(service.findById(c.getId()).closureBlockers()).isEmpty();
        // Et sans même interroger les non-conformités : la réponse est connue
        // d'avance, un dossier clos n'attend plus rien. (Le dépôt reste sollicité
        // par la résolution de l'écart d'origine, qui est un autre usage.)
        verify(ncRepo, never()).countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any());
    }

    @Test
    void findAll_doesNotComputeBlockers() {
        // Vingt dossiers vaudraient vingt requêtes pour une information que la
        // liste n'affiche pas.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByTenantId(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(c)));

        assertThat(service.findAll(null, PageRequest.of(0, 20)).getContent().get(0).closureBlockers())
                .isNull();
        verifyNoInteractions(ncRepo);
    }
}
