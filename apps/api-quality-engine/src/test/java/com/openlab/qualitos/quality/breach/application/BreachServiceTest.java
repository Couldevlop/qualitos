package com.openlab.qualitos.quality.breach.application;

import com.openlab.qualitos.quality.breach.domain.BreachIncident;
import com.openlab.qualitos.quality.breach.domain.BreachNotFoundException;
import com.openlab.qualitos.quality.breach.domain.BreachRepository;
import com.openlab.qualitos.quality.breach.domain.BreachSeverity;
import com.openlab.qualitos.quality.breach.domain.BreachStateException;
import com.openlab.qualitos.quality.breach.domain.BreachStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Duration;
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
class BreachServiceTest {

    @Mock BreachRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock BreachEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID HANDLER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    BreachService service;

    @BeforeEach
    void setup() {
        service = new BreachService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.save(any())).thenAnswer(inv -> {
            BreachIncident i = inv.getArgument(0);
            if (i.getId() == null) i.assignId(ID);
            return i;
        });
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
    }

    @Test
    void detect_persists_andPublishes() {
        BreachDto.View v = service.detect(req("BREACH-2026-001", BreachSeverity.MEDIUM));
        verify(events).publish(any(), eq(BreachEventPublisher.Action.DETECTED));
        assertThat(v.status()).isEqualTo(BreachStatus.DETECTED);
        assertThat(v.dpaDeadlineAt()).isEqualTo(NOW.plus(Duration.ofHours(72)));
    }

    @Test
    void detect_duplicateReference_throws() {
        when(repo.existsByTenantAndReference(TENANT, "BREACH-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.detect(req("BREACH-DUP", BreachSeverity.LOW)))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void detect_missingDetectedAt_throws() {
        assertThatThrownBy(() -> service.detect(new BreachDto.DetectRequest(
                "BREACH-1", "t", null, null, null, BreachSeverity.LOW,
                0, Set.of(), null, USER)))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void detect_nullSeverity_throws() {
        assertThatThrownBy(() -> service.detect(new BreachDto.DetectRequest(
                "BREACH-1", "t", null, NOW, null, null,
                0, Set.of(), null, USER)))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void startAssessment_moves_andPublishes() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored(BreachSeverity.LOW)));
        BreachDto.View v = service.startAssessment(ID,
                new BreachDto.StartAssessmentRequest(HANDLER));
        assertThat(v.status()).isEqualTo(BreachStatus.ASSESSING);
        verify(events).publish(any(), eq(BreachEventPublisher.Action.ASSESSING));
    }

    @Test
    void contain_moves_andPublishes() {
        BreachIncident i = stored(BreachSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        BreachDto.View v = service.contain(ID,
                new BreachDto.ContainRequest("rotate keys, revoke sessions", HANDLER));
        assertThat(v.status()).isEqualTo(BreachStatus.CONTAINED);
        verify(events).publish(any(), eq(BreachEventPublisher.Action.CONTAINED));
    }

    @Test
    void notifyDpa_recordsTimestamp() {
        BreachIncident i = stored(BreachSeverity.MEDIUM);
        i.startAssessment(HANDLER, NOW);
        i.contain("x", HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        BreachDto.View v = service.notifyDpa(ID,
                new BreachDto.DpaNotificationRequest(NOW.plusSeconds(60), "CNIL-2026-0001"));
        assertThat(v.dpaNotifiedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(v.dpaReference()).isEqualTo("CNIL-2026-0001");
        verify(events).publish(any(), eq(BreachEventPublisher.Action.DPA_NOTIFIED));
    }

    @Test
    void notifySubjects_recordsTimestamp() {
        BreachIncident i = stored(BreachSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.contain("x", HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        BreachDto.View v = service.notifySubjects(ID,
                new BreachDto.SubjectsNotificationRequest(NOW.plusSeconds(60), "email"));
        assertThat(v.subjectsNotifiedAt()).isEqualTo(NOW.plusSeconds(60));
        verify(events).publish(any(), eq(BreachEventPublisher.Action.SUBJECTS_NOTIFIED));
    }

    @Test
    void close_severityHigh_withoutNotif_andWithoutNotes_throws() {
        BreachIncident i = stored(BreachSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.contain("x", HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.close(ID, new BreachDto.CloseRequest(null)))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void close_severityHigh_withNotes_passes() {
        BreachIncident i = stored(BreachSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.contain("x", HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        BreachDto.View v = service.close(ID,
                new BreachDto.CloseRequest("Art. 34§3 — strong encryption applied"));
        assertThat(v.status()).isEqualTo(BreachStatus.CLOSED);
        verify(events).publish(any(), eq(BreachEventPublisher.Action.CLOSED));
    }

    @Test
    void reject_movesToRejected_andPublishes() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored(BreachSeverity.LOW)));
        BreachDto.View v = service.reject(ID,
                new BreachDto.RejectRequest("false positive"));
        assertThat(v.status()).isEqualTo(BreachStatus.REJECTED);
        verify(events).publish(any(), eq(BreachEventPublisher.Action.REJECTED));
    }

    @Test
    void updateSeverity_updates() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored(BreachSeverity.LOW)));
        BreachDto.View v = service.updateSeverity(ID,
                new BreachDto.UpdateSeverityRequest(BreachSeverity.HIGH));
        assertThat(v.severity()).isEqualTo(BreachSeverity.HIGH);
        verify(events).publish(any(), eq(BreachEventPublisher.Action.SEVERITY_UPDATED));
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(BreachNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        BreachIncident other = BreachIncident.detect(UUID.randomUUID(), "BREACH-X",
                "t", null, NOW, null, BreachSeverity.LOW, 0, Set.of(), null, USER);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(BreachNotFoundException.class);
    }

    @Test
    void list_withStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, BreachStatus.DETECTED))
                .thenReturn(List.of(stored(BreachSeverity.LOW)));
        assertThat(service.list(BreachStatus.DETECTED)).hasSize(1);
    }

    @Test
    void list_nullStatus_returnsAll() {
        when(repo.findByTenant(TENANT))
                .thenReturn(List.of(stored(BreachSeverity.LOW), stored(BreachSeverity.MEDIUM)));
        assertThat(service.list(null)).hasSize(2);
    }

    @Test
    void dpaOverdue_capsLimit() {
        when(repo.findDpaOverdue(eq(NOW), eq(500))).thenReturn(List.of());
        service.dpaOverdue(10_000);
        verify(repo).findDpaOverdue(NOW, 500);
    }

    @Test
    void dpaOverdue_floorsLimit() {
        when(repo.findDpaOverdue(eq(NOW), eq(1))).thenReturn(List.of());
        service.dpaOverdue(0);
        verify(repo).findDpaOverdue(NOW, 1);
    }

    private BreachDto.DetectRequest req(String ref, BreachSeverity sev) {
        return new BreachDto.DetectRequest(ref, "Lost laptop", null,
                NOW, NOW.minusSeconds(3600), sev,
                1500L, Set.of("customer-pii"), "risk", USER);
    }

    private BreachIncident stored(BreachSeverity sev) {
        BreachIncident i = BreachIncident.detect(TENANT, "BREACH-2026-001", "Lost laptop",
                null, NOW, null, sev, 1500L, Set.of("customer-pii"), "risk", USER);
        i.assignId(ID);
        return i;
    }
}
