package com.openlab.qualitos.quality.pdca;

import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Golden-master du chemin doré PDCA (roue de Deming).
 *
 * Verrouille le scénario nominal complet:
 *   create (PLAN) → addStep(PLAN, DONE) → advance ×4 → COMPLETED.
 *
 * Invariants de référence (régression):
 *   - création toujours en PLAN ;
 *   - séquence d'états stricte PLAN → DO → CHECK → ACT → COMPLETED ;
 *   - completedAt renseigné à la transition finale ;
 *   - les étapes non DONE de la phase courante bloquent l'avancement (409) ;
 *   - aucune transition illégale possible après COMPLETED.
 *
 * Pattern: slice service Mockito (cohérent avec PdcaServiceTest, sans Docker / Spring context).
 * Une seule instance de cycle est filée dans tout le flux via thenReturn(sameInstance),
 * ce qui reproduit fidèlement la mutation transactionnelle réelle.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PDCA — Golden Path (Deming PLAN→DO→CHECK→ACT→COMPLETED)")
class PdcaGoldenPathTest {

    @Mock
    private PdcaCycleRepository cycleRepository;

    @Mock
    private PdcaStepRepository stepRepository;

    @InjectMocks
    private PdcaService pdcaService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    @BeforeEach
    void setTenantContext() {
        TenantContext.setTenantId(TENANT_ID.toString());
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create → step DONE → advance ×4 → COMPLETED (séquence verrouillée)")
    void goldenPath_fullDemingCycle() {
        // --- 1. CREATE : toujours en PLAN ---
        // save() assigne l'id/horodatage à l'entité construite par le service et la renvoie
        // (le service y a déjà posé status=PLAN). On capture cette instance pour la filer ensuite.
        PdcaCycle[] holder = new PdcaCycle[1];
        when(cycleRepository.save(any(PdcaCycle.class))).thenAnswer(inv -> {
            PdcaCycle c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(UUID.randomUUID());
                c.setCreatedAt(Instant.now());
            }
            c.setUpdatedAt(Instant.now());
            holder[0] = c;
            return c;
        });

        PdcaDto.CycleResponse created = pdcaService.createCycle(
                new PdcaDto.CreateCycleRequest("Réduction des défauts soudure", "MVP", OWNER_ID));

        assertThat(created.status()).isEqualTo(PdcaStatus.PLAN);
        assertThat(created.tenantId()).isEqualTo(TENANT_ID);
        assertThat(created.completedAt()).isNull();

        PdcaCycle cycle = holder[0];

        // --- 2. SUB-RESOURCE : ajout d'une étape de la phase PLAN, complétée (DONE) ---
        // Le service lit cycle.getSteps() lors de l'advance : on synchronise la collection
        // parent dans le stub save() (en réel, le cascade JPA s'en charge).
        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));
        when(stepRepository.save(any(PdcaStep.class))).thenAnswer(inv -> {
            PdcaStep s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            cycle.getSteps().add(s); // synchronisation collection parent
            return s;
        });

        PdcaDto.StepResponse step = pdcaService.addStep(cycle.getId(),
                new PdcaDto.StepRequest("Analyser Pareto des défauts", "desc",
                        PdcaPhase.PLAN, StepStatus.DONE, OWNER_ID, LocalDate.now().plusDays(7)));

        assertThat(step.phase()).isEqualTo(PdcaPhase.PLAN);
        assertThat(step.status()).isEqualTo(StepStatus.DONE);
        assertThat(cycle.getSteps()).hasSize(1);

        // --- 3. ADVANCE ×4 : PLAN → DO → CHECK → ACT → COMPLETED ---
        when(cycleRepository.save(cycle)).thenReturn(cycle);

        assertThat(pdcaService.advanceCycle(cycle.getId()).status()).isEqualTo(PdcaStatus.DO);
        assertThat(pdcaService.advanceCycle(cycle.getId()).status()).isEqualTo(PdcaStatus.CHECK);
        assertThat(pdcaService.advanceCycle(cycle.getId()).status()).isEqualTo(PdcaStatus.ACT);

        PdcaDto.CycleResponse completed = pdcaService.advanceCycle(cycle.getId());

        // --- 4. RÉSULTAT ATTENDU : état final + horodatage de clôture ---
        assertThat(completed.status()).isEqualTo(PdcaStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();
        assertThat(cycle.getCompletedAt()).isNotNull();

        // --- 5. INVARIANT : aucune transition illégale après COMPLETED ---
        assertThatThrownBy(() -> pdcaService.advanceCycle(cycle.getId()))
                .isInstanceOf(PdcaStateException.class);
    }

    @Test
    @DisplayName("invariant — étape PLAN non DONE bloque l'avancement (409 métier)")
    void goldenPath_pendingStepBlocksAdvance() {
        PdcaCycle cycle = new PdcaCycle();
        cycle.setId(UUID.randomUUID());
        cycle.setTenantId(TENANT_ID);
        cycle.setStatus(PdcaStatus.PLAN);
        cycle.setOwnerId(OWNER_ID);
        cycle.setCreatedAt(Instant.now());
        cycle.setUpdatedAt(Instant.now());

        PdcaStep pending = new PdcaStep();
        pending.setId(UUID.randomUUID());
        pending.setCycle(cycle);
        pending.setPhase(PdcaPhase.PLAN);
        pending.setStatus(StepStatus.PENDING);
        cycle.getSteps().add(pending);

        when(cycleRepository.findByIdAndTenantId(cycle.getId(), TENANT_ID))
                .thenReturn(Optional.of(cycle));

        assertThatThrownBy(() -> pdcaService.advanceCycle(cycle.getId()))
                .isInstanceOf(PdcaStateException.class)
                .hasMessageContaining("DONE");

        // L'état n'a pas changé : toujours en PLAN.
        assertThat(cycle.getStatus()).isEqualTo(PdcaStatus.PLAN);
    }
}
