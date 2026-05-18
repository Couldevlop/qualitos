package com.openlab.qualitos.quality.aieudb.application;

import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationNotFoundException;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationRepository;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStateException;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — enregistrements EUDB (AI Act Art. 49 / 71).
 * Cross-tenant 404 (OWASP A01). Audit trail (OWASP A09).
 */
public class EudbRegistrationService {

    private final EudbRegistrationRepository repo;
    private final TenantProvider tenantProvider;
    private final EudbRegistrationEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public EudbRegistrationService(EudbRegistrationRepository repo,
                                   TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new EudbRegistrationEventPublisher.NoOp(), clock);
    }

    public EudbRegistrationService(EudbRegistrationRepository repo,
                                   TenantProvider tenantProvider,
                                   EudbRegistrationEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public EudbRegistrationDto.View draft(EudbRegistrationDto.DraftRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new EudbRegistrationStateException("createdByUserId required");
        }
        if (req.aiSystemId() == null) {
            throw new EudbRegistrationStateException("aiSystemId required");
        }
        if (repo.existsByTenantAndReference(tenantId, req.reference())) {
            throw new EudbRegistrationStateException(
                    "Reference already used: " + req.reference());
        }
        Instant now = Instant.now(clock);
        EudbRegistration r = EudbRegistration.draft(tenantId, req.reference(), req.aiSystemId(),
                req.providerEntityName(), req.providerEuRepresentative(),
                req.memberStateOfReference(), req.intendedPurposeSummary(),
                req.technicalDocumentationReference(),
                req.createdByUserId(), now);
        EudbRegistration saved = repo.save(r);
        events.publish(saved, EudbRegistrationEventPublisher.Action.DRAFTED);
        return EudbRegistrationDto.View.of(saved);
    }

    public EudbRegistrationDto.View edit(UUID id, EudbRegistrationDto.EditRequest req) {
        EudbRegistration r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.editDraft(req.providerEntityName(), req.providerEuRepresentative(),
                req.memberStateOfReference(), req.intendedPurposeSummary(),
                req.technicalDocumentationReference(), now);
        EudbRegistration saved = repo.save(r);
        events.publish(saved, EudbRegistrationEventPublisher.Action.EDITED);
        return EudbRegistrationDto.View.of(saved);
    }

    public EudbRegistrationDto.View submit(UUID id, EudbRegistrationDto.SubmitRequest req) {
        EudbRegistration r = loadForTenant(id);
        if (req.submittedByUserId() == null) {
            throw new EudbRegistrationStateException("submittedByUserId required");
        }
        Instant now = Instant.now(clock);
        r.submit(req.submittedByUserId(), now);
        EudbRegistration saved = repo.save(r);
        events.publish(saved, EudbRegistrationEventPublisher.Action.SUBMITTED);
        return EudbRegistrationDto.View.of(saved);
    }

    public EudbRegistrationDto.View markRegistered(UUID id,
            EudbRegistrationDto.MarkRegisteredRequest req) {
        EudbRegistration r = loadForTenant(id);
        if (req.eudbId() == null || req.eudbId().isBlank()) {
            throw new EudbRegistrationStateException("eudbId required");
        }
        if (req.registrationDate() == null) {
            throw new EudbRegistrationStateException("registrationDate required");
        }
        Instant now = Instant.now(clock);
        r.markRegistered(req.eudbId(), req.registrationDate(), now);
        EudbRegistration saved = repo.save(r);
        events.publish(saved, EudbRegistrationEventPublisher.Action.REGISTERED);
        return EudbRegistrationDto.View.of(saved);
    }

    public EudbRegistrationDto.View declareUpdate(UUID id,
            EudbRegistrationDto.DeclareUpdateRequest req) {
        EudbRegistration r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.declareUpdate(req.updateSummary(), req.updateDate(), now);
        EudbRegistration saved = repo.save(r);
        events.publish(saved, EudbRegistrationEventPublisher.Action.UPDATED);
        return EudbRegistrationDto.View.of(saved);
    }

    public EudbRegistrationDto.View reject(UUID id, EudbRegistrationDto.RejectRequest req) {
        EudbRegistration r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.reject(req.reason(), now);
        EudbRegistration saved = repo.save(r);
        events.publish(saved, EudbRegistrationEventPublisher.Action.REJECTED);
        return EudbRegistrationDto.View.of(saved);
    }

    public EudbRegistrationDto.View retire(UUID id, EudbRegistrationDto.RetireRequest req) {
        EudbRegistration r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.retire(req.reason(), now);
        EudbRegistration saved = repo.save(r);
        events.publish(saved, EudbRegistrationEventPublisher.Action.RETIRED);
        return EudbRegistrationDto.View.of(saved);
    }

    public void delete(UUID id) {
        EudbRegistration r = loadForTenant(id);
        if (!r.isDraft()) {
            throw new EudbRegistrationStateException(
                    "Only DRAFT registrations can be deleted");
        }
        repo.delete(id);
        events.publish(r, EudbRegistrationEventPublisher.Action.DELETED);
    }

    public EudbRegistrationDto.View get(UUID id) {
        return EudbRegistrationDto.View.of(loadForTenant(id));
    }

    public List<EudbRegistrationDto.View> list(EudbRegistrationStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<EudbRegistration> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(EudbRegistrationDto.View::of).toList();
    }

    public List<EudbRegistrationDto.View> listByAiSystem(UUID aiSystemId) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (aiSystemId == null) {
            throw new EudbRegistrationStateException("aiSystemId required");
        }
        return repo.findByTenantAndAiSystemId(tenantId, aiSystemId).stream()
                .map(EudbRegistrationDto.View::of).toList();
    }

    public EudbRegistrationDto.View getByReference(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) {
            throw new EudbRegistrationStateException("reference required");
        }
        return repo.findByTenantAndReference(tenantId, reference)
                .map(EudbRegistrationDto.View::of)
                .orElseThrow(() -> new EudbRegistrationNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    public EudbRegistrationDto.View getByEudbId(String eudbId) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (eudbId == null || eudbId.isBlank()) {
            throw new EudbRegistrationStateException("eudbId required");
        }
        return repo.findByTenantAndEudbId(tenantId, eudbId)
                .map(EudbRegistrationDto.View::of)
                .orElseThrow(() -> new EudbRegistrationNotFoundException(
                        UUID.fromString("00000000-0000-0000-0000-000000000000")));
    }

    private EudbRegistration loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        EudbRegistration r = repo.findById(id)
                .orElseThrow(() -> new EudbRegistrationNotFoundException(id));
        if (!r.getTenantId().equals(tenantId)) {
            throw new EudbRegistrationNotFoundException(id);
        }
        return r;
    }
}
