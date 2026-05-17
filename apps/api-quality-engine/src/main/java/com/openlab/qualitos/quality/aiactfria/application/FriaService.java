package com.openlab.qualitos.quality.aiactfria.application;

import com.openlab.qualitos.quality.aiactfria.domain.Fria;
import com.openlab.qualitos.quality.aiactfria.domain.FriaNotFoundException;
import com.openlab.qualitos.quality.aiactfria.domain.FriaRepository;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStateException;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — FRIA (AI Act Art. 27).
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class FriaService {

    private final FriaRepository repo;
    private final TenantProvider tenantProvider;
    private final FriaEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public FriaService(FriaRepository repo, TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new FriaEventPublisher.NoOp(), clock);
    }

    public FriaService(FriaRepository repo, TenantProvider tenantProvider,
                       FriaEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public FriaDto.View draft(FriaDto.DraftRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new FriaStateException("createdByUserId required");
        }
        if (req.aiSystemId() == null) {
            throw new FriaStateException("aiSystemId required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new FriaStateException("Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        Fria f = Fria.draft(tenantId, req.reference(), req.aiSystemId(),
                req.processDescription(), req.deploymentDurationDescription(),
                req.affectedPersonsCategories(), req.specificRisks(),
                req.mitigationMeasures(), req.humanOversightMeasures(),
                req.complaintMechanismDescription(), req.createdByUserId(), now);
        Fria saved = repo.save(f);
        events.publish(saved, FriaEventPublisher.Action.DRAFTED);
        return FriaDto.View.of(saved);
    }

    public FriaDto.View edit(UUID id, FriaDto.EditRequest req) {
        Fria f = loadForTenant(id);
        Instant now = Instant.now(clock);
        f.editDraft(req.processDescription(), req.deploymentDurationDescription(),
                req.affectedPersonsCategories(), req.specificRisks(),
                req.mitigationMeasures(), req.humanOversightMeasures(),
                req.complaintMechanismDescription(), now);
        Fria saved = repo.save(f);
        events.publish(saved, FriaEventPublisher.Action.EDITED);
        return FriaDto.View.of(saved);
    }

    public FriaDto.View submit(UUID id, FriaDto.SubmitRequest req) {
        Fria f = loadForTenant(id);
        if (req.submittedByUserId() == null) {
            throw new FriaStateException("submittedByUserId required");
        }
        Instant now = Instant.now(clock);
        f.submit(req.submittedByUserId(), now);
        Fria saved = repo.save(f);
        events.publish(saved, FriaEventPublisher.Action.SUBMITTED);
        return FriaDto.View.of(saved);
    }

    public FriaDto.View approve(UUID id, FriaDto.ApproveRequest req) {
        Fria f = loadForTenant(id);
        if (req.approvedByUserId() == null) {
            throw new FriaStateException("approvedByUserId required");
        }
        Instant now = Instant.now(clock);
        f.approve(req.approvedByUserId(), req.approvalNotes(), now);
        Fria saved = repo.save(f);
        events.publish(saved, FriaEventPublisher.Action.APPROVED);
        return FriaDto.View.of(saved);
    }

    public FriaDto.View returnToDraft(UUID id, FriaDto.ReturnRequest req) {
        Fria f = loadForTenant(id);
        Instant now = Instant.now(clock);
        f.returnToDraft(req.reason(), now);
        Fria saved = repo.save(f);
        events.publish(saved, FriaEventPublisher.Action.RETURNED_TO_DRAFT);
        return FriaDto.View.of(saved);
    }

    public FriaDto.View archive(UUID id, FriaDto.ArchiveRequest req) {
        Fria f = loadForTenant(id);
        Instant now = Instant.now(clock);
        f.archive(req.reason(), now);
        Fria saved = repo.save(f);
        events.publish(saved, FriaEventPublisher.Action.ARCHIVED);
        return FriaDto.View.of(saved);
    }

    public void delete(UUID id) {
        Fria f = loadForTenant(id);
        if (!f.isDraft()) {
            throw new FriaStateException(
                    "Only DRAFT FRIA can be deleted (other states preserved for audit)");
        }
        repo.delete(id);
        events.publish(f, FriaEventPublisher.Action.DELETED);
    }

    public FriaDto.View get(UUID id) { return FriaDto.View.of(loadForTenant(id)); }

    public List<FriaDto.View> list(FriaStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<Fria> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(FriaDto.View::of).toList();
    }

    public List<FriaDto.View> listByAiSystem(UUID aiSystemId) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (aiSystemId == null) throw new FriaStateException("aiSystemId required");
        return repo.findByTenantAndAiSystemId(tenantId, aiSystemId).stream()
                .map(FriaDto.View::of).toList();
    }

    public FriaDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new FriaStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(FriaDto.View::of)
                .orElseThrow(() -> new FriaNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private Fria loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        Fria f = repo.findById(id).orElseThrow(() -> new FriaNotFoundException(id));
        if (!f.getTenantId().equals(tenantId)) {
            throw new FriaNotFoundException(id);
        }
        return f;
    }
}
