package com.openlab.qualitos.quality.ropa.application;

import com.openlab.qualitos.quality.ropa.domain.ProcessingActivity;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityNotFoundException;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityRepository;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStateException;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — registre des traitements RGPD (Art. 30).
 *
 * Cross-tenant lookups yieldent 404 (OWASP A01 — no info leak).
 * Audit trail via {@link ProcessingActivityEventPublisher} (OWASP A09).
 */
public class ProcessingActivityService {

    private final ProcessingActivityRepository repo;
    private final TenantProvider tenantProvider;
    private final ProcessingActivityEventPublisher events;
    private final Clock clock;

    public ProcessingActivityService(ProcessingActivityRepository repo,
                                     TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new ProcessingActivityEventPublisher.NoOp(), clock);
    }

    public ProcessingActivityService(ProcessingActivityRepository repo,
                                     TenantProvider tenantProvider,
                                     ProcessingActivityEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public ProcessingActivityDto.View create(ProcessingActivityDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new ProcessingActivityStateException("createdByUserId required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new ProcessingActivityStateException(
                    "Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        ProcessingActivity a = ProcessingActivity.draft(tenantId,
                req.reference(), req.name(), req.purposes(),
                req.lawfulBasis(), req.lawfulBasisDetails(),
                req.controllerName(), req.controllerContact(), req.dpoContact(),
                req.jointControllerName(), req.jointControllerContact(),
                req.dataSubjectCategories(), req.dataCategories(),
                req.specialCategoriesProcessed(), req.specialCategoriesJustification(),
                req.recipientCategories(), req.thirdCountryTransfers(),
                req.transferSafeguards(), req.linkedRetentionRuleIds(),
                req.technicalMeasures(), req.organizationalMeasures(),
                req.createdByUserId(), now);
        ProcessingActivity saved = repo.save(a);
        events.publish(saved, ProcessingActivityEventPublisher.Action.CREATED);
        return ProcessingActivityDto.View.of(saved);
    }

    public ProcessingActivityDto.View edit(UUID id, ProcessingActivityDto.EditRequest req) {
        ProcessingActivity a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.editDraft(req.name(), req.purposes(),
                req.lawfulBasis(), req.lawfulBasisDetails(),
                req.controllerName(), req.controllerContact(), req.dpoContact(),
                req.jointControllerName(), req.jointControllerContact(),
                req.dataSubjectCategories(), req.dataCategories(),
                req.specialCategoriesProcessed(), req.specialCategoriesJustification(),
                req.recipientCategories(), req.thirdCountryTransfers(),
                req.transferSafeguards(), req.linkedRetentionRuleIds(),
                req.technicalMeasures(), req.organizationalMeasures(), now);
        ProcessingActivity saved = repo.save(a);
        events.publish(saved, ProcessingActivityEventPublisher.Action.EDITED);
        return ProcessingActivityDto.View.of(saved);
    }

    public ProcessingActivityDto.View activate(UUID id) {
        ProcessingActivity a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.activate(now);
        ProcessingActivity saved = repo.save(a);
        events.publish(saved, ProcessingActivityEventPublisher.Action.ACTIVATED);
        return ProcessingActivityDto.View.of(saved);
    }

    public ProcessingActivityDto.View archive(UUID id) {
        ProcessingActivity a = loadForTenant(id);
        Instant now = Instant.now(clock);
        a.archive(now);
        ProcessingActivity saved = repo.save(a);
        events.publish(saved, ProcessingActivityEventPublisher.Action.ARCHIVED);
        return ProcessingActivityDto.View.of(saved);
    }

    public void delete(UUID id) {
        ProcessingActivity a = loadForTenant(id);
        if (!a.isDraft()) {
            throw new ProcessingActivityStateException(
                    "Only DRAFT activities can be deleted (active/archived preserved for audit)");
        }
        repo.delete(id);
        events.publish(a, ProcessingActivityEventPublisher.Action.DELETED);
    }

    public ProcessingActivityDto.View get(UUID id) {
        return ProcessingActivityDto.View.of(loadForTenant(id));
    }

    public List<ProcessingActivityDto.View> list(ProcessingActivityStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<ProcessingActivity> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(ProcessingActivityDto.View::of).toList();
    }

    public ProcessingActivityDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new ProcessingActivityStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(ProcessingActivityDto.View::of)
                .orElseThrow(() -> new ProcessingActivityNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private ProcessingActivity loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        ProcessingActivity a = repo.findById(id)
                .orElseThrow(() -> new ProcessingActivityNotFoundException(id));
        if (!a.getTenantId().equals(tenantId)) {
            throw new ProcessingActivityNotFoundException(id);
        }
        return a;
    }
}
