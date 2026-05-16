package com.openlab.qualitos.quality.ehs.application;

import com.openlab.qualitos.quality.ehs.domain.Incident;
import com.openlab.qualitos.quality.ehs.domain.IncidentRepository;
import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentStatus;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Vérifie que chaque use case publie l'événement attendu sur le port
 * {@link IncidentEventPublisher}. L'adapter réel vers le journal d'audit
 * est testé en infrastructure ; ici on isole le contrat de l'application.
 */
@ExtendWith(MockitoExtension.class)
class IncidentServiceAuditHooksTest {

    @Mock IncidentRepository repo;
    @Mock TenantProvider tenantProvider;
    @Mock IncidentEventPublisher events;
    IncidentService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new IncidentService(repo, tenantProvider, events, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void report_publishesReportedEvent() {
        when(repo.findByTenantIdAndCode(TENANT, "EHS-1")).thenReturn(Optional.empty());
        service.report(new IncidentDto.ReportRequest(
                "EHS-1", "Fall", null, IncidentType.INJURY,
                IncidentSeverity.HIGH, null, "Loc", USER));
        verify(events).publish(any(Incident.class), eq(IncidentEventPublisher.Action.REPORTED));
    }

    @Test
    void investigate_publishesInvestigatingEvent() {
        when(repo.findById(ID)).thenReturn(Optional.of(sample(IncidentStatus.REPORTED)));
        service.investigate(ID, new IncidentDto.InvestigateRequest(USER));
        verify(events).publish(any(Incident.class), eq(IncidentEventPublisher.Action.INVESTIGATING));
    }

    @Test
    void mitigate_publishesMitigatedEvent() {
        when(repo.findById(ID)).thenReturn(Optional.of(sample(IncidentStatus.INVESTIGATING)));
        service.mitigate(ID, new IncidentDto.MitigateRequest("rc", "ca"));
        verify(events).publish(any(Incident.class), eq(IncidentEventPublisher.Action.MITIGATED));
    }

    @Test
    void close_publishesClosedEvent() {
        when(repo.findById(ID)).thenReturn(Optional.of(sample(IncidentStatus.MITIGATED)));
        service.close(ID);
        verify(events).publish(any(Incident.class), eq(IncidentEventPublisher.Action.CLOSED));
    }

    @Test
    void cancel_publishesCancelledEvent() {
        when(repo.findById(ID)).thenReturn(Optional.of(sample(IncidentStatus.REPORTED)));
        service.cancel(ID);
        verify(events).publish(any(Incident.class), eq(IncidentEventPublisher.Action.CANCELLED));
    }

    @Test
    void linkCapa_publishesCapaLinkedEvent() {
        when(repo.findById(ID)).thenReturn(Optional.of(sample(IncidentStatus.INVESTIGATING)));
        service.linkCapa(ID, new IncidentDto.LinkCapaRequest(UUID.randomUUID()));
        verify(events).publish(any(Incident.class), eq(IncidentEventPublisher.Action.CAPA_LINKED));
    }

    @Test
    void linkNc_publishesNcLinkedEvent() {
        when(repo.findById(ID)).thenReturn(Optional.of(sample(IncidentStatus.INVESTIGATING)));
        service.linkNc(ID, new IncidentDto.LinkNcRequest(UUID.randomUUID()));
        verify(events).publish(any(Incident.class), eq(IncidentEventPublisher.Action.NC_LINKED));
    }

    @Test
    void edit_publishesEditedEvent() {
        when(repo.findById(ID)).thenReturn(Optional.of(sample(IncidentStatus.INVESTIGATING)));
        service.edit(ID, new IncidentDto.EditRequest(
                "renamed", null, null, null, null, null));
        verify(events).publish(any(Incident.class), eq(IncidentEventPublisher.Action.EDITED));
    }

    @Test
    void noOpPublisher_doesNotThrow() {
        IncidentService bare = new IncidentService(repo, tenantProvider, CLOCK);
        when(repo.findByTenantIdAndCode(TENANT, "EHS-2")).thenReturn(Optional.empty());
        bare.report(new IncidentDto.ReportRequest(
                "EHS-2", "x", null, IncidentType.OTHER, null, null, null, USER));
        // Pas d'exception ⇒ le NoOp tient.
    }

    private Incident sample(IncidentStatus status) {
        Incident i = Incident.report(TENANT, "EHS-1", "Title", "desc",
                IncidentType.INJURY, IncidentSeverity.MEDIUM, NOW, "Loc", USER, NOW);
        i.assignId(ID);
        if (status == IncidentStatus.INVESTIGATING) i.investigate(null, NOW);
        else if (status == IncidentStatus.MITIGATED) {
            i.investigate(null, NOW); i.mitigate("rc", "ca", NOW);
        }
        return i;
    }
}
