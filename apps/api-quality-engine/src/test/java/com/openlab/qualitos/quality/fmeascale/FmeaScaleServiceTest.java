package com.openlab.qualitos.quality.fmeascale;

import com.openlab.qualitos.quality.auditlog.ActorType;
import com.openlab.qualitos.quality.auditlog.AuditEventDto;
import com.openlab.qualitos.quality.auditlog.AuditEventService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le référentiel de cotation d'un tenant (§4.5).
 *
 * <p>Deux promesses tiennent ce module. La première : un tenant qui n'a rien
 * redéfini cote sur le barème de référence, et l'écran le DIT — deux RPN issus
 * de barèmes différents ne se comparent pas. La seconde : une échelle va de 1 à
 * 10 sans trou, parce qu'un score sans définition fait coter au jugé exactement
 * là où le barème existe pour l'éviter.
 */
@ExtendWith(MockitoExtension.class)
class FmeaScaleServiceTest {

    @Mock FmeaScaleRowRepository repository;
    @Mock AuditEventService auditEvents;
    @InjectMocks FmeaScaleService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    @BeforeEach
    void ctx() {
        TenantContext.setTenantId(TENANT.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ACTOR.toString(), "n/a", List.of()));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ---------- le barème par défaut ----------

    @Test
    void aTenantThatRedefinedNothingReceivesTheReferenceScale() {
        when(repository.findByTenantIdAndKindOrderByScoreDesc(any(), any()))
                .thenReturn(List.of());

        FmeaScaleDto.ReferenceView view = service.findAll();

        assertThat(view.scales()).hasSize(3);
        for (FmeaScaleDto.ScaleView scale : view.scales()) {
            // `custom = false` n'est pas cosmétique : il dit sur quel barème
            // l'organisation cote.
            assertThat(scale.custom()).isFalse();
            assertThat(scale.rows()).hasSize(10);
            assertThat(scale.rows().stream().map(FmeaScaleDto.RowView::score))
                    .containsExactly(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        }
    }

    @Test
    void theReferenceIsNeverWrittenToTheDatabase() {
        when(repository.findByTenantIdAndKindOrderByScoreDesc(any(), any()))
                .thenReturn(List.of());

        service.findAll();

        // Semer les trente lignes figerait le référentiel au jour de
        // l'inscription, et rendrait « jamais touché » indistinguable de
        // « redéfini à l'identique ».
        verify(repository, never()).save(any());
    }

    @Test
    void aRedefinedScaleIsServedInsteadOfTheReference() {
        when(repository.findByTenantIdAndKindOrderByScoreDesc(TENANT, "SEVERITY"))
                .thenReturn(storedScale("SEVERITY"));
        when(repository.findByTenantIdAndKindOrderByScoreDesc(TENANT, "OCCURRENCE"))
                .thenReturn(List.of());
        when(repository.findByTenantIdAndKindOrderByScoreDesc(TENANT, "DETECTION"))
                .thenReturn(List.of());

        FmeaScaleDto.ReferenceView view = service.findAll();

        FmeaScaleDto.ScaleView severity = view.scales().stream()
                .filter(s -> s.kind() == FmeaScaleKind.SEVERITY).findFirst().orElseThrow();
        assertThat(severity.custom()).isTrue();
        assertThat(severity.rows().get(0).label()).isEqualTo("Arrêt de ligne client");
        assertThat(severity.updatedBy()).isEqualTo(ACTOR);
    }

    // ---------- le remplacement ----------

    @Test
    void aCompleteScaleIsStoredFromTenToOne() {
        when(repository.findByTenantIdAndKindOrderByScoreDesc(TENANT, "SEVERITY"))
                .thenReturn(storedScale("SEVERITY"));

        service.replace(FmeaScaleKind.SEVERITY, request(10));

        ArgumentCaptor<FmeaScaleRowEntity> captor =
                ArgumentCaptor.forClass(FmeaScaleRowEntity.class);
        verify(repository, org.mockito.Mockito.times(10)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(FmeaScaleRowEntity::getScore)
                .containsExactly((short) 10, (short) 9, (short) 8, (short) 7, (short) 6,
                        (short) 5, (short) 4, (short) 3, (short) 2, (short) 1);
        assertThat(captor.getAllValues()).allSatisfy(row -> {
            assertThat(row.getTenantId()).isEqualTo(TENANT);
            // L'acteur vient du jeton : redéfinir un barème doit être attribuable.
            assertThat(row.getUpdatedBy()).isEqualTo(ACTOR);
        });
    }

    @Test
    void theOldScaleIsWipedBeforeTheNewOneIsWritten() {
        when(repository.findByTenantIdAndKindOrderByScoreDesc(TENANT, "SEVERITY"))
                .thenReturn(storedScale("SEVERITY"));

        service.replace(FmeaScaleKind.SEVERITY, request(10));

        // Sans la vidange forcée, la contrainte d'unicité (tenant, kind, score)
        // refuserait les nouvelles lignes dans la même transaction.
        verify(repository).deleteByTenantIdAndKind(TENANT, "SEVERITY");
        verify(repository).flush();
    }

    @Test
    void aScaleWithAHoleIsRefusedAndNothingIsWritten() {
        assertThatThrownBy(() -> service.replace(FmeaScaleKind.SEVERITY, request(9)))
                .isInstanceOf(FmeaScaleValidationException.class)
                .hasMessageContaining("1");

        // Un score sans définition ne se voit pas à l'écran : il se découvre le
        // jour où quelqu'un cote un 7 qui ne veut rien dire.
        verify(repository, never()).save(any());
        verify(repository, never()).deleteByTenantIdAndKind(any(), any());
    }

    @Test
    void theSameScoreTwiceIsRefused() {
        List<FmeaScaleDto.RowRequest> rows = new ArrayList<>(request(10).rows());
        rows.set(3, new FmeaScaleDto.RowRequest(10, "Doublon", null, null, null));

        assertThatThrownBy(() -> service.replace(FmeaScaleKind.SEVERITY,
                new FmeaScaleDto.ScaleRequest(rows)))
                .isInstanceOf(FmeaScaleValidationException.class)
                .hasMessageContaining("10");
    }

    @Test
    void anEmptyLabelIsRefusedBecauseTheScaleWouldBecomeUnreadable() {
        List<FmeaScaleDto.RowRequest> rows = new ArrayList<>(request(10).rows());
        rows.set(0, new FmeaScaleDto.RowRequest(10, "   ", null, null, null));

        assertThatThrownBy(() -> service.replace(FmeaScaleKind.SEVERITY,
                new FmeaScaleDto.ScaleRequest(rows)))
                .isInstanceOf(FmeaScaleValidationException.class);
    }

    @Test
    void aTooLongDescriptionIsRefusedRatherThanTruncated() {
        List<FmeaScaleDto.RowRequest> rows = new ArrayList<>(request(10).rows());
        rows.set(0, new FmeaScaleDto.RowRequest(10, "Danger", "x".repeat(501), null, null));

        // Une définition coupée au milieu se lit comme une règle complète.
        assertThatThrownBy(() -> service.replace(FmeaScaleKind.SEVERITY,
                new FmeaScaleDto.ScaleRequest(rows)))
                .isInstanceOf(FmeaScaleValidationException.class)
                .hasMessageContaining("description");
    }

    @Test
    void noScaleIsWrittenWithoutATenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.replace(FmeaScaleKind.SEVERITY, request(10)))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // ---------- le retour à la référence ----------

    @Test
    void revertingDeletesTheRowsRatherThanCopyingTheReference() {
        when(repository.findByTenantIdAndKindOrderByScoreDesc(TENANT, "DETECTION"))
                .thenReturn(List.of());

        FmeaScaleDto.ScaleView view = service.revertToReference(FmeaScaleKind.DETECTION);

        verify(repository).deleteByTenantIdAndKind(TENANT, "DETECTION");
        // Recopier laisserait croire que l'organisation a délibérément adopté ces
        // dix lignes, alors qu'elle a seulement renoncé à les redéfinir.
        verify(repository, never()).save(any());
        assertThat(view.custom()).isFalse();
        assertThat(view.rows()).hasSize(10);
    }

    // ---------- le journal ----------

    @Test
    void redefiningAScaleIsWrittenToTheTenantJournal() {
        when(repository.findByTenantIdAndKindOrderByScoreDesc(TENANT, "SEVERITY"))
                .thenReturn(List.of());

        service.replace(FmeaScaleKind.SEVERITY, request(10));

        ArgumentCaptor<AuditEventDto.RecordEventRequest> event =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(org.mockito.ArgumentMatchers.eq(TENANT), event.capture());
        assertThat(event.getValue().action()).isEqualTo("fmea.rating_scale.redefined");
        assertThat(event.getValue().resourceType()).isEqualTo("fmea_rating_scale");
        // L'acteur vient du jeton : un barème redéfini sans auteur attribuable
        // ne vaut rien devant un auditeur (OWASP A01).
        assertThat(event.getValue().actorType()).isEqualTo(ActorType.USER);
        assertThat(event.getValue().actorUserId()).isEqualTo(ACTOR);
        assertThat(event.getValue().payloadJson()).contains("\"kind\":\"SEVERITY\"", "\"rows\":10");
    }

    @Test
    void revertingIsAlsoWrittenToTheJournal() {
        when(repository.findByTenantIdAndKindOrderByScoreDesc(TENANT, "DETECTION"))
                .thenReturn(List.of());

        service.revertToReference(FmeaScaleKind.DETECTION);

        ArgumentCaptor<AuditEventDto.RecordEventRequest> event =
                ArgumentCaptor.forClass(AuditEventDto.RecordEventRequest.class);
        verify(auditEvents).recordForTenant(org.mockito.ArgumentMatchers.eq(TENANT), event.capture());
        // Sans cette ligne, la ligne en base ayant été SUPPRIMÉE, plus rien
        // n'attesterait que le tenant a un jour coté sur un autre barème.
        assertThat(event.getValue().action()).isEqualTo("fmea.rating_scale.reverted");
    }

    @Test
    void aRefusedScaleLeavesNoTraceInTheJournal() {
        assertThatThrownBy(() -> service.replace(FmeaScaleKind.SEVERITY, request(9)))
                .isInstanceOf(FmeaScaleValidationException.class);

        verify(auditEvents, never()).recordForTenant(any(), any());
    }

    // ---------- montage ----------

    /** Un barème complet, ou tronqué à {@code count} lignes pour le prendre en défaut. */
    private FmeaScaleDto.ScaleRequest request(int count) {
        List<FmeaScaleDto.RowRequest> rows = new ArrayList<>();
        for (int score = 10; score > 10 - count; score--) {
            rows.add(new FmeaScaleDto.RowRequest(score, "Niveau " + score,
                    "Ce que vaut un " + score, null, null));
        }
        return new FmeaScaleDto.ScaleRequest(rows);
    }

    private List<FmeaScaleRowEntity> storedScale(String kind) {
        List<FmeaScaleRowEntity> rows = new ArrayList<>();
        for (int score = 10; score >= 1; score--) {
            FmeaScaleRowEntity entity = new FmeaScaleRowEntity();
            entity.setId(UUID.randomUUID());
            entity.setTenantId(TENANT);
            entity.setKind(kind);
            entity.setScore((short) score);
            entity.setLabel(score == 10 ? "Arrêt de ligne client" : "Niveau " + score);
            entity.setUpdatedBy(ACTOR);
            entity.setUpdatedAt(Instant.parse("2026-08-30T10:00:00Z"));
            rows.add(entity);
        }
        return rows;
    }
}
