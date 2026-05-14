package com.openlab.qualitos.quality.pdca;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdcaServiceTest {

    @Mock
    private PdcaCycleRepository cycleRepository;

    @Mock
    private PdcaStepRepository stepRepository;

    @InjectMocks
    private PdcaService pdcaService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID OTHER_TENANT_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    @BeforeEach
    void setTenantContext() {
        TenantContext.setTenantId(TENANT_ID.toString());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    // --- createCycle ---

    @Test
    void createCycle_success() {
        PdcaDto.CreateCycleRequest request = new PdcaDto.CreateCycleRequest(
                "Réduction des défauts", "Description du cycle", OWNER_ID);

        PdcaCycle saved = buildCycle(TENANT_ID, PdcaStatus.PLAN);
        saved.setTitle(request.title());
        saved.setDescription(request.description());
        when(cycleRepository.save(any())).thenReturn(saved);

        PdcaDto.CycleResponse response = pdcaService.createCycle(request);

        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
        assertThat(response.status()).isEqualTo(PdcaStatus.PLAN);
        assertThat(response.title()).isEqualTo("Réduction des défauts");
        verify(cycleRepository).save(any(PdcaCycle.class));
    }

    @Test
    void createCycle_missingTenantContext_throwsMissingTenantContextException() {
        TenantContext.clear();

        PdcaDto.CreateCycleRequest request = new PdcaDto.CreateCycleRequest(
                "Title", null, OWNER_ID);

        assertThatThrownBy(() -> pdcaService.createCycle(request))
                .isInstanceOf(MissingTenantContextException.class);

        verifyNoInteractions(cycleRepository);
    }

    // --- findAll ---

    @Test
    void findAll_paginated_noStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.PLAN);
        when(cycleRepository.findByTenantId(TENANT_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(cycle)));

        Page<PdcaDto.CycleResponse> result = pdcaService.findAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(cycleRepository).findByTenantId(TENANT_ID, pageable);
        verify(cycleRepository, never()).findByTenantIdAndStatus(any(), any(), any());
    }

    @Test
    void findAll_paginated_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.DO);
        when(cycleRepository.findByTenantIdAndStatus(TENANT_ID, PdcaStatus.DO, pageable))
                .thenReturn(new PageImpl<>(List.of(cycle)));

        Page<PdcaDto.CycleResponse> result = pdcaService.findAll(PdcaStatus.DO, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(PdcaStatus.DO);
        verify(cycleRepository).findByTenantIdAndStatus(TENANT_ID, PdcaStatus.DO, pageable);
    }

    // --- findById ---

    @Test
    void findById_found() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.PLAN);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));

        PdcaDto.CycleResponse response = pdcaService.findById(cycle.getId());

        assertThat(response.id()).isEqualTo(cycle.getId());
        assertThat(response.tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void findById_notFound_throwsCycleNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(cycleRepository.findByIdAndTenantId(unknownId, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdcaService.findById(unknownId))
                .isInstanceOf(PdcaCycleNotFoundException.class);
    }

    @Test
    void findById_wrongTenant_throwsCycleNotFoundException() {
        // Un cycle appartenant à un autre tenant n'est pas visible
        PdcaCycle otherCycle = buildCycle(OTHER_TENANT_ID, PdcaStatus.PLAN);
        when(cycleRepository.findByIdAndTenantId(otherCycle.getId(), TENANT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> pdcaService.findById(otherCycle.getId()))
                .isInstanceOf(PdcaCycleNotFoundException.class);
    }

    // --- advanceCycle ---

    @Test
    void advanceCycle_plan_to_do() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.PLAN);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(cycleRepository.save(cycle)).thenReturn(cycle);

        PdcaDto.CycleResponse response = pdcaService.advanceCycle(cycle.getId());

        assertThat(response.status()).isEqualTo(PdcaStatus.DO);
    }

    @Test
    void advanceCycle_do_to_check() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.DO);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(cycleRepository.save(cycle)).thenReturn(cycle);

        PdcaDto.CycleResponse response = pdcaService.advanceCycle(cycle.getId());

        assertThat(response.status()).isEqualTo(PdcaStatus.CHECK);
    }

    @Test
    void advanceCycle_check_to_act() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.CHECK);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(cycleRepository.save(cycle)).thenReturn(cycle);

        PdcaDto.CycleResponse response = pdcaService.advanceCycle(cycle.getId());

        assertThat(response.status()).isEqualTo(PdcaStatus.ACT);
    }

    @Test
    void advanceCycle_act_to_completed() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.ACT);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(cycleRepository.save(cycle)).thenReturn(cycle);

        PdcaDto.CycleResponse response = pdcaService.advanceCycle(cycle.getId());

        assertThat(response.status()).isEqualTo(PdcaStatus.COMPLETED);
        assertThat(cycle.getCompletedAt()).isNotNull();
    }

    @Test
    void advanceCycle_alreadyCompleted_throwsStateException() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.COMPLETED);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));

        assertThatThrownBy(() -> pdcaService.advanceCycle(cycle.getId()))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void advanceCycle_cancelled_throwsStateException() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.CANCELLED);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));

        assertThatThrownBy(() -> pdcaService.advanceCycle(cycle.getId()))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("Cancelled");
    }

    @Test
    void advanceCycle_stepsNotAllDone_throwsStateException() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.PLAN);
        PdcaStep pendingStep = buildStep(cycle, PdcaPhase.PLAN, StepStatus.PENDING);
        cycle.getSteps().add(pendingStep);

        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));

        assertThatThrownBy(() -> pdcaService.advanceCycle(cycle.getId()))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("DONE");
    }

    // --- cancelCycle ---

    @Test
    void cancelCycle_success() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.DO);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(cycleRepository.save(cycle)).thenReturn(cycle);

        PdcaDto.CycleResponse response = pdcaService.cancelCycle(cycle.getId());

        assertThat(response.status()).isEqualTo(PdcaStatus.CANCELLED);
    }

    @Test
    void cancelCycle_alreadyCompleted_throwsStateException() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.COMPLETED);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));

        assertThatThrownBy(() -> pdcaService.cancelCycle(cycle.getId()))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("Completed");
    }

    // --- addStep ---

    @Test
    void addStep_success() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.PLAN);
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));

        PdcaStep step = buildStep(cycle, PdcaPhase.PLAN, StepStatus.PENDING);
        when(stepRepository.save(any())).thenReturn(step);

        PdcaDto.StepRequest request = new PdcaDto.StepRequest(
                "Analyser les défauts", "Description", PdcaPhase.PLAN,
                null, UUID.randomUUID(), LocalDate.now().plusDays(7));

        PdcaDto.StepResponse response = pdcaService.addStep(cycle.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.phase()).isEqualTo(PdcaPhase.PLAN);
        verify(stepRepository).save(any(PdcaStep.class));
    }

    @Test
    void addStep_cycleNotFound_throwsCycleNotFoundException() {
        UUID unknownCycleId = UUID.randomUUID();
        when(cycleRepository.findByIdAndTenantId(unknownCycleId, TENANT_ID))
                .thenReturn(Optional.empty());

        PdcaDto.StepRequest request = new PdcaDto.StepRequest(
                "Title", null, PdcaPhase.PLAN, null, null, null);

        assertThatThrownBy(() -> pdcaService.addStep(unknownCycleId, request))
                .isInstanceOf(PdcaCycleNotFoundException.class);
    }

    // --- updateStep ---

    @Test
    void updateStep_success() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.PLAN);
        PdcaStep step = buildStep(cycle, PdcaPhase.PLAN, StepStatus.PENDING);

        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(stepRepository.findByIdAndCycleId(step.getId(), cycle.getId()))
                .thenReturn(Optional.of(step));
        when(stepRepository.save(step)).thenReturn(step);

        UUID newAssignee = UUID.randomUUID();
        PdcaDto.StepRequest request = new PdcaDto.StepRequest(
                "Nouveau titre", "Nouvelle description", PdcaPhase.PLAN,
                StepStatus.IN_PROGRESS, newAssignee, LocalDate.now().plusDays(14));

        PdcaDto.StepResponse response = pdcaService.updateStep(cycle.getId(), step.getId(), request);

        assertThat(step.getTitle()).isEqualTo("Nouveau titre");
        assertThat(step.getStatus()).isEqualTo(StepStatus.IN_PROGRESS);
        assertThat(step.getAssigneeId()).isEqualTo(newAssignee);
        assertThat(response).isNotNull();
    }

    // --- deleteStep ---

    @Test
    void deleteStep_success_phaseNotActive() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.DO);
        // Étape en phase PLAN (phase précédente, donc pas active)
        PdcaStep step = buildStep(cycle, PdcaPhase.PLAN, StepStatus.DONE);

        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(stepRepository.findByIdAndCycleId(step.getId(), cycle.getId()))
                .thenReturn(Optional.of(step));

        pdcaService.deleteStep(cycle.getId(), step.getId());

        verify(stepRepository).delete(step);
    }

    @Test
    void deleteStep_phaseActive_throwsStateException() {
        PdcaCycle cycle = buildCycle(TENANT_ID, PdcaStatus.PLAN);
        // Étape dans la phase courante PLAN → on ne peut pas la supprimer
        PdcaStep step = buildStep(cycle, PdcaPhase.PLAN, StepStatus.PENDING);

        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(stepRepository.findByIdAndCycleId(step.getId(), cycle.getId()))
                .thenReturn(Optional.of(step));

        assertThatThrownBy(() -> pdcaService.deleteStep(cycle.getId(), step.getId()))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("active phase");
    }

    // --- helpers ---

    private PdcaCycle buildCycle(UUID tenantId, PdcaStatus status) {
        PdcaCycle cycle = new PdcaCycle();
        cycle.setId(UUID.randomUUID());
        cycle.setTenantId(tenantId);
        cycle.setTitle("Test Cycle");
        cycle.setDescription("Description");
        cycle.setStatus(status);
        cycle.setOwnerId(OWNER_ID);
        cycle.setCreatedAt(Instant.now());
        cycle.setUpdatedAt(Instant.now());
        return cycle;
    }

    private PdcaStep buildStep(PdcaCycle cycle, PdcaPhase phase, StepStatus status) {
        PdcaStep step = new PdcaStep();
        step.setId(UUID.randomUUID());
        step.setCycle(cycle);
        step.setPhase(phase);
        step.setTitle("Test Step");
        step.setStatus(status);
        step.setCreatedAt(Instant.now());
        step.setUpdatedAt(Instant.now());
        return step;
    }
}
