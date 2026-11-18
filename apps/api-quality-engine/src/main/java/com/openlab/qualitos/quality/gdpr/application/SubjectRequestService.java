package com.openlab.qualitos.quality.gdpr.application;

import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequest;
import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequestRepository;
import com.openlab.qualitos.quality.gdpr.domain.SubjectIdentifierHasher;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestNotFoundException;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStateException;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestStatus;
import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestType;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases RGPD. La PII (subjectIdentifier) est hashée par
 * {@link SubjectIdentifierHasher} avant tout stockage (privacy by design).
 * Cross-tenant lookups yieldent 404 (no info leak).
 */
public class SubjectRequestService {

    private final DataSubjectRequestRepository repo;
    private final SubjectIdentifierHasher hasher;
    private final TenantProvider tenantProvider;
    private final SubjectRequestEventPublisher events;
    private final Clock clock;

    public SubjectRequestService(DataSubjectRequestRepository repo,
                                 SubjectIdentifierHasher hasher,
                                 TenantProvider tenantProvider, Clock clock) {
        this(repo, hasher, tenantProvider, new SubjectRequestEventPublisher.NoOp(), clock);
    }

    public SubjectRequestService(DataSubjectRequestRepository repo,
                                 SubjectIdentifierHasher hasher,
                                 TenantProvider tenantProvider,
                                 SubjectRequestEventPublisher events, Clock clock) {
        this.repo = repo;
        this.hasher = hasher;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public SubjectRequestDto.View receive(SubjectRequestDto.ReceiveRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.subjectIdentifier() == null || req.subjectIdentifier().isBlank()) {
            throw new SubjectRequestStateException("subjectIdentifier required");
        }
        if (req.type() == null) {
            throw new SubjectRequestStateException("type required");
        }
        Instant now = Instant.now(clock);
        String hash = hasher.hash(req.subjectIdentifier().trim().toLowerCase());
        DataSubjectRequest r = DataSubjectRequest.receive(tenantId, req.type(), hash,
                req.subjectIdentifierLabel(), req.requestedByUserId(), now);
        DataSubjectRequest saved = repo.save(r);
        events.publish(saved, SubjectRequestEventPublisher.Action.RECEIVED);
        return SubjectRequestDto.View.of(saved, now);
    }

    public SubjectRequestDto.View startProcessing(UUID id,
                                                  SubjectRequestDto.StartProcessingRequest req) {
        DataSubjectRequest r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.startProcessing(req.handledByUserId(), now);
        DataSubjectRequest saved = repo.save(r);
        events.publish(saved, SubjectRequestEventPublisher.Action.IN_PROGRESS);
        return SubjectRequestDto.View.of(saved, now);
    }

    public SubjectRequestDto.View complete(UUID id, SubjectRequestDto.CompleteRequest req) {
        DataSubjectRequest r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.complete(req.resolutionNotes(), req.evidenceUrl(), req.handledByUserId(), now);
        DataSubjectRequest saved = repo.save(r);
        events.publish(saved, SubjectRequestEventPublisher.Action.COMPLETED);
        return SubjectRequestDto.View.of(saved, now);
    }

    public SubjectRequestDto.View reject(UUID id, SubjectRequestDto.RejectRequest req) {
        DataSubjectRequest r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.reject(req.reason(), req.handledByUserId(), now);
        DataSubjectRequest saved = repo.save(r);
        events.publish(saved, SubjectRequestEventPublisher.Action.REJECTED);
        return SubjectRequestDto.View.of(saved, now);
    }

    public SubjectRequestDto.View extendDeadline(UUID id,
                                                  SubjectRequestDto.ExtendDeadlineRequest req) {
        DataSubjectRequest r = loadForTenant(id);
        Instant now = Instant.now(clock);
        r.extendDeadline(req.newDeadline(), now);
        DataSubjectRequest saved = repo.save(r);
        events.publish(saved, SubjectRequestEventPublisher.Action.EXTENDED);
        return SubjectRequestDto.View.of(saved, now);
    }

    public SubjectRequestDto.View get(UUID id) {
        return SubjectRequestDto.View.of(loadForTenant(id), Instant.now(clock));
    }

    public List<SubjectRequestDto.View> list(SubjectRequestStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        Instant now = Instant.now(clock);
        List<DataSubjectRequest> all = status == null
                ? repo.findByTenantId(tenantId)
                : repo.findByTenantIdAndStatus(tenantId, status);
        return all.stream().map(r -> SubjectRequestDto.View.of(r, now)).toList();
    }

    /** Recherche par identifiant clair — hashé avant lookup. Renvoie [] si rien. */
    public List<SubjectRequestDto.View> findBySubjectIdentifier(String identifier) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (identifier == null || identifier.isBlank()) return List.of();
        Instant now = Instant.now(clock);
        String hash = hasher.hash(identifier.trim().toLowerCase());
        return repo.findByTenantIdAndSubjectIdentifierHash(tenantId, hash).stream()
                .map(r -> SubjectRequestDto.View.of(r, now)).toList();
    }

    public List<SubjectRequestDto.View> overdue(int limit) {
        Instant now = Instant.now(clock);
        return repo.findOverdue(now, Math.max(1, Math.min(limit, 500))).stream()
                .map(r -> SubjectRequestDto.View.of(r, now)).toList();
    }

    private DataSubjectRequest loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        DataSubjectRequest r = repo.findById(id)
                .orElseThrow(() -> new SubjectRequestNotFoundException(id));
        if (!r.getTenantId().equals(tenantId)) throw new SubjectRequestNotFoundException(id);
        return r;
    }
}
