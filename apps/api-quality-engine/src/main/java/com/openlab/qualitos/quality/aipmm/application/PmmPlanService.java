package com.openlab.qualitos.quality.aipmm.application;

import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanNotFoundException;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanRepository;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStateException;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — plans de surveillance post-marché (AI Act Art. 72).
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class PmmPlanService {

    private final PmmPlanRepository repo;
    private final TenantProvider tenantProvider;
    private final PmmPlanEventPublisher events;
    private final Clock clock;

    public PmmPlanService(PmmPlanRepository repo, TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new PmmPlanEventPublisher.NoOp(), clock);
    }

    public PmmPlanService(PmmPlanRepository repo, TenantProvider tenantProvider,
                          PmmPlanEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public PmmPlanDto.View draft(PmmPlanDto.DraftRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new PmmPlanStateException("createdByUserId required");
        }
        if (req.aiSystemId() == null) {
            throw new PmmPlanStateException("aiSystemId required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new PmmPlanStateException("Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        PmmPlan p = PmmPlan.draft(tenantId, req.reference(), req.aiSystemId(),
                req.name(), req.description(),
                req.metricsMonitored(), req.collectionMethod(), req.reviewFrequency(),
                req.responsiblePartyDescription(), req.triggerCriteria(),
                req.qmsLinkReference(), req.createdByUserId(), now);
        PmmPlan saved = repo.save(p);
        events.publish(saved, PmmPlanEventPublisher.Action.DRAFTED);
        return PmmPlanDto.View.of(saved);
    }

    public PmmPlanDto.View edit(UUID id, PmmPlanDto.EditRequest req) {
        PmmPlan p = loadForTenant(id);
        Instant now = Instant.now(clock);
        p.editDraft(req.name(), req.description(),
                req.metricsMonitored(), req.collectionMethod(), req.reviewFrequency(),
                req.responsiblePartyDescription(), req.triggerCriteria(),
                req.qmsLinkReference(), now);
        PmmPlan saved = repo.save(p);
        events.publish(saved, PmmPlanEventPublisher.Action.EDITED);
        return PmmPlanDto.View.of(saved);
    }

    public PmmPlanDto.View activate(UUID id) {
        PmmPlan p = loadForTenant(id);
        Instant now = Instant.now(clock);
        p.activate(now);
        PmmPlan saved = repo.save(p);
        events.publish(saved, PmmPlanEventPublisher.Action.ACTIVATED);
        return PmmPlanDto.View.of(saved);
    }

    public PmmPlanDto.View recordReview(UUID id, PmmPlanDto.ReviewRequest req) {
        PmmPlan p = loadForTenant(id);
        if (req.reviewedByUserId() == null) {
            throw new PmmPlanStateException("reviewedByUserId required");
        }
        Instant now = Instant.now(clock);
        p.recordReview(req.reviewedByUserId(), now);
        PmmPlan saved = repo.save(p);
        events.publish(saved, PmmPlanEventPublisher.Action.REVIEWED);
        return PmmPlanDto.View.of(saved);
    }

    public PmmPlanDto.View suspend(UUID id, PmmPlanDto.SuspendRequest req) {
        PmmPlan p = loadForTenant(id);
        Instant now = Instant.now(clock);
        p.suspend(req.reason(), now);
        PmmPlan saved = repo.save(p);
        events.publish(saved, PmmPlanEventPublisher.Action.SUSPENDED);
        return PmmPlanDto.View.of(saved);
    }

    public PmmPlanDto.View close(UUID id, PmmPlanDto.CloseRequest req) {
        PmmPlan p = loadForTenant(id);
        Instant now = Instant.now(clock);
        p.close(req.reason(), now);
        PmmPlan saved = repo.save(p);
        events.publish(saved, PmmPlanEventPublisher.Action.CLOSED);
        return PmmPlanDto.View.of(saved);
    }

    public void delete(UUID id) {
        PmmPlan p = loadForTenant(id);
        if (!p.isDraft()) {
            throw new PmmPlanStateException(
                    "Only DRAFT plans can be deleted (others preserved for audit)");
        }
        repo.delete(id);
        events.publish(p, PmmPlanEventPublisher.Action.DELETED);
    }

    public PmmPlanDto.View get(UUID id) { return PmmPlanDto.View.of(loadForTenant(id)); }

    public List<PmmPlanDto.View> list(PmmPlanStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<PmmPlan> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(PmmPlanDto.View::of).toList();
    }

    public List<PmmPlanDto.View> listByAiSystem(UUID aiSystemId) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (aiSystemId == null) throw new PmmPlanStateException("aiSystemId required");
        return repo.findByTenantAndAiSystemId(tenantId, aiSystemId).stream()
                .map(PmmPlanDto.View::of).toList();
    }

    public List<PmmPlanDto.View> listOverdueReviews(int limit) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (limit < 1 || limit > 1000) {
            throw new PmmPlanStateException("limit must be in [1, 1000]");
        }
        return repo.findOverdueReviews(tenantId, Instant.now(clock), limit).stream()
                .map(PmmPlanDto.View::of).toList();
    }

    public PmmPlanDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new PmmPlanStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(PmmPlanDto.View::of)
                .orElseThrow(() -> new PmmPlanNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private PmmPlan loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        PmmPlan p = repo.findById(id).orElseThrow(() -> new PmmPlanNotFoundException(id));
        if (!p.getTenantId().equals(tenantId)) {
            throw new PmmPlanNotFoundException(id);
        }
        return p;
    }
}
