package com.openlab.qualitos.quality.processoragreements.application;

import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementNotFoundException;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementRepository;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStateException;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — accords de sous-traitance RGPD (Art. 28).
 *
 * Cross-tenant lookups yieldent 404 (OWASP A01).
 * Audit trail via {@link ProcessorAgreementEventPublisher} (OWASP A09).
 */
public class ProcessorAgreementService {

    private final ProcessorAgreementRepository repo;
    private final TenantProvider tenantProvider;
    private final ProcessorAgreementEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ProcessorAgreementService(ProcessorAgreementRepository repo,
                                     TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new ProcessorAgreementEventPublisher.NoOp(), clock);
    }

    public ProcessorAgreementService(ProcessorAgreementRepository repo,
                                     TenantProvider tenantProvider,
                                     ProcessorAgreementEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public ProcessorAgreementDto.View create(ProcessorAgreementDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new ProcessorAgreementStateException("createdByUserId required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new ProcessorAgreementStateException(
                    "Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        ProcessorAgreement a = ProcessorAgreement.draft(tenantId, req.reference(),
                req.processorName(), req.processorLegalEntity(),
                req.processorContact(), req.processorDpoContact(),
                req.processorCountry(), req.servicesDescription(),
                req.subProcessorCategories(), req.linkedProcessingActivityIds(),
                req.thirdCountryTransfers(), req.transferSafeguards(),
                req.contractDocumentUrl(),
                req.signedAt(), req.effectiveFrom(), req.expirationDate(),
                req.securityMeasures(), req.breachNotificationCommitmentHours(),
                req.auditRights(), req.auditRightsNotes(),
                req.dataReturnOrDeletionTerms(), req.createdByUserId(), now);
        ProcessorAgreement saved = repo.save(a);
        events.publish(saved, ProcessorAgreementEventPublisher.Action.CREATED);
        return ProcessorAgreementDto.View.of(saved, now);
    }

    public ProcessorAgreementDto.View edit(UUID id, ProcessorAgreementDto.EditRequest req) {
        ProcessorAgreement a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.editDraft(req.processorName(), req.processorLegalEntity(),
                req.processorContact(), req.processorDpoContact(),
                req.processorCountry(), req.servicesDescription(),
                req.subProcessorCategories(), req.linkedProcessingActivityIds(),
                req.thirdCountryTransfers(), req.transferSafeguards(),
                req.contractDocumentUrl(),
                req.signedAt(), req.effectiveFrom(), req.expirationDate(),
                req.securityMeasures(), req.breachNotificationCommitmentHours(),
                req.auditRights(), req.auditRightsNotes(),
                req.dataReturnOrDeletionTerms(), now);
        ProcessorAgreement saved = repo.save(a);
        events.publish(saved, ProcessorAgreementEventPublisher.Action.EDITED);
        return ProcessorAgreementDto.View.of(saved, now);
    }

    public ProcessorAgreementDto.View activate(UUID id) {
        ProcessorAgreement a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.activate(now);
        ProcessorAgreement saved = repo.save(a);
        events.publish(saved, ProcessorAgreementEventPublisher.Action.ACTIVATED);
        return ProcessorAgreementDto.View.of(saved, now);
    }

    public ProcessorAgreementDto.View terminate(UUID id, ProcessorAgreementDto.TerminateRequest req) {
        ProcessorAgreement a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.terminate(req.reason(), now);
        ProcessorAgreement saved = repo.save(a);
        events.publish(saved, ProcessorAgreementEventPublisher.Action.TERMINATED);
        return ProcessorAgreementDto.View.of(saved, now);
    }

    public void delete(UUID id) {
        ProcessorAgreement a = loadForTenant(id);
        if (!a.isDraft()) {
            throw new ProcessorAgreementStateException(
                    "Only DRAFT agreements can be deleted (other states preserved for audit)");
        }
        repo.delete(id);
        events.publish(a, ProcessorAgreementEventPublisher.Action.DELETED);
    }

    public ProcessorAgreementDto.View get(UUID id) {
        Instant now = Instant.now(clock);
        return ProcessorAgreementDto.View.of(loadForTenant(id), now);
    }

    public List<ProcessorAgreementDto.View> list(ProcessorAgreementStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        Instant now = Instant.now(clock);
        List<ProcessorAgreement> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(a -> ProcessorAgreementDto.View.of(a, now)).toList();
    }

    public ProcessorAgreementDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new ProcessorAgreementStateException("reference required");
        }
        Instant now = Instant.now(clock);
        return repo.findByTenantAndReference(tenantId, reference)
                .map(a -> ProcessorAgreementDto.View.of(a, now))
                .orElseThrow(() -> new ProcessorAgreementNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    /** Scan d'expiration — marque comme EXPIRED les accords dont la date est passée. */
    public int expireDue(int limit) {
        Instant now = Instant.now(clock);
        int capped = Math.max(1, Math.min(limit, 500));
        List<ProcessorAgreement> due = repo.findExpirable(now, capped);
        int n = 0;
        for (ProcessorAgreement a : due) {
            a.expireIfDue(now);
            if (a.getStatus() == ProcessorAgreementStatus.EXPIRED) {
                repo.save(a);
                events.publish(a, ProcessorAgreementEventPublisher.Action.EXPIRED);
                n++;
            }
        }
        return n;
    }

    private ProcessorAgreement loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        ProcessorAgreement a = repo.findById(id)
                .orElseThrow(() -> new ProcessorAgreementNotFoundException(id));
        if (!a.getTenantId().equals(tenantId)) {
            throw new ProcessorAgreementNotFoundException(id);
        }
        return a;
    }
}
