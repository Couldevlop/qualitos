package com.openlab.qualitos.quality.dpia.application;

import com.openlab.qualitos.quality.dpia.domain.Dpia;
import com.openlab.qualitos.quality.dpia.domain.DpiaNotFoundException;
import com.openlab.qualitos.quality.dpia.domain.DpiaRepository;
import com.openlab.qualitos.quality.dpia.domain.DpiaStateException;
import com.openlab.qualitos.quality.dpia.domain.DpiaStatus;
import com.openlab.qualitos.quality.dpia.domain.RiskLevel;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — DPIA (Art. 35).
 *
 * Cross-tenant lookups yieldent 404 (OWASP A01).
 * Audit trail via {@link DpiaEventPublisher} (OWASP A09).
 */
public class DpiaService {

    private final DpiaRepository repo;
    private final TenantProvider tenantProvider;
    private final DpiaEventPublisher events;
    private final Clock clock;

    public DpiaService(DpiaRepository repo, TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new DpiaEventPublisher.NoOp(), clock);
    }

    public DpiaService(DpiaRepository repo, TenantProvider tenantProvider,
                       DpiaEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public DpiaDto.View create(DpiaDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new DpiaStateException("createdByUserId required");
        }
        if (req.initialRiskLevel() == null) {
            throw new DpiaStateException("initialRiskLevel required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new DpiaStateException("Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        Dpia d = Dpia.draft(tenantId, req.reference(), req.title(), req.description(),
                req.linkedProcessingActivityIds(), req.initialRiskLevel(),
                req.createdByUserId(), now);
        Dpia saved = repo.save(d);
        events.publish(saved, DpiaEventPublisher.Action.CREATED);
        return DpiaDto.View.of(saved);
    }

    public DpiaDto.View edit(UUID id, DpiaDto.EditRequest req) {
        Dpia d = loadForTenant(id);
        if (req.overallRiskLevel() == null) {
            throw new DpiaStateException("overallRiskLevel required");
        }
        Instant now = Instant.now(clock);
        d.editDraft(req.title(), req.description(),
                req.linkedProcessingActivityIds(),
                req.necessityAndProportionalityNotes(),
                req.risksToRightsAndFreedoms(),
                req.mitigationMeasures(),
                req.overallRiskLevel(),
                req.consultationRequired(), req.consultationNotes(), now);
        Dpia saved = repo.save(d);
        events.publish(saved, DpiaEventPublisher.Action.EDITED);
        return DpiaDto.View.of(saved);
    }

    public DpiaDto.View start(UUID id, DpiaDto.StartRequest req) {
        Dpia d = loadForTenant(id);
        Instant now = Instant.now(clock);
        d.start(req.handledByUserId(), now);
        Dpia saved = repo.save(d);
        events.publish(saved, DpiaEventPublisher.Action.STARTED);
        return DpiaDto.View.of(saved);
    }

    public DpiaDto.View returnToDraft(UUID id) {
        Dpia d = loadForTenant(id);
        Instant now = Instant.now(clock);
        d.returnToDraft(now);
        Dpia saved = repo.save(d);
        events.publish(saved, DpiaEventPublisher.Action.RETURNED_TO_DRAFT);
        return DpiaDto.View.of(saved);
    }

    public DpiaDto.View submitToDpo(UUID id) {
        Dpia d = loadForTenant(id);
        Instant now = Instant.now(clock);
        d.submitToDpo(now);
        Dpia saved = repo.save(d);
        events.publish(saved, DpiaEventPublisher.Action.SUBMITTED_TO_DPO);
        return DpiaDto.View.of(saved);
    }

    public DpiaDto.View approve(UUID id, DpiaDto.OpinionRequest req) {
        Dpia d = loadForTenant(id);
        Instant now = Instant.now(clock);
        d.approve(req.dpoUserId(), req.dpoOpinion(), now);
        Dpia saved = repo.save(d);
        events.publish(saved, DpiaEventPublisher.Action.APPROVED);
        return DpiaDto.View.of(saved);
    }

    public DpiaDto.View reject(UUID id, DpiaDto.OpinionRequest req) {
        Dpia d = loadForTenant(id);
        Instant now = Instant.now(clock);
        d.reject(req.dpoUserId(), req.dpoOpinion(), now);
        Dpia saved = repo.save(d);
        events.publish(saved, DpiaEventPublisher.Action.REJECTED);
        return DpiaDto.View.of(saved);
    }

    public DpiaDto.View archive(UUID id) {
        Dpia d = loadForTenant(id);
        Instant now = Instant.now(clock);
        d.archive(now);
        Dpia saved = repo.save(d);
        events.publish(saved, DpiaEventPublisher.Action.ARCHIVED);
        return DpiaDto.View.of(saved);
    }

    public void delete(UUID id) {
        Dpia d = loadForTenant(id);
        if (!d.isDraft()) {
            throw new DpiaStateException(
                    "Only DRAFT DPIAs can be deleted (other states preserved for audit)");
        }
        repo.delete(id);
        events.publish(d, DpiaEventPublisher.Action.DELETED);
    }

    public DpiaDto.View get(UUID id) { return DpiaDto.View.of(loadForTenant(id)); }

    public List<DpiaDto.View> list(DpiaStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<Dpia> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(DpiaDto.View::of).toList();
    }

    public DpiaDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new DpiaStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(DpiaDto.View::of)
                .orElseThrow(() -> new DpiaNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    /** Liste les DPIA non encore approuvées avec un risque résiduel HIGH/SEVERE. */
    public List<DpiaDto.View> requiringConsultation() {
        UUID tenantId = tenantProvider.requireTenantId();
        return repo.findByTenant(tenantId).stream()
                .filter(d -> d.getOverallRiskLevel() == RiskLevel.HIGH
                        || d.getOverallRiskLevel() == RiskLevel.SEVERE)
                .filter(d -> d.getStatus() != DpiaStatus.REJECTED
                        && d.getStatus() != DpiaStatus.ARCHIVED)
                .map(DpiaDto.View::of)
                .toList();
    }

    private Dpia loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        Dpia d = repo.findById(id)
                .orElseThrow(() -> new DpiaNotFoundException(id));
        if (!d.getTenantId().equals(tenantId)) throw new DpiaNotFoundException(id);
        return d;
    }
}
