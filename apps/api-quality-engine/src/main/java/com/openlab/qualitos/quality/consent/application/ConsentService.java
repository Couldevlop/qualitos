package com.openlab.qualitos.quality.consent.application;

import com.openlab.qualitos.quality.consent.domain.Consent;
import com.openlab.qualitos.quality.consent.domain.ConsentNotFoundException;
import com.openlab.qualitos.quality.consent.domain.ConsentRepository;
import com.openlab.qualitos.quality.consent.domain.ConsentStateException;
import com.openlab.qualitos.quality.consent.domain.SubjectIdentifierHasher;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases consentement RGPD (Art. 7). PII hashée par
 * {@link SubjectIdentifierHasher} avant tout stockage (privacy by design).
 * Cross-tenant lookups yieldent 404 (no info leak — OWASP A01).
 *
 * Le retrait est terminal et irréversible : un nouveau consentement crée une
 * nouvelle entrée — garantit la traçabilité historique pour la charge de la
 * preuve (Art. 7§1).
 */
public class ConsentService {

    private final ConsentRepository repo;
    private final SubjectIdentifierHasher hasher;
    private final TenantProvider tenantProvider;
    private final ConsentEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ConsentService(ConsentRepository repo, SubjectIdentifierHasher hasher,
                          TenantProvider tenantProvider, Clock clock) {
        this(repo, hasher, tenantProvider, new ConsentEventPublisher.NoOp(), clock);
    }

    public ConsentService(ConsentRepository repo, SubjectIdentifierHasher hasher,
                          TenantProvider tenantProvider,
                          ConsentEventPublisher events, Clock clock) {
        this.repo = repo;
        this.hasher = hasher;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public ConsentDto.View grant(ConsentDto.GrantRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.subjectIdentifier() == null || req.subjectIdentifier().isBlank()) {
            throw new ConsentStateException("subjectIdentifier required");
        }
        Instant now = Instant.now(clock);
        String hash = hasher.hash(normalizeIdentifier(req.subjectIdentifier()));
        Consent c = Consent.grant(tenantId, hash, req.subjectIdentifierLabel(),
                req.purposeCode(), req.purposeVersion(),
                req.source(), req.evidenceUrl(),
                req.ipAddress(), req.userAgent(),
                req.grantedByUserId(), now, req.expiresAt());
        Consent saved = repo.save(c);
        events.publish(saved, ConsentEventPublisher.Action.GRANTED);
        return ConsentDto.View.of(saved, now);
    }

    public ConsentDto.View withdraw(UUID id, ConsentDto.WithdrawRequest req) {
        Consent c = loadForTenant(id);
        Instant now = Instant.now(clock);
        c.withdraw(req.actorUserId(), req.reason(), now);
        Consent saved = repo.save(c);
        events.publish(saved, ConsentEventPublisher.Action.WITHDRAWN);
        return ConsentDto.View.of(saved, now);
    }

    public ConsentDto.View get(UUID id) {
        Instant now = Instant.now(clock);
        return ConsentDto.View.of(loadForTenant(id), now);
    }

    /** Recherche par identifiant en clair — hashé avant lookup. */
    public List<ConsentDto.View> findBySubject(String subjectIdentifier) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (subjectIdentifier == null || subjectIdentifier.isBlank()) return List.of();
        Instant now = Instant.now(clock);
        String hash = hasher.hash(normalizeIdentifier(subjectIdentifier));
        return repo.findByTenantAndSubjectHash(tenantId, hash).stream()
                .map(c -> ConsentDto.View.of(c, now)).toList();
    }

    public Optional<ConsentDto.View> findActiveByPurpose(String subjectIdentifier, String purposeCode) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (subjectIdentifier == null || subjectIdentifier.isBlank()
                || purposeCode == null || purposeCode.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        String hash = hasher.hash(normalizeIdentifier(subjectIdentifier));
        return repo.findLatestActiveByPurpose(tenantId, hash, purposeCode, now)
                .map(c -> ConsentDto.View.of(c, now));
    }

    public List<ConsentDto.View> listByPurpose(String purposeCode) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (purposeCode == null || purposeCode.isBlank()) return List.of();
        Instant now = Instant.now(clock);
        return repo.findByTenantAndPurpose(tenantId, purposeCode).stream()
                .map(c -> ConsentDto.View.of(c, now)).toList();
    }

    /** Scan d'expiration — marque comme EXPIRED les consentements dont la date
     *  est dépassée. À appeler par un scheduler. */
    public int expireDue(int limit) {
        Instant now = Instant.now(clock);
        int capped = Math.max(1, Math.min(limit, 500));
        List<Consent> due = repo.findExpirable(now, capped);
        int n = 0;
        for (Consent c : due) {
            c.expireIfDue(now);
            if (c.getStatus() == com.openlab.qualitos.quality.consent.domain.ConsentStatus.EXPIRED) {
                repo.save(c);
                events.publish(c, ConsentEventPublisher.Action.EXPIRED);
                n++;
            }
        }
        return n;
    }

    private Consent loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        Consent c = repo.findById(id)
                .orElseThrow(() -> new ConsentNotFoundException(id));
        if (!c.getTenantId().equals(tenantId)) throw new ConsentNotFoundException(id);
        return c;
    }

    private static String normalizeIdentifier(String raw) {
        return raw.trim().toLowerCase();
    }
}
