package com.openlab.qualitos.quality.automateddecisions.application;

import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionNotFoundException;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRepository;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStateException;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — registre des décisions automatisées (RGPD Art. 22).
 *
 * Cross-tenant lookups yieldent 404 (OWASP A01).
 * Audit trail via {@link AutomatedDecisionEventPublisher} (OWASP A09).
 */
public class AutomatedDecisionService {

    private final AutomatedDecisionRepository repo;
    private final TenantProvider tenantProvider;
    private final AutomatedDecisionEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AutomatedDecisionService(AutomatedDecisionRepository repo,
                                    TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new AutomatedDecisionEventPublisher.NoOp(), clock);
    }

    public AutomatedDecisionService(AutomatedDecisionRepository repo,
                                    TenantProvider tenantProvider,
                                    AutomatedDecisionEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public AutomatedDecisionDto.View create(AutomatedDecisionDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new AutomatedDecisionStateException("createdByUserId required");
        }
        if (req.decisionType() == null) {
            throw new AutomatedDecisionStateException("decisionType required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new AutomatedDecisionStateException(
                    "Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(tenantId,
                req.reference(), req.name(), req.description(),
                req.decisionType(), req.art22LawfulBasis(), req.lawfulBasisDetails(),
                req.inputDataCategories(), req.linkedProcessingActivityIds(), req.linkedDpiaId(),
                req.algorithmDescription(), req.significanceForSubject(),
                req.humanReviewMechanism(), req.objectionMechanism(),
                req.createdByUserId(), now);
        AutomatedDecisionRecord saved = repo.save(r);
        events.publish(saved, AutomatedDecisionEventPublisher.Action.CREATED);
        return AutomatedDecisionDto.View.of(saved);
    }

    public AutomatedDecisionDto.View edit(UUID id, AutomatedDecisionDto.EditRequest req) {
        AutomatedDecisionRecord r = loadForTenant(id);
        if (req.decisionType() == null) {
            throw new AutomatedDecisionStateException("decisionType required");
        }
        Instant now = Instant.now(clock);
        r.editDraft(req.name(), req.description(),
                req.decisionType(), req.art22LawfulBasis(), req.lawfulBasisDetails(),
                req.inputDataCategories(), req.linkedProcessingActivityIds(), req.linkedDpiaId(),
                req.algorithmDescription(), req.significanceForSubject(),
                req.humanReviewMechanism(), req.objectionMechanism(), now);
        AutomatedDecisionRecord saved = repo.save(r);
        events.publish(saved, AutomatedDecisionEventPublisher.Action.EDITED);
        return AutomatedDecisionDto.View.of(saved);
    }

    public AutomatedDecisionDto.View activate(UUID id) {
        AutomatedDecisionRecord r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.activate(now);
        AutomatedDecisionRecord saved = repo.save(r);
        events.publish(saved, AutomatedDecisionEventPublisher.Action.ACTIVATED);
        return AutomatedDecisionDto.View.of(saved);
    }

    public AutomatedDecisionDto.View deprecate(UUID id) {
        AutomatedDecisionRecord r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.deprecate(now);
        AutomatedDecisionRecord saved = repo.save(r);
        events.publish(saved, AutomatedDecisionEventPublisher.Action.DEPRECATED);
        return AutomatedDecisionDto.View.of(saved);
    }

    public AutomatedDecisionDto.View archive(UUID id) {
        AutomatedDecisionRecord r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.archive(now);
        AutomatedDecisionRecord saved = repo.save(r);
        events.publish(saved, AutomatedDecisionEventPublisher.Action.ARCHIVED);
        return AutomatedDecisionDto.View.of(saved);
    }

    public void delete(UUID id) {
        AutomatedDecisionRecord r = loadForTenant(id);
        if (!r.isDraft()) {
            throw new AutomatedDecisionStateException(
                    "Only DRAFT records can be deleted (other states preserved for audit)");
        }
        repo.delete(id);
        events.publish(r, AutomatedDecisionEventPublisher.Action.DELETED);
    }

    public AutomatedDecisionDto.View get(UUID id) {
        return AutomatedDecisionDto.View.of(loadForTenant(id));
    }

    public List<AutomatedDecisionDto.View> list(AutomatedDecisionStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<AutomatedDecisionRecord> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(AutomatedDecisionDto.View::of).toList();
    }

    public AutomatedDecisionDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new AutomatedDecisionStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(AutomatedDecisionDto.View::of)
                .orElseThrow(() -> new AutomatedDecisionNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private AutomatedDecisionRecord loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        AutomatedDecisionRecord r = repo.findById(id)
                .orElseThrow(() -> new AutomatedDecisionNotFoundException(id));
        if (!r.getTenantId().equals(tenantId)) {
            throw new AutomatedDecisionNotFoundException(id);
        }
        return r;
    }
}
