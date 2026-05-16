package com.openlab.qualitos.quality.cyberincidents.application;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentNotFoundException;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentRepository;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentSeverity;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStateException;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStatus;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentType;
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
class CyberIncidentServiceTest {

    @Mock CyberIncidentRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock CyberIncidentEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID HANDLER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();

    CyberIncidentService service;

    @BeforeEach
    void setup() {
        service = new CyberIncidentService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            CyberIncident i = inv.getArgument(0);
            if (i.getId() == null) i.assignId(ID);
            return i;
        });
    }

    @Test
    void detect_publishes() {
        CyberIncidentDto.View v = service.detect(req("CYB-2026-001", CyberIncidentSeverity.MEDIUM));
        verify(events).publish(any(), eq(CyberIncidentEventPublisher.Action.DETECTED));
        assertThat(v.status()).isEqualTo(CyberIncidentStatus.DETECTED);
    }

    @Test
    void detect_duplicateRef_throws() {
        when(repo.existsByTenantAndReference(TENANT, "CYB-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.detect(req("CYB-DUP", CyberIncidentSeverity.LOW)))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void detect_missingDetectedAt_throws() {
        CyberIncidentDto.DetectRequest r = new CyberIncidentDto.DetectRequest(
                "CYB-1", "t", null, null, null, CyberIncidentType.MALWARE,
                CyberIncidentSeverity.LOW, 0, Set.of(), Set.of(), null, USER);
        assertThatThrownBy(() -> service.detect(r))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void detect_missingType_throws() {
        CyberIncidentDto.DetectRequest r = new CyberIncidentDto.DetectRequest(
                "CYB-1", "t", null, NOW, null, null,
                CyberIncidentSeverity.LOW, 0, Set.of(), Set.of(), null, USER);
        assertThatThrownBy(() -> service.detect(r))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void detect_missingSeverity_throws() {
        CyberIncidentDto.DetectRequest r = new CyberIncidentDto.DetectRequest(
                "CYB-1", "t", null, NOW, null, CyberIncidentType.MALWARE,
                null, 0, Set.of(), Set.of(), null, USER);
        assertThatThrownBy(() -> service.detect(r))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void startAssessment_succeeds() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored(CyberIncidentSeverity.LOW)));
        CyberIncidentDto.View v = service.startAssessment(ID,
                new CyberIncidentDto.StartAssessmentRequest(HANDLER));
        assertThat(v.status()).isEqualTo(CyberIncidentStatus.ASSESSING);
    }

    @Test
    void mitigate_succeeds() {
        CyberIncident i = stored(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        CyberIncidentDto.View v = service.mitigate(ID,
                new CyberIncidentDto.MitigateRequest("Patched", "Outage 12h", HANDLER));
        assertThat(v.status()).isEqualTo(CyberIncidentStatus.MITIGATED);
    }

    @Test
    void recordEarlyWarning_publishes() {
        CyberIncident i = stored(CyberIncidentSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        CyberIncidentDto.View v = service.recordEarlyWarning(ID,
                new CyberIncidentDto.NotificationRequest(NOW.plusSeconds(3600), "CSIRT-2026-001"));
        assertThat(v.earlyWarningSentAt()).isEqualTo(NOW.plusSeconds(3600));
        verify(events).publish(any(), eq(CyberIncidentEventPublisher.Action.EARLY_WARNING_SENT));
    }

    @Test
    void recordInitialAssessment_publishes() {
        CyberIncident i = stored(CyberIncidentSeverity.MEDIUM);
        i.startAssessment(HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        service.recordInitialAssessment(ID,
                new CyberIncidentDto.NotificationRequest(NOW.plusSeconds(3600), "INIT-1"));
        verify(events).publish(any(), eq(CyberIncidentEventPublisher.Action.INITIAL_ASSESSMENT_SENT));
    }

    @Test
    void recordFinalReport_publishes() {
        CyberIncident i = stored(CyberIncidentSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        service.recordFinalReport(ID,
                new CyberIncidentDto.NotificationRequest(NOW.plusSeconds(3600), "FINAL-1"));
        verify(events).publish(any(), eq(CyberIncidentEventPublisher.Action.FINAL_REPORT_SENT));
    }

    @Test
    void close_publishes() {
        CyberIncident i = stored(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        CyberIncidentDto.View v = service.close(ID, new CyberIncidentDto.CloseRequest(null));
        assertThat(v.status()).isEqualTo(CyberIncidentStatus.CLOSED);
    }

    @Test
    void close_highSeverity_withoutFinalReport_throws() {
        CyberIncident i = stored(CyberIncidentSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.close(ID, new CyberIncidentDto.CloseRequest(null)))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void reject_publishes() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored(CyberIncidentSeverity.LOW)));
        CyberIncidentDto.View v = service.reject(ID, new CyberIncidentDto.RejectRequest("FP"));
        assertThat(v.status()).isEqualTo(CyberIncidentStatus.REJECTED);
    }

    @Test
    void updateSeverity_publishes() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored(CyberIncidentSeverity.LOW)));
        CyberIncidentDto.View v = service.updateSeverity(ID,
                new CyberIncidentDto.UpdateSeverityRequest(CyberIncidentSeverity.CRITICAL));
        assertThat(v.severity()).isEqualTo(CyberIncidentSeverity.CRITICAL);
    }

    @Test
    void linkBreach_publishes() {
        when(repo.findById(ID)).thenReturn(Optional.of(stored(CyberIncidentSeverity.MEDIUM)));
        UUID breach = UUID.randomUUID();
        CyberIncidentDto.View v = service.linkBreach(ID,
                new CyberIncidentDto.LinkBreachRequest(breach));
        assertThat(v.linkedBreachId()).isEqualTo(breach);
    }

    @Test
    void get_missing_404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(CyberIncidentNotFoundException.class);
    }

    @Test
    void get_crossTenant_404_noLeak() {
        CyberIncident other = CyberIncident.detect(UUID.randomUUID(), "CYB-X",
                "t", null, NOW, null, CyberIncidentType.MALWARE,
                CyberIncidentSeverity.LOW, 0, Set.of(), Set.of(), null, USER);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(CyberIncidentNotFoundException.class);
    }

    @Test
    void list_byStatus_filters() {
        when(repo.findByTenantAndStatus(TENANT, CyberIncidentStatus.DETECTED))
                .thenReturn(List.of(stored(CyberIncidentSeverity.LOW)));
        assertThat(service.list(CyberIncidentStatus.DETECTED)).hasSize(1);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void getByReference_missing_404() {
        when(repo.findByTenantAndReference(TENANT, "CYB-X"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("CYB-X"))
                .isInstanceOf(CyberIncidentNotFoundException.class);
    }

    @Test
    void earlyWarningOverdue_capsLimit() {
        when(repo.findEarlyWarningOverdue(eq(NOW), eq(500))).thenReturn(List.of());
        service.earlyWarningOverdue(10_000);
        verify(repo).findEarlyWarningOverdue(NOW, 500);
    }

    @Test
    void initialAssessmentOverdue_floorsLimit() {
        when(repo.findInitialAssessmentOverdue(eq(NOW), eq(1))).thenReturn(List.of());
        service.initialAssessmentOverdue(0);
        verify(repo).findInitialAssessmentOverdue(NOW, 1);
    }

    @Test
    void finalReportOverdue_delegates() {
        when(repo.findFinalReportOverdue(eq(NOW), eq(100))).thenReturn(List.of());
        service.finalReportOverdue(100);
        verify(repo).findFinalReportOverdue(NOW, 100);
    }

    private CyberIncidentDto.DetectRequest req(String ref, CyberIncidentSeverity sev) {
        return new CyberIncidentDto.DetectRequest(ref, "Ransomware",
                "EDR alert", NOW, null,
                CyberIncidentType.RANSOMWARE, sev,
                500L, Set.of("file-server"), Set.of("storage"),
                null, USER);
    }

    private CyberIncident stored(CyberIncidentSeverity sev) {
        CyberIncident i = CyberIncident.detect(TENANT, "CYB-2026-001",
                "Ransomware on file server", null,
                NOW, null, CyberIncidentType.RANSOMWARE, sev,
                500L, Set.of(), Set.of(), null, USER);
        i.assignId(ID);
        return i;
    }
}
