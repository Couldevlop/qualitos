package com.openlab.qualitos.quality.ehs.application;

import com.openlab.qualitos.quality.ehs.domain.Incident;
import com.openlab.qualitos.quality.ehs.domain.IncidentNotFoundException;
import com.openlab.qualitos.quality.ehs.domain.IncidentRepository;
import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentStateException;
import com.openlab.qualitos.quality.ehs.domain.IncidentStatus;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases EHS. Aucune dépendance JPA/Spring ici — uniquement les ports
 * {@link IncidentRepository} et {@link TenantProvider}, et {@link Clock}.
 * Testable sans contexte Spring.
 */
public class IncidentService {

    private final IncidentRepository repo;
    private final TenantProvider tenantProvider;
    private final IncidentEventPublisher events;
    private final Clock clock;

    /** Constructeur sans audit — utile pour les tests / contextes sans audit log. */
    public IncidentService(IncidentRepository repo, TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new IncidentEventPublisher.NoOp(), clock);
    }

    public IncidentService(IncidentRepository repo, TenantProvider tenantProvider,
                           IncidentEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public IncidentDto.IncidentView report(IncidentDto.ReportRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        repo.findByTenantIdAndCode(tenantId, req.code()).ifPresent(i -> {
            throw new IncidentStateException("Incident code already exists: " + req.code());
        });
        Incident i = Incident.report(
                tenantId, req.code(), req.title(), req.description(),
                req.type(), req.severity(), req.occurredAt(), req.location(),
                req.reportedBy(), Instant.now(clock));
        Incident saved = repo.save(i);
        events.publish(saved, IncidentEventPublisher.Action.REPORTED);
        return IncidentDto.IncidentView.of(saved);
    }

    public IncidentDto.IncidentView get(UUID id) {
        return IncidentDto.IncidentView.of(loadForTenant(id));
    }

    public IncidentDto.IncidentView edit(UUID id, IncidentDto.EditRequest req) {
        Incident i = loadForTenant(id);
        i.editDetails(req.title(), req.description(), req.location(),
                req.personsInvolved(), req.severity(), req.standardsCsv(),
                Instant.now(clock));
        Incident saved = repo.save(i);
        events.publish(saved, IncidentEventPublisher.Action.EDITED);
        return IncidentDto.IncidentView.of(saved);
    }

    public IncidentDto.IncidentView investigate(UUID id, IncidentDto.InvestigateRequest req) {
        Incident i = loadForTenant(id);
        i.investigate(req.ownerUserId(), Instant.now(clock));
        Incident saved = repo.save(i);
        events.publish(saved, IncidentEventPublisher.Action.INVESTIGATING);
        return IncidentDto.IncidentView.of(saved);
    }

    public IncidentDto.IncidentView mitigate(UUID id, IncidentDto.MitigateRequest req) {
        Incident i = loadForTenant(id);
        i.mitigate(req.rootCause(), req.correctiveActions(), Instant.now(clock));
        Incident saved = repo.save(i);
        events.publish(saved, IncidentEventPublisher.Action.MITIGATED);
        return IncidentDto.IncidentView.of(saved);
    }

    public IncidentDto.IncidentView close(UUID id) {
        Incident i = loadForTenant(id);
        i.close(Instant.now(clock));
        Incident saved = repo.save(i);
        events.publish(saved, IncidentEventPublisher.Action.CLOSED);
        return IncidentDto.IncidentView.of(saved);
    }

    public IncidentDto.IncidentView cancel(UUID id) {
        Incident i = loadForTenant(id);
        i.cancel(Instant.now(clock));
        Incident saved = repo.save(i);
        events.publish(saved, IncidentEventPublisher.Action.CANCELLED);
        return IncidentDto.IncidentView.of(saved);
    }

    public IncidentDto.IncidentView linkCapa(UUID id, IncidentDto.LinkCapaRequest req) {
        Incident i = loadForTenant(id);
        i.linkCapa(req.capaCaseId(), Instant.now(clock));
        Incident saved = repo.save(i);
        events.publish(saved, IncidentEventPublisher.Action.CAPA_LINKED);
        return IncidentDto.IncidentView.of(saved);
    }

    public IncidentDto.IncidentView linkNc(UUID id, IncidentDto.LinkNcRequest req) {
        Incident i = loadForTenant(id);
        i.linkNc(req.ncId(), Instant.now(clock));
        Incident saved = repo.save(i);
        events.publish(saved, IncidentEventPublisher.Action.NC_LINKED);
        return IncidentDto.IncidentView.of(saved);
    }

    public void delete(UUID id) {
        Incident i = loadForTenant(id);
        if (i.getStatus() != IncidentStatus.REPORTED && i.getStatus() != IncidentStatus.CANCELLED) {
            throw new IncidentStateException(
                    "Only REPORTED or CANCELLED incidents can be deleted (current: "
                            + i.getStatus() + ")");
        }
        repo.delete(i);
    }

    public IncidentRepository.PagedResult<IncidentDto.IncidentView> list(
            IncidentStatus status, IncidentType type, IncidentSeverity severity,
            int page, int size) {
        UUID tenantId = tenantProvider.requireTenantId();
        IncidentRepository.PagedResult<Incident> r = repo.list(
                tenantId, new IncidentRepository.IncidentFilter(status, type, severity), page, size);
        List<IncidentDto.IncidentView> views = r.content().stream()
                .map(IncidentDto.IncidentView::of).toList();
        return new IncidentRepository.PagedResult<>(views, r.totalElements(), r.page(), r.size());
    }

    public Statistics statistics() {
        UUID tenantId = tenantProvider.requireTenantId();
        return new Statistics(
                tenantId,
                repo.countByTenantIdAndStatus(tenantId, IncidentStatus.REPORTED),
                repo.countByTenantIdAndStatus(tenantId, IncidentStatus.INVESTIGATING),
                repo.countByTenantIdAndStatus(tenantId, IncidentStatus.MITIGATED),
                repo.countByTenantIdAndStatus(tenantId, IncidentStatus.CLOSED),
                repo.countByTenantIdAndStatus(tenantId, IncidentStatus.CANCELLED),
                repo.countByTenantIdAndType(tenantId, IncidentType.INJURY),
                repo.countByTenantIdAndType(tenantId, IncidentType.NEAR_MISS),
                repo.countByTenantIdAndType(tenantId, IncidentType.ENVIRONMENTAL),
                repo.countByTenantIdAndType(tenantId, IncidentType.SECURITY),
                repo.countByTenantIdAndType(tenantId, IncidentType.PROPERTY_DAMAGE),
                repo.countByTenantIdAndType(tenantId, IncidentType.OTHER));
    }

    Incident loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        Incident i = repo.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
        if (!i.getTenantId().equals(tenantId)) throw new IncidentNotFoundException(id);
        return i;
    }

    public record Statistics(
            UUID tenantId,
            long reported, long investigating, long mitigated, long closed, long cancelled,
            long injuries, long nearMisses, long environmental, long security,
            long propertyDamage, long other
    ) {}
}
