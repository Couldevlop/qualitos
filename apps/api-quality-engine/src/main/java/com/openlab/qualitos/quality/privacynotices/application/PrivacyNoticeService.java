package com.openlab.qualitos.quality.privacynotices.application;

import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeNotFoundException;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeRepository;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStateException;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases — mentions d'information RGPD (Art. 13/14).
 *
 * Garantie clé : à la publication, toute notice PUBLISHED existante pour la
 * même (tenant, reference, language) est automatiquement archivée — invariant
 * "au plus une PUBLISHED par (référence, langue)".
 *
 * Cross-tenant lookups yieldent 404 (OWASP A01).
 * Audit trail via {@link PrivacyNoticeEventPublisher} (OWASP A09).
 */
public class PrivacyNoticeService {

    private final PrivacyNoticeRepository repo;
    private final TenantProvider tenantProvider;
    private final PrivacyNoticeEventPublisher events;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public PrivacyNoticeService(PrivacyNoticeRepository repo,
                                TenantProvider tenantProvider, Clock clock) {
        this(repo, tenantProvider, new PrivacyNoticeEventPublisher.NoOp(), clock);
    }

    public PrivacyNoticeService(PrivacyNoticeRepository repo,
                                TenantProvider tenantProvider,
                                PrivacyNoticeEventPublisher events, Clock clock) {
        this.repo = repo;
        this.tenantProvider = tenantProvider;
        this.events = events;
        this.clock = clock;
    }

    public PrivacyNoticeDto.View create(PrivacyNoticeDto.CreateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (req.createdByUserId() == null) {
            throw new PrivacyNoticeStateException("createdByUserId required");
        }
        if (repo.existsByTenantAndReferenceAndVersionAndLanguage(
                tenantId, req.reference(), req.version(), req.language())) {
            throw new PrivacyNoticeStateException(
                    "Version already exists: " + req.reference()
                            + "@" + req.version() + " [" + req.language() + "]");
        }
        Instant now = Instant.now(clock);
        PrivacyNotice n = PrivacyNotice.draft(tenantId,
                req.reference(), req.version(), req.language(),
                req.title(), req.summary(), req.contentMarkdown(),
                req.linkedProcessingActivityIds(), req.publishUrl(),
                req.contactName(), req.contactEmail(), req.createdByUserId(), now);
        PrivacyNotice saved = repo.save(n);
        events.publish(saved, PrivacyNoticeEventPublisher.Action.CREATED);
        return PrivacyNoticeDto.View.of(saved);
    }

    public PrivacyNoticeDto.View edit(UUID id, PrivacyNoticeDto.EditRequest req) {
        PrivacyNotice n = loadForTenant(id);
        Instant now = Instant.now(clock);
        n.editDraft(req.title(), req.summary(), req.contentMarkdown(),
                req.linkedProcessingActivityIds(), req.publishUrl(),
                req.contactName(), req.contactEmail(), now);
        PrivacyNotice saved = repo.save(n);
        events.publish(saved, PrivacyNoticeEventPublisher.Action.EDITED);
        return PrivacyNoticeDto.View.of(saved);
    }

    /** Publication — archive automatiquement l'éventuelle PUBLISHED précédente
     *  pour la même (reference, language) (invariant 0 ou 1 par couple). */
    public PrivacyNoticeDto.View publish(UUID id, PrivacyNoticeDto.PublishRequest req) {
        PrivacyNotice n = loadForTenant(id);
        if (!n.isDraft()) {
            throw new PrivacyNoticeStateException("Only DRAFT notices can be published");
        }
        Instant now = Instant.now(clock);
        Optional<PrivacyNotice> existing = repo.findPublishedByReferenceAndLanguage(
                n.getTenantId(), n.getReference(), n.getLanguage());
        if (existing.isPresent() && !existing.get().getId().equals(n.getId())) {
            PrivacyNotice old = existing.get();
            old.archive(now);
            PrivacyNotice archived = repo.save(old);
            events.publish(archived, PrivacyNoticeEventPublisher.Action.ARCHIVED);
        }
        n.publish(req.publishedByUserId(), now);
        PrivacyNotice saved = repo.save(n);
        events.publish(saved, PrivacyNoticeEventPublisher.Action.PUBLISHED);
        return PrivacyNoticeDto.View.of(saved);
    }

    public PrivacyNoticeDto.View archive(UUID id) {
        PrivacyNotice n = loadForTenant(id);
        Instant now = Instant.now(clock);
        n.archive(now);
        PrivacyNotice saved = repo.save(n);
        events.publish(saved, PrivacyNoticeEventPublisher.Action.ARCHIVED);
        return PrivacyNoticeDto.View.of(saved);
    }

    public void delete(UUID id) {
        PrivacyNotice n = loadForTenant(id);
        if (!n.isDraft()) {
            throw new PrivacyNoticeStateException(
                    "Only DRAFT notices can be deleted (published/archived preserved for audit)");
        }
        repo.delete(id);
        events.publish(n, PrivacyNoticeEventPublisher.Action.DELETED);
    }

    public PrivacyNoticeDto.View get(UUID id) {
        return PrivacyNoticeDto.View.of(loadForTenant(id));
    }

    public List<PrivacyNoticeDto.View> list(PrivacyNoticeStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<PrivacyNotice> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(PrivacyNoticeDto.View::of).toList();
    }

    public Optional<PrivacyNoticeDto.View> findPublished(String reference, String language) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()
                || language == null || language.isBlank()) {
            return Optional.empty();
        }
        return repo.findPublishedByReferenceAndLanguage(tenantId, reference, language)
                .map(PrivacyNoticeDto.View::of);
    }

    public List<PrivacyNoticeDto.View> versions(String reference) {
        UUID tenantId = tenantProvider.requireTenantId();
        if (reference == null || reference.isBlank()) return List.of();
        return repo.findVersionsByReference(tenantId, reference).stream()
                .map(PrivacyNoticeDto.View::of).toList();
    }

    private PrivacyNotice loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        PrivacyNotice n = repo.findById(id)
                .orElseThrow(() -> new PrivacyNoticeNotFoundException(id));
        if (!n.getTenantId().equals(tenantId)) {
            throw new PrivacyNoticeNotFoundException(id);
        }
        return n;
    }
}
