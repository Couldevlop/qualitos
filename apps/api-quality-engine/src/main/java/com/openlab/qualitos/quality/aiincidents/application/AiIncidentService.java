package com.openlab.qualitos.quality.aiincidents.application;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentNotFoundException;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentRepository;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStateException;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — signalements d'incidents IA (AI Act Art. 73).
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class AiIncidentService {

    private final AiIncidentRepository repo;
    private final TenantProvider tenantProvider;
    private final AiIncidentEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AiIncidentService(AiIncidentRepository repo,
                             TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new AiIncidentEventPublisher.NoOp(), clock);
    }

    public AiIncidentService(AiIncidentRepository repo,
                             TenantProvider tenantProvider,
                             AiIncidentEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public AiIncidentDto.View detect(AiIncidentDto.DetectRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new AiIncidentStateException("createdByUserId required");
        }
        if (req.aiSystemId() == null) {
            throw new AiIncidentStateException("aiSystemId required");
        }
        if (req.severity() == null) {
            throw new AiIncidentStateException("severity required");
        }
        if (req.occurredAt() == null || req.detectedAt() == null) {
            throw new AiIncidentStateException("occurredAt and detectedAt required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new AiIncidentStateException("Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        AiIncident i = AiIncident.detect(tenantId, req.reference(), req.aiSystemId(),
                req.severity(), req.description(),
                req.affectedPersonsDescription(), req.immediateActionsTaken(),
                req.occurredAt(), req.detectedAt(), req.createdByUserId(), now);
        AiIncident saved = repo.save(i);
        events.publish(saved, AiIncidentEventPublisher.Action.DETECTED);
        return AiIncidentDto.View.of(saved);
    }

    public AiIncidentDto.View edit(UUID id, AiIncidentDto.EditRequest req) {
        AiIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.editDetected(req.description(),
                req.affectedPersonsDescription(), req.immediateActionsTaken(), now);
        AiIncident saved = repo.save(i);
        events.publish(saved, AiIncidentEventPublisher.Action.EDITED);
        return AiIncidentDto.View.of(saved);
    }

    public AiIncidentDto.View startInvestigation(UUID id,
            AiIncidentDto.StartInvestigationRequest req) {
        AiIncident i = loadForTenant(id);
        if (req.investigationLeadUserId() == null) {
            throw new AiIncidentStateException("investigationLeadUserId required");
        }
        Instant now = Instant.now(clock);
        i.startInvestigation(req.investigationLeadUserId(), now);
        AiIncident saved = repo.save(i);
        events.publish(saved, AiIncidentEventPublisher.Action.INVESTIGATION_STARTED);
        return AiIncidentDto.View.of(saved);
    }

    public AiIncidentDto.View notifyRegulator(UUID id,
            AiIncidentDto.NotifyRegulatorRequest req) {
        AiIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.notifyRegulator(req.regulatorReference(), req.rootCauseAnalysis(),
                req.correctiveActions(), now);
        AiIncident saved = repo.save(i);
        events.publish(saved, AiIncidentEventPublisher.Action.NOTIFIED_REGULATOR);
        return AiIncidentDto.View.of(saved);
    }

    public AiIncidentDto.View close(UUID id, AiIncidentDto.CloseRequest req) {
        AiIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.close(req.correctiveActions(), now);
        AiIncident saved = repo.save(i);
        events.publish(saved, AiIncidentEventPublisher.Action.CLOSED);
        return AiIncidentDto.View.of(saved);
    }

    public AiIncidentDto.View dismiss(UUID id, AiIncidentDto.DismissRequest req) {
        AiIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.dismiss(req.reason(), now);
        AiIncident saved = repo.save(i);
        events.publish(saved, AiIncidentEventPublisher.Action.DISMISSED);
        return AiIncidentDto.View.of(saved);
    }

    public void delete(UUID id) {
        AiIncident i = loadForTenant(id);
        if (!i.isDetected()) {
            throw new AiIncidentStateException(
                    "Only DETECTED incidents can be deleted (others preserved for audit)");
        }
        repo.delete(id);
        events.publish(i, AiIncidentEventPublisher.Action.DELETED);
    }

    public AiIncidentDto.View get(UUID id) {
        return AiIncidentDto.View.of(loadForTenant(id));
    }

    public List<AiIncidentDto.View> list(AiIncidentStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<AiIncident> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(AiIncidentDto.View::of).toList();
    }

    public List<AiIncidentDto.View> listByAiSystem(UUID aiSystemId) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (aiSystemId == null) throw new AiIncidentStateException("aiSystemId required");
        return repo.findByTenantAndAiSystemId(tenantId, aiSystemId).stream()
                .map(AiIncidentDto.View::of).toList();
    }

    public List<AiIncidentDto.View> listBySeverity(AiIncidentSeverity severity) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (severity == null) throw new AiIncidentStateException("severity required");
        return repo.findByTenantAndSeverity(tenantId, severity).stream()
                .map(AiIncidentDto.View::of).toList();
    }

    public List<AiIncidentDto.View> listOverdueForRegulator(int limit) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (limit < 1 || limit > 1000) {
            throw new AiIncidentStateException("limit must be in [1, 1000]");
        }
        return repo.findOverdueForRegulatorNotification(tenantId, Instant.now(clock), limit)
                .stream().map(AiIncidentDto.View::of).toList();
    }

    public AiIncidentDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new AiIncidentStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(AiIncidentDto.View::of)
                .orElseThrow(() -> new AiIncidentNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private AiIncident loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        AiIncident i = repo.findById(id)
                .orElseThrow(() -> new AiIncidentNotFoundException(id));
        if (!i.getTenantId().equals(tenantId)) {
            throw new AiIncidentNotFoundException(id);
        }
        return i;
    }
}
