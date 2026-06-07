package com.openlab.qualitos.quality.dmaic;

import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Golden-master du chemin doré DMAIC + capabilité.
 *
 * Verrouille le scénario nominal complet:
 *   create (DEFINE/ACTIVE) → addMeasure ×5 (en MEASURE) → computeCapability
 *     → advance ×4 (DEFINE→MEASURE→ANALYZE→IMPROVE→CONTROL) → advance final → COMPLETED.
 *
 * Invariants de référence (régression):
 *   - création toujours en phase DEFINE, statut ACTIVE ;
 *   - séquence stricte des phases DEFINE→MEASURE→ANALYZE→IMPROVE→CONTROL ;
 *   - 5ᵉ avancement (depuis CONTROL) clôture le projet (COMPLETED + completedAt) ;
 *   - la capabilité est réellement calculée (CapabilityCalculator non mocké) :
 *     mesures {12,13,14,15,16}, LSL=10 USL=20 → mean=14.0, Cpk non nul ;
 *   - avancement interdit hors statut ACTIVE (ici après COMPLETED) → 409 métier.
 *
 * Pattern: slice service Mockito (cohérent avec DmaicServiceTest) avec le VRAI
 * CapabilityCalculator (@Spy) pour exercer le calcul Cp/Cpk de bout en bout, sans Docker.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DMAIC — Golden Path (DEFINE→…→CONTROL→COMPLETED + capabilité réelle)")
class DmaicGoldenPathTest {

    @Mock DmaicProjectRepository projectRepo;
    @Mock ProcessMeasureRepository measureRepo;
    @Mock PokaYokeDeviceRepository deviceRepo;
    @Mock PokaYokeAssignmentRepository assignmentRepo;
    @Spy CapabilityCalculator calculator = new CapabilityCalculator();
    @InjectMocks DmaicService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID BLACK_BELT = UUID.randomUUID();

    // Mesures déterministes en spec : moyenne 14.0, écart-type non nul.
    private static final double[] VALUES = {12.0, 13.0, 14.0, 15.0, 16.0};

    @BeforeEach
    void ctx() {
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clr() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create → measures → capability (réelle) → advance ×5 → COMPLETED")
    void goldenPath_fullDmaicProject() {
        // --- 1. CREATE : phase DEFINE, statut ACTIVE ---
        DmaicProject project = new DmaicProject();
        when(projectRepo.save(any(DmaicProject.class))).thenAnswer(inv -> {
            DmaicProject p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
                p.setCreatedAt(Instant.now());
            }
            p.setUpdatedAt(Instant.now());
            return p;
        });

        DmaicDto.ProjectResponse created = service.createProject(new DmaicDto.CreateProjectRequest(
                "Réduire défauts soudure", "trop de NC", "Cpk >= 1.33", BLACK_BELT, null, null,
                10.0, 20.0, 15.0, "mm", 50000.0));

        assertThat(created.phase()).isEqualTo(DmaicPhase.DEFINE);
        assertThat(created.status()).isEqualTo(DmaicStatus.ACTIVE);
        assertThat(created.tenantId()).isEqualTo(TENANT);

        // Instance stable filée dans tout le flux (spécifications conservées).
        project.setId(created.id());
        project.setTenantId(TENANT);
        project.setTitle("Réduire défauts soudure");
        project.setBlackBeltId(BLACK_BELT);
        project.setPhase(DmaicPhase.DEFINE);
        project.setStatus(DmaicStatus.ACTIVE);
        project.setSpecLowerLimit(10.0);
        project.setSpecUpperLimit(20.0);
        project.setSpecTarget(15.0);
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());
        when(projectRepo.findByIdAndTenantId(project.getId(), TENANT)).thenReturn(Optional.of(project));
        when(projectRepo.save(project)).thenReturn(project);

        // --- 2. ADVANCE DEFINE → MEASURE (les mesures se prennent en phase MEASURE) ---
        assertThat(service.advancePhase(project.getId()).phase()).isEqualTo(DmaicPhase.MEASURE);

        // --- 3. ADD MEASURE ×5 ---
        List<ProcessMeasure> persistedMeasures = new ArrayList<>();
        when(measureRepo.save(any(ProcessMeasure.class))).thenAnswer(inv -> {
            ProcessMeasure m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            persistedMeasures.add(m);
            return m;
        });
        for (double v : VALUES) {
            DmaicDto.MeasureResponse mr = service.addMeasure(project.getId(),
                    new DmaicDto.AddMeasureRequest(v, "G1", "lot-2026-001", null, null, "ok"));
            assertThat(mr.value()).isEqualTo(v);
        }
        assertThat(persistedMeasures).hasSize(5);

        // --- 4. COMPUTE CAPABILITY (calcul réel via @Spy CapabilityCalculator) ---
        when(measureRepo.findByProjectIdOrderByRecordedAtAsc(project.getId()))
                .thenReturn(persistedMeasures);
        DmaicDto.CapabilityResponse cap = service.computeCapability(project.getId());

        assertThat(cap.sampleSize()).isEqualTo(5);
        assertThat(cap.mean()).isEqualTo(14.0d);          // moyenne verrouillée
        assertThat(cap.stdDev()).isNotNull().isGreaterThan(0d);
        assertThat(cap.cpk()).isNotNull();                // capabilité réellement calculée
        assertThat(cap.specLowerLimit()).isEqualTo(10.0d);
        assertThat(cap.specUpperLimit()).isEqualTo(20.0d);

        // --- 5. ADVANCE jusqu'à CONTROL puis clôture ---
        assertThat(service.advancePhase(project.getId()).phase()).isEqualTo(DmaicPhase.ANALYZE);
        assertThat(service.advancePhase(project.getId()).phase()).isEqualTo(DmaicPhase.IMPROVE);
        assertThat(service.advancePhase(project.getId()).phase()).isEqualTo(DmaicPhase.CONTROL);

        DmaicDto.ProjectResponse completed = service.advancePhase(project.getId());
        assertThat(completed.status()).isEqualTo(DmaicStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();

        // --- 6. INVARIANT : pas d'avancement après COMPLETED (409 métier) ---
        assertThatThrownBy(() -> service.advancePhase(project.getId()))
                .isInstanceOf(DmaicStateException.class);
    }

    @Test
    @DisplayName("invariant — mesures interdites hors statut ACTIVE (409 métier)")
    void goldenPath_measuresOnlyOnActiveProject() {
        DmaicProject project = new DmaicProject();
        project.setId(UUID.randomUUID());
        project.setTenantId(TENANT);
        project.setTitle("p");
        project.setBlackBeltId(BLACK_BELT);
        project.setPhase(DmaicPhase.CONTROL);
        project.setStatus(DmaicStatus.COMPLETED);
        project.setCreatedAt(Instant.now());
        project.setUpdatedAt(Instant.now());
        when(projectRepo.findByIdAndTenantId(project.getId(), TENANT)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> service.addMeasure(project.getId(),
                new DmaicDto.AddMeasureRequest(1.0, null, null, null, null, null)))
                .isInstanceOf(DmaicStateException.class);
    }
}
