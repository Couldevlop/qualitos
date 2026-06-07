package com.openlab.qualitos.quality.ishikawa;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
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
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Golden-master du chemin doré Ishikawa (diagramme causes-effet 6M).
 *
 * Verrouille le scénario nominal complet:
 *   create (mode SIX_M, DRAFT) → addCause sur chacune des 6 branches 6M
 *     → vérifier les 6 branches peuplées → passage DRAFT→IN_REVIEW→VALIDATED.
 *
 * Invariants de référence (régression):
 *   - création par défaut/explicite en mode SIX_M, statut DRAFT ;
 *   - les 6 branches 6M sont exactement
 *     {METHODS, MANPOWER, MACHINES, MATERIALS, MEASUREMENTS, ENVIRONMENT} ;
 *   - une cause par branche → 6 causes, 6 catégories distinctes ;
 *   - une catégorie hors 6M (MANAGEMENT, propre au 7M) est refusée par le mode SIX_M (409 métier) ;
 *   - transitions de statut valides DRAFT→IN_REVIEW→VALIDATED.
 *
 * Pattern: slice service Mockito (cohérent avec les tests Ishikawa existants),
 * AiGatewayClient mocké mais NON sollicité par le chemin doré (suggestCauses hors scope).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ishikawa — Golden Path (6M : 6 branches + lifecycle)")
class IshikawaGoldenPathTest {

    @Mock IshikawaDiagramRepository diagramRepository;
    @Mock IshikawaCauseRepository causeRepository;
    @Mock AiGatewayClient ai;
    @InjectMocks IshikawaService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OWNER = UUID.randomUUID();

    private static final Set<CauseCategory> SIX_M = EnumSet.of(
            CauseCategory.METHODS, CauseCategory.MANPOWER, CauseCategory.MACHINES,
            CauseCategory.MATERIALS, CauseCategory.MEASUREMENTS, CauseCategory.ENVIRONMENT);

    @BeforeEach
    void ctx() {
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clr() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create SIX_M → 1 cause par branche 6M → 6 branches → DRAFT→IN_REVIEW→VALIDATED")
    void goldenPath_sixMDiagram() {
        // --- 1. CREATE : mode SIX_M, statut DRAFT ---
        IshikawaDiagram diagram = new IshikawaDiagram();
        when(diagramRepository.save(any(IshikawaDiagram.class))).thenAnswer(inv -> {
            IshikawaDiagram d = inv.getArgument(0);
            if (d.getId() == null) {
                d.setId(UUID.randomUUID());
                d.setCreatedAt(Instant.now());
            }
            d.setUpdatedAt(Instant.now());
            return d;
        });

        IshikawaDto.DiagramResponse created = service.createDiagram(new IshikawaDto.CreateDiagramRequest(
                "Taux de rebut élevé en finition", "MVP", IshikawaMode.SIX_M, OWNER));

        assertThat(created.mode()).isEqualTo(IshikawaMode.SIX_M);
        assertThat(created.status()).isEqualTo(IshikawaStatus.DRAFT);
        assertThat(created.tenantId()).isEqualTo(TENANT);

        // Instance stable filée dans le flux (le service lit getMode() + getCauses()).
        diagram.setId(created.id());
        diagram.setTenantId(TENANT);
        diagram.setProblemStatement("Taux de rebut élevé en finition");
        diagram.setMode(IshikawaMode.SIX_M);
        diagram.setStatus(IshikawaStatus.DRAFT);
        diagram.setOwnerId(OWNER);
        diagram.setCreatedAt(Instant.now());
        diagram.setUpdatedAt(Instant.now());
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT))
                .thenReturn(Optional.of(diagram));

        // addCause persiste via causeRepository.save ; on synchronise getCauses() (cascade JPA réel).
        when(causeRepository.save(any(IshikawaCause.class))).thenAnswer(inv -> {
            IshikawaCause c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            diagram.getCauses().add(c);
            return c;
        });

        // --- 2. ADD CAUSE : une par branche 6M ---
        for (CauseCategory cat : SIX_M) {
            IshikawaDto.CauseResponse cause = service.addCause(diagram.getId(),
                    new IshikawaDto.CauseRequest(cat, "Cause " + cat, "détail", null, 0.5));
            assertThat(cause.category()).isEqualTo(cat);
        }

        // --- 3. VÉRIFIER les 6 branches 6M peuplées ---
        assertThat(diagram.getCauses()).hasSize(6);
        Set<CauseCategory> branches = diagram.getCauses().stream()
                .map(IshikawaCause::getCategory).collect(Collectors.toSet());
        assertThat(branches).containsExactlyInAnyOrderElementsOf(SIX_M);

        // --- 4. INVARIANT : catégorie hors 6M refusée par le mode (MANAGEMENT = 7M) ---
        assertThatThrownBy(() -> service.addCause(diagram.getId(),
                new IshikawaDto.CauseRequest(CauseCategory.MANAGEMENT, "Pilotage", null, null, null)))
                .isInstanceOf(IshikawaStateException.class)
                .hasMessageContaining("not allowed");
        assertThat(diagram.getCauses()).hasSize(6); // inchangé

        // --- 5. LIFECYCLE : DRAFT → IN_REVIEW → VALIDATED ---
        IshikawaDto.DiagramResponse inReview = service.updateDiagram(diagram.getId(),
                new IshikawaDto.UpdateDiagramRequest(null, null, null, IshikawaStatus.IN_REVIEW));
        assertThat(inReview.status()).isEqualTo(IshikawaStatus.IN_REVIEW);

        IshikawaDto.DiagramResponse validated = service.updateDiagram(diagram.getId(),
                new IshikawaDto.UpdateDiagramRequest(null, null, null, IshikawaStatus.VALIDATED));
        assertThat(validated.status()).isEqualTo(IshikawaStatus.VALIDATED);
        assertThat(validated.causes()).hasSize(6);
    }

    @Test
    @DisplayName("invariant — un diagramme VALIDATED ne peut être supprimé (à archiver) (409 métier)")
    void goldenPath_validatedCannotBeDeleted() {
        IshikawaDiagram diagram = new IshikawaDiagram();
        diagram.setId(UUID.randomUUID());
        diagram.setTenantId(TENANT);
        diagram.setProblemStatement("p");
        diagram.setMode(IshikawaMode.SIX_M);
        diagram.setStatus(IshikawaStatus.VALIDATED);
        diagram.setOwnerId(OWNER);
        diagram.setCreatedAt(Instant.now());
        diagram.setUpdatedAt(Instant.now());
        when(diagramRepository.findByIdAndTenantId(diagram.getId(), TENANT))
                .thenReturn(Optional.of(diagram));

        assertThatThrownBy(() -> service.deleteDiagram(diagram.getId()))
                .isInstanceOf(IshikawaStateException.class);
    }
}
