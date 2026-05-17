package com.openlab.qualitos.quality.aiconformity.application;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentNotFoundException;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentRepository;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStateException;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — évaluations de conformité AI Act (Art. 43).
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class ConformityAssessmentService {

    private final ConformityAssessmentRepository repo;
    private final TenantProvider tenantProvider;
    private final ConformityAssessmentEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ConformityAssessmentService(ConformityAssessmentRepository repo,
                                       TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new ConformityAssessmentEventPublisher.NoOp(), clock);
    }

    public ConformityAssessmentService(ConformityAssessmentRepository repo,
                                       TenantProvider tenantProvider,
                                       ConformityAssessmentEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public ConformityAssessmentDto.View plan(ConformityAssessmentDto.PlanRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new ConformityAssessmentStateException("createdByUserId required");
        }
        if (req.aiSystemId() == null) {
            throw new ConformityAssessmentStateException("aiSystemId required");
        }
        if (req.procedure() == null) {
            throw new ConformityAssessmentStateException("procedure required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new ConformityAssessmentStateException(
                    "Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        ConformityAssessment a = ConformityAssessment.plan(tenantId, req.reference(),
                req.aiSystemId(), req.qmsId(), req.procedure(),
                req.notifiedBodyId(), req.notifiedBodyName(), req.scope(),
                req.createdByUserId(), now);
        ConformityAssessment saved = repo.save(a);
        events.publish(saved, ConformityAssessmentEventPublisher.Action.PLANNED);
        return ConformityAssessmentDto.View.of(saved);
    }

    public ConformityAssessmentDto.View edit(UUID id, ConformityAssessmentDto.EditRequest req) {
        ConformityAssessment a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.editPlanned(req.qmsId(), req.notifiedBodyId(), req.notifiedBodyName(),
                req.scope(), now);
        ConformityAssessment saved = repo.save(a);
        events.publish(saved, ConformityAssessmentEventPublisher.Action.EDITED);
        return ConformityAssessmentDto.View.of(saved);
    }

    public ConformityAssessmentDto.View start(UUID id) {
        ConformityAssessment a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.start(now);
        ConformityAssessment saved = repo.save(a);
        events.publish(saved, ConformityAssessmentEventPublisher.Action.STARTED);
        return ConformityAssessmentDto.View.of(saved);
    }

    public ConformityAssessmentDto.View certify(UUID id,
            ConformityAssessmentDto.CertifyRequest req) {
        ConformityAssessment a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.certify(req.certificateNumber(), req.euDeclarationReference(),
                req.validUntil(), now);
        ConformityAssessment saved = repo.save(a);
        events.publish(saved, ConformityAssessmentEventPublisher.Action.CERTIFIED);
        return ConformityAssessmentDto.View.of(saved);
    }

    public ConformityAssessmentDto.View markExpired(UUID id) {
        ConformityAssessment a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.markExpired(now);
        ConformityAssessment saved = repo.save(a);
        events.publish(saved, ConformityAssessmentEventPublisher.Action.EXPIRED);
        return ConformityAssessmentDto.View.of(saved);
    }

    public ConformityAssessmentDto.View revoke(UUID id,
            ConformityAssessmentDto.RevokeRequest req) {
        ConformityAssessment a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.revoke(req.reason(), now);
        ConformityAssessment saved = repo.save(a);
        events.publish(saved, ConformityAssessmentEventPublisher.Action.REVOKED);
        return ConformityAssessmentDto.View.of(saved);
    }

    public ConformityAssessmentDto.View markFailed(UUID id,
            ConformityAssessmentDto.FailRequest req) {
        ConformityAssessment a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.markFailed(req.reason(), now);
        ConformityAssessment saved = repo.save(a);
        events.publish(saved, ConformityAssessmentEventPublisher.Action.FAILED);
        return ConformityAssessmentDto.View.of(saved);
    }

    public void delete(UUID id) {
        ConformityAssessment a = loadForTenant(id);
        if (!a.isPlanned()) {
            throw new ConformityAssessmentStateException(
                    "Only PLANNED assessments can be deleted");
        }
        repo.delete(id);
        events.publish(a, ConformityAssessmentEventPublisher.Action.DELETED);
    }

    public ConformityAssessmentDto.View get(UUID id) {
        return ConformityAssessmentDto.View.of(loadForTenant(id));
    }

    public List<ConformityAssessmentDto.View> list(ConformityAssessmentStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<ConformityAssessment> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(ConformityAssessmentDto.View::of).toList();
    }

    public List<ConformityAssessmentDto.View> listByAiSystem(UUID aiSystemId) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (aiSystemId == null) {
            throw new ConformityAssessmentStateException("aiSystemId required");
        }
        return repo.findByTenantAndAiSystemId(tenantId, aiSystemId).stream()
                .map(ConformityAssessmentDto.View::of).toList();
    }

    public List<ConformityAssessmentDto.View> listExpiringCertificates(int limit) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (limit < 1 || limit > 1000) {
            throw new ConformityAssessmentStateException("limit must be in [1, 1000]");
        }
        return repo.findExpiringCertificates(tenantId, Instant.now(clock), limit).stream()
                .map(ConformityAssessmentDto.View::of).toList();
    }

    public ConformityAssessmentDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new ConformityAssessmentStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(ConformityAssessmentDto.View::of)
                .orElseThrow(() -> new ConformityAssessmentNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private ConformityAssessment loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        ConformityAssessment a = repo.findById(id)
                .orElseThrow(() -> new ConformityAssessmentNotFoundException(id));
        if (!a.getTenantId().equals(tenantId)) {
            throw new ConformityAssessmentNotFoundException(id);
        }
        return a;
    }
}
