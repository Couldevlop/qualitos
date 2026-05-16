package com.openlab.qualitos.quality.nis2measures.application;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureNotFoundException;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStateException;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStatus;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasureRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — mesures de gestion des risques NIS2 (Art. 21).
 *
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class Nis2MeasureService {

    private final Nis2RiskMeasureRepository repo;
    private final TenantProvider tenantProvider;
    private final Nis2MeasureEventPublisher events;
    private final Clock clock;

    public Nis2MeasureService(Nis2RiskMeasureRepository repo,
                              TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new Nis2MeasureEventPublisher.NoOp(), clock);
    }

    public Nis2MeasureService(Nis2RiskMeasureRepository repo,
                              TenantProvider tenantProvider,
                              Nis2MeasureEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public Nis2MeasureDto.View plan(Nis2MeasureDto.PlanRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new Nis2MeasureStateException("createdByUserId required");
        }
        if (req.category() == null) {
            throw new Nis2MeasureStateException("category required");
        }
        if (req.residualRiskRating() == null) {
            throw new Nis2MeasureStateException("residualRiskRating required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new Nis2MeasureStateException("Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        Nis2RiskMeasure m = Nis2RiskMeasure.plan(tenantId, req.reference(),
                req.category(), req.title(), req.description(),
                req.ownerUserId(), req.maturityLevel(),
                req.residualRiskRating(), req.criticalRiskJustification(),
                req.reviewIntervalDays(),
                req.evidenceUrls(),
                req.linkedProcessingActivityIds(),
                req.linkedProcessorAgreementIds(),
                req.notes(), req.createdByUserId(), now);
        Nis2RiskMeasure saved = repo.save(m);
        events.publish(saved, Nis2MeasureEventPublisher.Action.PLANNED);
        return Nis2MeasureDto.View.of(saved, now);
    }

    public Nis2MeasureDto.View edit(UUID id, Nis2MeasureDto.EditRequest req) {
        Nis2RiskMeasure m = loadForTenant(id);
        if (req.residualRiskRating() == null) {
            throw new Nis2MeasureStateException("residualRiskRating required");
        }
        Instant now = Instant.now(clock);
        m.edit(req.title(), req.description(),
                req.ownerUserId(), req.maturityLevel(),
                req.residualRiskRating(), req.criticalRiskJustification(),
                req.reviewIntervalDays(),
                req.evidenceUrls(),
                req.linkedProcessingActivityIds(),
                req.linkedProcessorAgreementIds(),
                req.notes(), now);
        Nis2RiskMeasure saved = repo.save(m);
        events.publish(saved, Nis2MeasureEventPublisher.Action.EDITED);
        return Nis2MeasureDto.View.of(saved, now);
    }

    public Nis2MeasureDto.View startImplementation(UUID id) {
        Nis2RiskMeasure m = loadForTenant(id);
        Instant now = Instant.now(clock);
        m.startImplementation(now);
        Nis2RiskMeasure saved = repo.save(m);
        events.publish(saved, Nis2MeasureEventPublisher.Action.STARTED);
        return Nis2MeasureDto.View.of(saved, now);
    }

    public Nis2MeasureDto.View markImplemented(UUID id) {
        Nis2RiskMeasure m = loadForTenant(id);
        Instant now = Instant.now(clock);
        m.markImplemented(now);
        Nis2RiskMeasure saved = repo.save(m);
        events.publish(saved, Nis2MeasureEventPublisher.Action.IMPLEMENTED);
        return Nis2MeasureDto.View.of(saved, now);
    }

    public Nis2MeasureDto.View verify(UUID id, Nis2MeasureDto.VerifyRequest req) {
        Nis2RiskMeasure m = loadForTenant(id);
        Instant now = Instant.now(clock);
        m.verify(req.reviewedByUserId(), req.reviewedAt(), now);
        Nis2RiskMeasure saved = repo.save(m);
        events.publish(saved, Nis2MeasureEventPublisher.Action.VERIFIED);
        return Nis2MeasureDto.View.of(saved, now);
    }

    public Nis2MeasureDto.View review(UUID id, Nis2MeasureDto.ReviewRequest req) {
        Nis2RiskMeasure m = loadForTenant(id);
        Instant now = Instant.now(clock);
        m.review(req.reviewedByUserId(), req.reviewedAt(), now);
        Nis2RiskMeasure saved = repo.save(m);
        events.publish(saved, Nis2MeasureEventPublisher.Action.REVIEWED);
        return Nis2MeasureDto.View.of(saved, now);
    }

    public Nis2MeasureDto.View deprecate(UUID id) {
        Nis2RiskMeasure m = loadForTenant(id);
        Instant now = Instant.now(clock);
        m.deprecate(now);
        Nis2RiskMeasure saved = repo.save(m);
        events.publish(saved, Nis2MeasureEventPublisher.Action.DEPRECATED);
        return Nis2MeasureDto.View.of(saved, now);
    }

    public void delete(UUID id) {
        Nis2RiskMeasure m = loadForTenant(id);
        if (!m.isPlanned()) {
            throw new Nis2MeasureStateException(
                    "Only PLANNED measures can be deleted (other states preserved for audit)");
        }
        repo.delete(id);
        events.publish(m, Nis2MeasureEventPublisher.Action.DELETED);
    }

    public Nis2MeasureDto.View get(UUID id) {
        Instant now = Instant.now(clock);
        return Nis2MeasureDto.View.of(loadForTenant(id), now);
    }

    public List<Nis2MeasureDto.View> list(Nis2MeasureStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        Instant now = Instant.now(clock);
        List<Nis2RiskMeasure> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(m -> Nis2MeasureDto.View.of(m, now)).toList();
    }

    public List<Nis2MeasureDto.View> listByCategory(Nis2MeasureCategory category) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (category == null) {
            throw new Nis2MeasureStateException("category required");
        }
        Instant now = Instant.now(clock);
        return repo.findByTenantAndCategory(tenantId, category).stream()
                .map(m -> Nis2MeasureDto.View.of(m, now)).toList();
    }

    public Nis2MeasureDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new Nis2MeasureStateException("reference required");
        }
        Instant now = Instant.now(clock);
        return repo.findByTenantAndReference(tenantId, reference)
                .map(m -> Nis2MeasureDto.View.of(m, now))
                .orElseThrow(() -> new Nis2MeasureNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    public List<Nis2MeasureDto.View> reviewOverdue(int limit) {
        Instant now = Instant.now(clock);
        int capped = Math.max(1, Math.min(limit, 500));
        return repo.findReviewOverdue(now, capped).stream()
                .map(m -> Nis2MeasureDto.View.of(m, now)).toList();
    }

    private Nis2RiskMeasure loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        Nis2RiskMeasure m = repo.findById(id)
                .orElseThrow(() -> new Nis2MeasureNotFoundException(id));
        if (!m.getTenantId().equals(tenantId)) {
            throw new Nis2MeasureNotFoundException(id);
        }
        return m;
    }
}
