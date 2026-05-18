package com.openlab.qualitos.quality.breach.application;

import com.openlab.qualitos.quality.breach.domain.BreachIncident;
import com.openlab.qualitos.quality.breach.domain.BreachNotFoundException;
import com.openlab.qualitos.quality.breach.domain.BreachRepository;
import com.openlab.qualitos.quality.breach.domain.BreachStateException;
import com.openlab.qualitos.quality.breach.domain.BreachStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — incidents de violation RGPD (Art. 33/34).
 *
 * Cross-tenant lookups yieldent 404 (OWASP A01 — no info leak).
 * Audit trail systématique via {@link BreachEventPublisher} (OWASP A09).
 */
public class BreachService {

    private final BreachRepository repo;
    private final TenantProvider tenantProvider;
    private final BreachEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public BreachService(BreachRepository repo, TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new BreachEventPublisher.NoOp(), clock);
    }

    public BreachService(BreachRepository repo, TenantProvider tenantProvider,
                         BreachEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public BreachDto.View detect(BreachDto.DetectRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.detectedAt() == null) {
            throw new BreachStateException("detectedAt required");
        }
        if (req.severity() == null) {
            throw new BreachStateException("severity required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.internalReference())) {
            throw new BreachStateException(
                    "Reference already used: " + req.internalReference());
        }
        Instant now = Instant.now(clock);
        BreachIncident i = BreachIncident.detect(tenantId,
                req.internalReference(), req.title(), req.description(),
                req.detectedAt(), req.occurredAt(),
                req.severity(), req.affectedSubjectsCount(),
                req.affectedDataCategories(), req.riskOfHarmDescription(),
                req.reportedByUserId());
        BreachIncident saved = repo.save(i);
        events.publish(saved, BreachEventPublisher.Action.DETECTED);
        return BreachDto.View.of(saved, now);
    }

    public BreachDto.View startAssessment(UUID id, BreachDto.StartAssessmentRequest req) {
        BreachIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.startAssessment(req.handledByUserId(), now);
        BreachIncident saved = repo.save(i);
        events.publish(saved, BreachEventPublisher.Action.ASSESSING);
        return BreachDto.View.of(saved, now);
    }

    public BreachDto.View contain(UUID id, BreachDto.ContainRequest req) {
        BreachIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.contain(req.containmentMeasures(), req.handledByUserId(), now);
        BreachIncident saved = repo.save(i);
        events.publish(saved, BreachEventPublisher.Action.CONTAINED);
        return BreachDto.View.of(saved, now);
    }

    public BreachDto.View notifyDpa(UUID id, BreachDto.DpaNotificationRequest req) {
        BreachIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.notifyDpa(req.notifiedAt(), req.reference(), now);
        BreachIncident saved = repo.save(i);
        events.publish(saved, BreachEventPublisher.Action.DPA_NOTIFIED);
        return BreachDto.View.of(saved, now);
    }

    public BreachDto.View notifySubjects(UUID id, BreachDto.SubjectsNotificationRequest req) {
        BreachIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.notifySubjects(req.notifiedAt(), req.channel(), now);
        BreachIncident saved = repo.save(i);
        events.publish(saved, BreachEventPublisher.Action.SUBJECTS_NOTIFIED);
        return BreachDto.View.of(saved, now);
    }

    public BreachDto.View close(UUID id, BreachDto.CloseRequest req) {
        BreachIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.close(req.closureNotes(), now);
        BreachIncident saved = repo.save(i);
        events.publish(saved, BreachEventPublisher.Action.CLOSED);
        return BreachDto.View.of(saved, now);
    }

    public BreachDto.View reject(UUID id, BreachDto.RejectRequest req) {
        BreachIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.reject(req.reason(), now);
        BreachIncident saved = repo.save(i);
        events.publish(saved, BreachEventPublisher.Action.REJECTED);
        return BreachDto.View.of(saved, now);
    }

    public BreachDto.View updateSeverity(UUID id, BreachDto.UpdateSeverityRequest req) {
        BreachIncident i = loadForTenant(id);
        Instant now = Instant.now(clock);
        i.updateSeverity(req.severity(), now);
        BreachIncident saved = repo.save(i);
        events.publish(saved, BreachEventPublisher.Action.SEVERITY_UPDATED);
        return BreachDto.View.of(saved, now);
    }

    public BreachDto.View get(UUID id) {
        Instant now = Instant.now(clock);
        return BreachDto.View.of(loadForTenant(id), now);
    }

    public List<BreachDto.View> list(BreachStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        Instant now = Instant.now(clock);
        List<BreachIncident> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(i -> BreachDto.View.of(i, now)).toList();
    }

    /** Liste les incidents non terminés dont la deadline DPA (72h) est dépassée. */
    public List<BreachDto.View> dpaOverdue(int limit) {
        Instant now = Instant.now(clock);
        int capped = Math.max(1, Math.min(limit, 500));
        return repo.findDpaOverdue(now, capped).stream()
                .map(i -> BreachDto.View.of(i, now)).toList();
    }

    private BreachIncident loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        BreachIncident i = repo.findById(id)
                .orElseThrow(() -> new BreachNotFoundException(id));
        if (!i.getTenantId().equals(tenantId)) throw new BreachNotFoundException(id);
        return i;
    }
}
