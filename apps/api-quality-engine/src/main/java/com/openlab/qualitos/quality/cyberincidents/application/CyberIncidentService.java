package com.openlab.qualitos.quality.cyberincidents.application;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentNotFoundException;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentRepository;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStateException;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — incidents cyber NIS2 (Art. 23).
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class CyberIncidentService {

    private final CyberIncidentRepository repo;
    private final TenantProvider tenantProvider;
    private final CyberIncidentEventPublisher events;
    private final Clock clock;

    public CyberIncidentService(CyberIncidentRepository repo,
                                TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new CyberIncidentEventPublisher.NoOp(), clock);
    }

    public CyberIncidentService(CyberIncidentRepository repo,
                                TenantProvider tenantProvider,
                                CyberIncidentEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public CyberIncidentDto.View detect(CyberIncidentDto.DetectRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.detectedAt() == null) throw new CyberIncidentStateException("detectedAt required");
        if (req.severity() == null)   throw new CyberIncidentStateException("severity required");
        if (req.incidentType() == null) throw new CyberIncidentStateException("incidentType required");
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new CyberIncidentStateException("Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        CyberIncident i = CyberIncident.detect(tenantId, req.reference(),
                req.title(), req.description(),
                req.detectedAt(), req.occurredAt(),
                req.incidentType(), req.severity(),
                req.estimatedAffectedUsers(),
                req.affectedAssets(), req.affectedServices(),
                req.linkedBreachId(), req.reportedByUserId());
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.DETECTED);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View startAssessment(UUID id, CyberIncidentDto.StartAssessmentRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.startAssessment(req.handledByUserId(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.ASSESSING);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View mitigate(UUID id, CyberIncidentDto.MitigateRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.mitigate(req.containmentMeasures(), req.impactDescription(), req.handledByUserId(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.MITIGATED);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View recordEarlyWarning(UUID id, CyberIncidentDto.NotificationRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.recordEarlyWarning(req.sentAt(), req.reference(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.EARLY_WARNING_SENT);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View recordInitialAssessment(UUID id, CyberIncidentDto.NotificationRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.recordInitialAssessment(req.sentAt(), req.reference(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.INITIAL_ASSESSMENT_SENT);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View recordFinalReport(UUID id, CyberIncidentDto.NotificationRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.recordFinalReport(req.sentAt(), req.reference(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.FINAL_REPORT_SENT);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View close(UUID id, CyberIncidentDto.CloseRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.close(req.closureNotes(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.CLOSED);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View reject(UUID id, CyberIncidentDto.RejectRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.reject(req.reason(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.REJECTED);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View updateSeverity(UUID id, CyberIncidentDto.UpdateSeverityRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.updateSeverity(req.severity(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.SEVERITY_UPDATED);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View linkBreach(UUID id, CyberIncidentDto.LinkBreachRequest req) {
        CyberIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.linkBreach(req.breachId(), now);
        CyberIncident saved = repo.save(i);
        events.publish(saved, CyberIncidentEventPublisher.Action.BREACH_LINKED);
        return CyberIncidentDto.View.of(saved, now);
    }

    public CyberIncidentDto.View get(UUID id) {
        Instant now = Instant.now(clock);
        return CyberIncidentDto.View.of(loadForTenant(id), now);
    }

    public List<CyberIncidentDto.View> list(CyberIncidentStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        Instant now = Instant.now(clock);
        List<CyberIncident> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(i -> CyberIncidentDto.View.of(i, now)).toList();
    }

    public CyberIncidentDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new CyberIncidentStateException("reference required");
        }
        Instant now = Instant.now(clock);
        return repo.findByTenantAndReference(tenantId, reference)
                .map(i -> CyberIncidentDto.View.of(i, now))
                .orElseThrow(() -> new CyberIncidentNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    public List<CyberIncidentDto.View> earlyWarningOverdue(int limit) {
        Instant now = Instant.now(clock);
        int capped = Math.max(1, Math.min(limit, 500));
        return repo.findEarlyWarningOverdue(now, capped).stream()
                .map(i -> CyberIncidentDto.View.of(i, now)).toList();
    }

    public List<CyberIncidentDto.View> initialAssessmentOverdue(int limit) {
        Instant now = Instant.now(clock);
        int capped = Math.max(1, Math.min(limit, 500));
        return repo.findInitialAssessmentOverdue(now, capped).stream()
                .map(i -> CyberIncidentDto.View.of(i, now)).toList();
    }

    public List<CyberIncidentDto.View> finalReportOverdue(int limit) {
        Instant now = Instant.now(clock);
        int capped = Math.max(1, Math.min(limit, 500));
        return repo.findFinalReportOverdue(now, capped).stream()
                .map(i -> CyberIncidentDto.View.of(i, now)).toList();
    }

    private CyberIncident loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        CyberIncident i = repo.findById(id)
                .orElseThrow(() -> new CyberIncidentNotFoundException(id));
        if (!i.getTenantId().equals(tenantId)) {
            throw new CyberIncidentNotFoundException(id);
        }
        return i;
    }
}
