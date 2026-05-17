package com.openlab.qualitos.quality.crossbordertransfers.application;

import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferNotFoundException;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferRepository;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStateException;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — transferts internationaux (RGPD Chap. V).
 * Cross-tenant lookups yieldent 404 (OWASP A01).
 * Audit trail via {@link CrossBorderTransferEventPublisher} (OWASP A09).
 */
public class CrossBorderTransferService {

    private final CrossBorderTransferRepository repo;
    private final TenantProvider tenantProvider;
    private final CrossBorderTransferEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public CrossBorderTransferService(CrossBorderTransferRepository repo,
                                      TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new CrossBorderTransferEventPublisher.NoOp(), clock);
    }

    public CrossBorderTransferService(CrossBorderTransferRepository repo,
                                      TenantProvider tenantProvider,
                                      CrossBorderTransferEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public CrossBorderTransferDto.View create(CrossBorderTransferDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new CrossBorderTransferStateException("createdByUserId required");
        }
        if (req.mechanism() == null) {
            throw new CrossBorderTransferStateException("mechanism required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new CrossBorderTransferStateException(
                    "Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        CrossBorderTransfer t = CrossBorderTransfer.draft(tenantId, req.reference(),
                req.recipientName(), req.recipientLegalEntity(), req.recipientContact(),
                req.destinationCountries(), req.mechanism(),
                req.safeguardsDescription(), req.safeguardsDocumentUrl(),
                req.derogationJustification(),
                req.dataCategories(),
                req.linkedProcessingActivityIds(), req.linkedProcessorAgreementIds(),
                req.createdByUserId(), now);
        CrossBorderTransfer saved = repo.save(t);
        events.publish(saved, CrossBorderTransferEventPublisher.Action.CREATED);
        return CrossBorderTransferDto.View.of(saved);
    }

    public CrossBorderTransferDto.View edit(UUID id, CrossBorderTransferDto.EditRequest req) {
        CrossBorderTransfer t = loadForTenant(id);
        if (req.mechanism() == null) {
            throw new CrossBorderTransferStateException("mechanism required");
        }
        Instant now = Instant.now(clock);
        t.editDraft(req.recipientName(), req.recipientLegalEntity(), req.recipientContact(),
                req.destinationCountries(), req.mechanism(),
                req.safeguardsDescription(), req.safeguardsDocumentUrl(),
                req.derogationJustification(),
                req.dataCategories(),
                req.linkedProcessingActivityIds(), req.linkedProcessorAgreementIds(), now);
        CrossBorderTransfer saved = repo.save(t);
        events.publish(saved, CrossBorderTransferEventPublisher.Action.EDITED);
        return CrossBorderTransferDto.View.of(saved);
    }

    public CrossBorderTransferDto.View activate(UUID id) {
        CrossBorderTransfer t = loadForTenant(id);
        boolean wasSuspended = t.isSuspended();
        Instant now = Instant.now(clock);
        t.activate(now);
        CrossBorderTransfer saved = repo.save(t);
        events.publish(saved, wasSuspended
                ? CrossBorderTransferEventPublisher.Action.REACTIVATED
                : CrossBorderTransferEventPublisher.Action.ACTIVATED);
        return CrossBorderTransferDto.View.of(saved);
    }

    public CrossBorderTransferDto.View suspend(UUID id, CrossBorderTransferDto.SuspendRequest req) {
        CrossBorderTransfer t = loadForTenant(id);
        Instant now = Instant.now(clock);
        t.suspend(req.reason(), now);
        CrossBorderTransfer saved = repo.save(t);
        events.publish(saved, CrossBorderTransferEventPublisher.Action.SUSPENDED);
        return CrossBorderTransferDto.View.of(saved);
    }

    public CrossBorderTransferDto.View terminate(UUID id, CrossBorderTransferDto.TerminateRequest req) {
        CrossBorderTransfer t = loadForTenant(id);
        Instant now = Instant.now(clock);
        t.terminate(req.reason(), now);
        CrossBorderTransfer saved = repo.save(t);
        events.publish(saved, CrossBorderTransferEventPublisher.Action.TERMINATED);
        return CrossBorderTransferDto.View.of(saved);
    }

    public void delete(UUID id) {
        CrossBorderTransfer t = loadForTenant(id);
        if (!t.isDraft()) {
            throw new CrossBorderTransferStateException(
                    "Only DRAFT transfers can be deleted (other states preserved for audit)");
        }
        repo.delete(id);
        events.publish(t, CrossBorderTransferEventPublisher.Action.DELETED);
    }

    public CrossBorderTransferDto.View get(UUID id) {
        return CrossBorderTransferDto.View.of(loadForTenant(id));
    }

    public List<CrossBorderTransferDto.View> list(CrossBorderTransferStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<CrossBorderTransfer> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(CrossBorderTransferDto.View::of).toList();
    }

    public CrossBorderTransferDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new CrossBorderTransferStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(CrossBorderTransferDto.View::of)
                .orElseThrow(() -> new CrossBorderTransferNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private CrossBorderTransfer loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        CrossBorderTransfer t = repo.findById(id)
                .orElseThrow(() -> new CrossBorderTransferNotFoundException(id));
        if (!t.getTenantId().equals(tenantId)) {
            throw new CrossBorderTransferNotFoundException(id);
        }
        return t;
    }
}
