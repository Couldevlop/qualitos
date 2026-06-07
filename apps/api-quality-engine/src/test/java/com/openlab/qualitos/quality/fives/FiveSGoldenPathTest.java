package com.openlab.qualitos.quality.fives;

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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Golden-master du chemin doré 5S (audit terrain).
 *
 * Verrouille le scénario nominal complet:
 *   create (DRAFT) → start (IN_PROGRESS) → score des 5 piliers → complete (COMPLETED).
 *
 * Invariants de référence (régression):
 *   - création toujours en DRAFT ;
 *   - start exige DRAFT ; complete exige IN_PROGRESS ;
 *   - score global = moyenne des piliers × 10 (0..10 par pilier → 0..100 global) ;
 *     scores {8,6,10,7,9} → moyenne 8.0 → overallScore = 80.0 (valeur verrouillée) ;
 *   - complétion impossible si les 5 piliers ne sont pas scorés (409 métier) ;
 *   - completedAt renseigné à la clôture.
 *
 * Pattern: slice service Mockito (cohérent avec FiveSServiceTest, sans Docker / Spring context).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("5S — Golden Path (DRAFT→IN_PROGRESS→score×5→COMPLETED)")
class FiveSGoldenPathTest {

    @Mock
    FiveSAuditRepository auditRepository;

    @Mock
    FiveSAuditItemRepository itemRepository;

    @InjectMocks
    FiveSService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID AUDITOR = UUID.randomUUID();

    // Scores déterministes, un par pilier (ordre enum SEIRI..SHITSUKE).
    private static final int[] SCORES = {8, 6, 10, 7, 9}; // moyenne = 8.0 → overall 80.0

    @BeforeEach
    void setCtx() {
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void clr() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create → start → score 5 piliers → complete : score global = 80.0")
    void goldenPath_fullAudit() {
        // --- 1. CREATE : toujours en DRAFT ---
        // save() renvoie l'entité construite par le service (status=DRAFT déjà posé) ;
        // on la capture pour la filer dans toute la suite du flux.
        FiveSAudit[] holder = new FiveSAudit[1];
        when(auditRepository.save(any(FiveSAudit.class))).thenAnswer(inv -> {
            FiveSAudit a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
                a.setCreatedAt(Instant.now());
            }
            a.setUpdatedAt(Instant.now());
            holder[0] = a;
            return a;
        });

        FiveSDto.AuditResponse created = service.createAudit(
                new FiveSDto.CreateAuditRequest("Atelier mécanique A", "MVP terrain", AUDITOR, Instant.now()));

        assertThat(created.status()).isEqualTo(FiveSAuditStatus.DRAFT);
        assertThat(created.tenantId()).isEqualTo(TENANT);

        FiveSAudit audit = holder[0];

        // --- 2. START : DRAFT → IN_PROGRESS ---
        when(auditRepository.findByIdAndTenantId(audit.getId(), TENANT)).thenReturn(Optional.of(audit));
        when(auditRepository.save(audit)).thenReturn(audit);

        assertThat(service.startAudit(audit.getId()).status()).isEqualTo(FiveSAuditStatus.IN_PROGRESS);

        // --- 3. SCORE des 5 piliers ---
        // scorePillar persiste via itemRepository.save sans toucher la collection parent ;
        // on synchronise audit.getItems() dans le stub (en réel : cascade JPA).
        when(itemRepository.save(any(FiveSAuditItem.class))).thenAnswer(inv -> {
            FiveSAuditItem it = inv.getArgument(0);
            it.setId(UUID.randomUUID());
            it.setCreatedAt(Instant.now());
            it.setUpdatedAt(Instant.now());
            audit.getItems().add(it);
            return it;
        });

        FiveSPillar[] pillars = FiveSPillar.values();
        for (int i = 0; i < pillars.length; i++) {
            when(itemRepository.findByAuditIdAndPillar(audit.getId(), pillars[i]))
                    .thenReturn(Optional.empty());
            FiveSDto.ItemResponse item = service.scorePillar(audit.getId(),
                    new FiveSDto.ScoreRequest(pillars[i], SCORES[i], "note " + pillars[i], null));
            assertThat(item.pillar()).isEqualTo(pillars[i]);
            assertThat(item.score()).isEqualTo(SCORES[i]);
        }
        assertThat(audit.getItems()).hasSize(5);

        // --- 4. COMPLETE : score global verrouillé = 80.0 ---
        FiveSDto.AuditResponse completed = service.completeAudit(audit.getId());

        assertThat(completed.status()).isEqualTo(FiveSAuditStatus.COMPLETED);
        assertThat(completed.overallScore()).isEqualTo(80.0d); // moyenne(8,6,10,7,9)=8.0 ×10
        assertThat(audit.getCompletedAt()).isNotNull();
        assertThat(completed.items()).hasSize(5);
    }

    @Test
    @DisplayName("invariant — complétion impossible si les 5 piliers ne sont pas scorés (409 métier)")
    void goldenPath_incompletePillarsBlocksCompletion() {
        FiveSAudit audit = new FiveSAudit();
        audit.setId(UUID.randomUUID());
        audit.setTenantId(TENANT);
        audit.setStatus(FiveSAuditStatus.IN_PROGRESS);
        audit.setAuditorId(AUDITOR);
        audit.setCreatedAt(Instant.now());
        audit.setUpdatedAt(Instant.now());

        // Un seul pilier scoré sur 5.
        FiveSAuditItem only = new FiveSAuditItem();
        only.setId(UUID.randomUUID());
        only.setAudit(audit);
        only.setPillar(FiveSPillar.SEIRI);
        only.setScore(8);
        audit.getItems().add(only);

        when(auditRepository.findByIdAndTenantId(audit.getId(), TENANT)).thenReturn(Optional.of(audit));

        assertThatThrownBy(() -> service.completeAudit(audit.getId()))
                .isInstanceOf(FiveSStateException.class)
                .hasMessageContaining("5 pillars");

        // L'audit n'a pas été clôturé.
        assertThat(audit.getStatus()).isEqualTo(FiveSAuditStatus.IN_PROGRESS);
        assertThat(audit.getCompletedAt()).isNull();
    }
}
