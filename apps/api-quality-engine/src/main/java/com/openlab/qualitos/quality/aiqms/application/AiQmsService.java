package com.openlab.qualitos.quality.aiqms.application;

import com.openlab.qualitos.quality.aiqms.domain.AiQms;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsNotFoundException;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsRepository;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStateException;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — QMS AI Act (Art. 17).
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class AiQmsService {

    private final AiQmsRepository repo;
    private final TenantProvider tenantProvider;
    private final AiQmsEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AiQmsService(AiQmsRepository repo, TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new AiQmsEventPublisher.NoOp(), clock);
    }

    public AiQmsService(AiQmsRepository repo, TenantProvider tenantProvider,
                        AiQmsEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public AiQmsDto.View draft(AiQmsDto.DraftRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new AiQmsStateException("createdByUserId required");
        }
        if (repo.existsByTenantAndReferenceAndVersion(tenantId, req.reference(), req.version())) {
            throw new AiQmsStateException(
                    "Reference+version already used: " + req.reference() + "/" + req.version());
        }
        Instant now = Instant.now(clock);
        AiQms q = AiQms.draft(tenantId, req.reference(), req.version(),
                req.name(), req.description(),
                req.regulatoryComplianceStrategy(), req.designControlDescription(),
                req.qualityControlDescription(), req.dataManagementDescription(),
                req.riskManagementDescription(), req.pmmDescription(),
                req.regulatorCommunicationDescription(),
                req.resourceManagementDescription(), req.supplierMonitoringDescription(),
                req.coveredAiSystemIds(), req.createdByUserId(), now);
        AiQms saved = repo.save(q);
        events.publish(saved, AiQmsEventPublisher.Action.DRAFTED);
        return AiQmsDto.View.of(saved);
    }

    public AiQmsDto.View edit(UUID id, AiQmsDto.EditRequest req) {
        AiQms q = loadForTenant(id);
        Instant now = Instant.now(clock);
        q.editDraft(req.name(), req.description(),
                req.regulatoryComplianceStrategy(), req.designControlDescription(),
                req.qualityControlDescription(), req.dataManagementDescription(),
                req.riskManagementDescription(), req.pmmDescription(),
                req.regulatorCommunicationDescription(),
                req.resourceManagementDescription(), req.supplierMonitoringDescription(),
                req.coveredAiSystemIds(), now);
        AiQms saved = repo.save(q);
        events.publish(saved, AiQmsEventPublisher.Action.EDITED);
        return AiQmsDto.View.of(saved);
    }

    public AiQmsDto.View approve(UUID id, AiQmsDto.ApproveRequest req) {
        AiQms q = loadForTenant(id);
        if (req.submittedByUserId() == null) {
            throw new AiQmsStateException("submittedByUserId required");
        }
        if (req.approvedByUserId() == null) {
            throw new AiQmsStateException("approvedByUserId required");
        }
        Instant now = Instant.now(clock);
        q.approve(req.submittedByUserId(), req.approvedByUserId(), req.approvalNotes(), now);
        AiQms saved = repo.save(q);
        events.publish(saved, AiQmsEventPublisher.Action.APPROVED);
        return AiQmsDto.View.of(saved);
    }

    public AiQmsDto.View putInForce(UUID id) {
        AiQms q = loadForTenant(id);
        Instant now = Instant.now(clock);
        q.putInForce(now);
        AiQms saved = repo.save(q);
        events.publish(saved, AiQmsEventPublisher.Action.IN_FORCE);
        return AiQmsDto.View.of(saved);
    }

    public AiQmsDto.View supersede(UUID id, AiQmsDto.SupersedeRequest req) {
        AiQms q = loadForTenant(id);
        if (req.supersededByQmsId() == null) {
            throw new AiQmsStateException("supersededByQmsId required");
        }
        // S'assurer que le QMS cible existe dans le tenant.
        loadForTenant(req.supersededByQmsId());
        Instant now = Instant.now(clock);
        q.supersede(req.supersededByQmsId(), now);
        AiQms saved = repo.save(q);
        events.publish(saved, AiQmsEventPublisher.Action.SUPERSEDED);
        return AiQmsDto.View.of(saved);
    }

    public AiQmsDto.View archive(UUID id, AiQmsDto.ArchiveRequest req) {
        AiQms q = loadForTenant(id);
        Instant now = Instant.now(clock);
        q.archive(req.reason(), now);
        AiQms saved = repo.save(q);
        events.publish(saved, AiQmsEventPublisher.Action.ARCHIVED);
        return AiQmsDto.View.of(saved);
    }

    public void delete(UUID id) {
        AiQms q = loadForTenant(id);
        if (!q.isDraft()) {
            throw new AiQmsStateException(
                    "Only DRAFT QMS can be deleted (others preserved for audit)");
        }
        repo.delete(id);
        events.publish(q, AiQmsEventPublisher.Action.DELETED);
    }

    public AiQmsDto.View get(UUID id) { return AiQmsDto.View.of(loadForTenant(id)); }

    public List<AiQmsDto.View> list(AiQmsStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<AiQms> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(AiQmsDto.View::of).toList();
    }

    public AiQmsDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new AiQmsStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(AiQmsDto.View::of)
                .orElseThrow(() -> new AiQmsNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private AiQms loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        AiQms q = repo.findById(id).orElseThrow(() -> new AiQmsNotFoundException(id));
        if (!q.getTenantId().equals(tenantId)) {
            throw new AiQmsNotFoundException(id);
        }
        return q;
    }
}
