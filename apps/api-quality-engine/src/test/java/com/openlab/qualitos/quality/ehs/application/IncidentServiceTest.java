package com.openlab.qualitos.quality.ehs.application;

import com.openlab.qualitos.quality.ehs.domain.Incident;
import com.openlab.qualitos.quality.ehs.domain.IncidentNotFoundException;
import com.openlab.qualitos.quality.ehs.domain.IncidentRepository;
import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentStateException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock IncidentRepository repo;
    @Mock TenantProvider tenantProvider;
    IncidentService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new IncidentService(repo, tenantProvider, CLOCK);
        when(tenantProvider.requireTenantId()).thenReturn(TENANT);
    }

    @Test
    void report_persistsNewIncident() {
        when(repo.findByTenantIdAndCode(TENANT, "EHS-1")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> {
            Incident i = inv.getArgument(0);
            i.assignId(ID);
            return i;
        });
        IncidentDto.IncidentView out = service.report(new IncidentDto.ReportRequest(
                "EHS-1", "Fall", "desc", IncidentType.INJURY,
                IncidentSeverity.HIGH, null, "Loc", USER));
        assertThat(out.id()).isEqualTo(ID);
        assertThat(out.status()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(out.severity()).isEqualTo(IncidentSeverity.HIGH);
    }

    @Test
    void report_duplicateCode_rejected() {
        when(repo.findByTenantIdAndCode(TENANT, "dup")).thenReturn(Optional.of(sample(IncidentStatus.REPORTED)));
        assertThatThrownBy(() -> service.report(new IncidentDto.ReportRequest(
                "dup", "x", null, IncidentType.OTHER, null, null, null, USER)))
                .isInstanceOf(IncidentStateException.class);
    }

    @Test
    void get_crossTenant_appearsNotFound() {
        Incident other = Incident.report(UUID.randomUUID(), "EHS-1", "x", null,
                IncidentType.OTHER, null, NOW, null, USER, NOW);
        other.assignId(ID);
        when(repo.findById(ID)).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.get(ID))
                .isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    void investigate_movesToInvestigating() {
        Incident i = sample(IncidentStatus.REPORTED);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        IncidentDto.IncidentView out = service.investigate(ID, new IncidentDto.InvestigateRequest(USER));
        assertThat(out.status()).isEqualTo(IncidentStatus.INVESTIGATING);
        assertThat(out.ownerUserId()).isEqualTo(USER);
    }

    @Test
    void mitigate_requiresInvestigatingStatus() {
        Incident i = sample(IncidentStatus.REPORTED);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.mitigate(ID,
                new IncidentDto.MitigateRequest("rc", "ca")))
                .isInstanceOf(IncidentStateException.class);
    }

    @Test
    void close_fullLifecycle() {
        Incident i = sample(IncidentStatus.REPORTED);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.investigate(ID, new IncidentDto.InvestigateRequest(USER));
        service.mitigate(ID, new IncidentDto.MitigateRequest("rc", "ca"));
        IncidentDto.IncidentView out = service.close(ID);
        assertThat(out.status()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(out.closedAt()).isEqualTo(NOW);
    }

    @Test
    void cancel_fromReported_ok() {
        Incident i = sample(IncidentStatus.REPORTED);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.cancel(ID).status()).isEqualTo(IncidentStatus.CANCELLED);
    }

    @Test
    void linkCapa_setsCapaCaseId() {
        Incident i = sample(IncidentStatus.INVESTIGATING);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID capa = UUID.randomUUID();
        assertThat(service.linkCapa(ID, new IncidentDto.LinkCapaRequest(capa)).capaCaseId())
                .isEqualTo(capa);
    }

    @Test
    void linkNc_setsNcId() {
        Incident i = sample(IncidentStatus.INVESTIGATING);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID nc = UUID.randomUUID();
        assertThat(service.linkNc(ID, new IncidentDto.LinkNcRequest(nc)).ncId()).isEqualTo(nc);
    }

    @Test
    void edit_appliesPatches() {
        Incident i = sample(IncidentStatus.INVESTIGATING);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        IncidentDto.IncidentView out = service.edit(ID, new IncidentDto.EditRequest(
                "Renamed", null, "Loc B", "PB", IncidentSeverity.CRITICAL, "iso-45001"));
        assertThat(out.title()).isEqualTo("Renamed");
        assertThat(out.severity()).isEqualTo(IncidentSeverity.CRITICAL);
    }

    @Test
    void delete_inProgress_rejected() {
        Incident i = sample(IncidentStatus.INVESTIGATING);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        assertThatThrownBy(() -> service.delete(ID))
                .isInstanceOf(IncidentStateException.class);
    }

    @Test
    void delete_reported_cascadesToRepo() {
        Incident i = sample(IncidentStatus.REPORTED);
        when(repo.findById(ID)).thenReturn(Optional.of(i));
        service.delete(ID);
        verify(repo).delete(i);
    }

    @Test
    void list_delegatesToRepo() {
        when(repo.list(eqUuid(TENANT), any(), eqInt(0), eqInt(10)))
                .thenReturn(new IncidentRepository.PagedResult<>(
                        List.of(sample(IncidentStatus.REPORTED)), 1L, 0, 10));
        IncidentRepository.PagedResult<IncidentDto.IncidentView> r =
                service.list(IncidentStatus.REPORTED, null, null, 0, 10);
        assertThat(r.totalElements()).isOne();
        assertThat(r.content()).hasSize(1);
    }

    @Test
    void statistics_aggregatesAllCounters() {
        when(repo.countByTenantIdAndStatus(eqUuid(TENANT), any())).thenReturn(2L);
        when(repo.countByTenantIdAndType(eqUuid(TENANT), any())).thenReturn(3L);
        IncidentService.Statistics s = service.statistics();
        assertThat(s.reported()).isEqualTo(2L);
        assertThat(s.injuries()).isEqualTo(3L);
    }

    private static UUID eqUuid(UUID v) { return org.mockito.ArgumentMatchers.eq(v); }
    private static int eqInt(int v) { return org.mockito.ArgumentMatchers.eq(v); }

    private Incident sample(IncidentStatus status) {
        Incident i = Incident.report(TENANT, "EHS-1", "Title", "desc",
                IncidentType.INJURY, IncidentSeverity.MEDIUM,
                NOW, "Loc", USER, NOW);
        i.assignId(ID);
        if (status == IncidentStatus.INVESTIGATING) i.investigate(null, NOW);
        else if (status == IncidentStatus.MITIGATED) {
            i.investigate(null, NOW);
            i.mitigate("rc", "ca", NOW);
        } else if (status == IncidentStatus.CLOSED) {
            i.investigate(null, NOW);
            i.mitigate("rc", "ca", NOW);
            i.close(NOW);
        } else if (status == IncidentStatus.CANCELLED) {
            i.cancel(NOW);
        }
        return i;
    }
}
