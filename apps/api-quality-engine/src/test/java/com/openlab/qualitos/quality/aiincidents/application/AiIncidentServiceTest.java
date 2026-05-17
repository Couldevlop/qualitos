package com.openlab.qualitos.quality.aiincidents.application;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentNotFoundException;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentRepository;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStateException;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStatus;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiIncidentServiceTest {

    @Mock AiIncidentRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock AiIncidentEventPublisher events;

    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();

    AiIncidentService service;

    @BeforeEach
    void setup() {
        service = new AiIncidentService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.existsByTenantAndReference(any(), any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            AiIncident i = inv.getArgument(0);
            if (i.getId() == null) i.assignId(ID);
            return i;
        });
    }

    @Test
    void detect_publishes() {
        AiIncidentDto.View v = service.detect(req("REF-1"));
        verify(events).publish(any(), eq(AiIncidentEventPublisher.Action.DETECTED));
        assertThat(v.status()).isEqualTo(AiIncidentStatus.DETECTED);
    }

    @Test
    void detect_duplicate_throws() {
        when(repo.existsByTenantAndReference(TENANT, "REF-DUP")).thenReturn(true);
        assertThatThrownBy(() -> service.detect(req("REF-DUP")))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void detect_missingActor_throws() {
        AiIncidentDto.DetectRequest r = new AiIncidentDto.DetectRequest("REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "d", null, null, NOW.minusSeconds(3600), NOW, null);
        assertThatThrownBy(() -> service.detect(r))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void detect_missingSystem_throws() {
        AiIncidentDto.DetectRequest r = new AiIncidentDto.DetectRequest("REF-1", null,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "d", null, null, NOW.minusSeconds(3600), NOW, USER);
        assertThatThrownBy(() -> service.detect(r))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void detect_missingSeverity_throws() {
        AiIncidentDto.DetectRequest r = new AiIncidentDto.DetectRequest("REF-1", SYS,
                null, "d", null, null, NOW.minusSeconds(3600), NOW, USER);
        assertThatThrownBy(() -> service.detect(r))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void detect_missingTimestamps_throws() {
        AiIncidentDto.DetectRequest r = new AiIncidentDto.DetectRequest("REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "d", null, null, null, NOW, USER);
        assertThatThrownBy(() -> service.detect(r))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void edit_ok() {
        AiIncident i = sample();
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        AiIncidentDto.View v = service.edit(ID, new AiIncidentDto.EditRequest(
                "new desc", null, null));
        assertThat(v.description()).isEqualTo("new desc");
        verify(events).publish(any(), eq(AiIncidentEventPublisher.Action.EDITED));
    }

    @Test
    void edit_crossTenant_404() {
        AiIncident foreign = AiIncident.detect(OTHER, "REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "d", null, null, NOW.minusSeconds(3600), NOW, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.edit(ID, new AiIncidentDto.EditRequest(
                "x", null, null)))
                .isInstanceOf(AiIncidentNotFoundException.class);
    }

    @Test
    void startInvestigation_ok() {
        AiIncident i = sample();
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        AiIncidentDto.View v = service.startInvestigation(ID,
                new AiIncidentDto.StartInvestigationRequest(LEAD));
        assertThat(v.status()).isEqualTo(AiIncidentStatus.INVESTIGATING);
        verify(events).publish(any(), eq(AiIncidentEventPublisher.Action.INVESTIGATION_STARTED));
    }

    @Test
    void startInvestigation_nullLead_throws() {
        AiIncident i = sample();
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.startInvestigation(ID,
                new AiIncidentDto.StartInvestigationRequest(null)))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void notifyRegulator_ok() {
        AiIncident i = sample();
        i.startInvestigation(LEAD, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        AiIncidentDto.View v = service.notifyRegulator(ID,
                new AiIncidentDto.NotifyRegulatorRequest("REG-1", "rca", "actions"));
        assertThat(v.status()).isEqualTo(AiIncidentStatus.NOTIFIED_REGULATOR);
        verify(events).publish(any(), eq(AiIncidentEventPublisher.Action.NOTIFIED_REGULATOR));
    }

    @Test
    void close_ok() {
        AiIncident i = sample();
        i.startInvestigation(LEAD, NOW);
        i.notifyRegulator("REG-1", "rca", null, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        AiIncidentDto.View v = service.close(ID, new AiIncidentDto.CloseRequest("done"));
        assertThat(v.status()).isEqualTo(AiIncidentStatus.CLOSED);
        verify(events).publish(any(), eq(AiIncidentEventPublisher.Action.CLOSED));
    }

    @Test
    void dismiss_ok() {
        AiIncident i = sample();
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        AiIncidentDto.View v = service.dismiss(ID, new AiIncidentDto.DismissRequest("false alarm"));
        assertThat(v.status()).isEqualTo(AiIncidentStatus.DISMISSED);
        verify(events).publish(any(), eq(AiIncidentEventPublisher.Action.DISMISSED));
    }

    @Test
    void delete_detectedOnly() {
        AiIncident i = sample();
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        service.delete(ID);
        verify(repo).delete(ID);
        verify(events).publish(any(), eq(AiIncidentEventPublisher.Action.DELETED));
    }

    @Test
    void delete_nonDetected_throws() {
        AiIncident i = sample();
        i.startInvestigation(LEAD, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(AiIncidentStateException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void get_notFound_throws() {
        when(repo.findById(ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(AiIncidentNotFoundException.class);
    }

    @Test
    void get_crossTenant_404() {
        AiIncident foreign = AiIncident.detect(OTHER, "REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "d", null, null, NOW.minusSeconds(3600), NOW, USER, NOW);
        when(repo.findById(ID)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(AiIncidentNotFoundException.class);
    }

    @Test
    void list_all() {
        when(repo.findByTenant(TENANT)).thenReturn(List.of(sample()));
        assertThat(service.list(null)).hasSize(1);
    }

    @Test
    void list_byStatus() {
        when(repo.findByTenantAndStatus(TENANT, AiIncidentStatus.DETECTED))
                .thenReturn(List.of(sample()));
        assertThat(service.list(AiIncidentStatus.DETECTED)).hasSize(1);
    }

    @Test
    void listByAiSystem_ok() {
        when(repo.findByTenantAndAiSystemId(TENANT, SYS))
                .thenReturn(List.of(sample()));
        assertThat(service.listByAiSystem(SYS)).hasSize(1);
    }

    @Test
    void listByAiSystem_null_throws() {
        assertThatThrownBy(() -> service.listByAiSystem(null))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void listBySeverity_ok() {
        when(repo.findByTenantAndSeverity(TENANT,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION))
                .thenReturn(List.of(sample()));
        assertThat(service.listBySeverity(
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION)).hasSize(1);
    }

    @Test
    void listBySeverity_null_throws() {
        assertThatThrownBy(() -> service.listBySeverity(null))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void listOverdue_ok() {
        when(repo.findOverdueForRegulatorNotification(eq(TENANT), any(), anyInt()))
                .thenReturn(List.of(sample()));
        assertThat(service.listOverdueForRegulator(100)).hasSize(1);
    }

    @Test
    void listOverdue_outOfRange_throws() {
        assertThatThrownBy(() -> service.listOverdueForRegulator(0))
                .isInstanceOf(AiIncidentStateException.class);
        assertThatThrownBy(() -> service.listOverdueForRegulator(1001))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void getByReference_blank_throws() {
        assertThatThrownBy(() -> service.getByReference(" "))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void getByReference_notFound_throws() {
        when(repo.findByTenantAndReference(TENANT, "REF-X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByReference("REF-X"))
                .isInstanceOf(AiIncidentNotFoundException.class);
    }

    @Test
    void getByReference_found() {
        when(repo.findByTenantAndReference(TENANT, "REF-1"))
                .thenReturn(Optional.of(sample()));
        assertThat(service.getByReference("REF-1").reference()).isEqualTo("REF-1");
    }

    @Test
    void constructor_withoutEvents_usesNoOp() {
        AiIncidentService s2 = new AiIncidentService(repo, tenantProvider, CLOCK);
        s2.detect(req("REF-N"));
    }

    private AiIncidentDto.DetectRequest req(String ref) {
        return new AiIncidentDto.DetectRequest(ref, SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "desc", "persons", "actions",
                NOW.minusSeconds(3600), NOW, USER);
    }

    private AiIncident sample() {
        AiIncident i = AiIncident.detect(TENANT, "REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "desc", "persons", "actions",
                NOW.minusSeconds(3600), NOW, USER, NOW);
        i.assignId(ID);
        return i;
    }
}
