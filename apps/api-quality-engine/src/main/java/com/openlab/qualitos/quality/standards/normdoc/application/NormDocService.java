package com.openlab.qualitos.quality.standards.normdoc.application;

import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.domain.GeneratedNormDoc;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerationCommand;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocGenerator;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocRepository;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage — génération assistée IA de documents normatifs + workflow de
 * validation humaine (Standards Hub §8.8). Clean architecture : dépend
 * uniquement des ports du domaine. Le tenant et l'acteur viennent du JWT
 * (jamais du body, OWASP A01). Le 404 cross-tenant masque l'existence.
 */
public class NormDocService {

    private final NormDocRepository repo;
    private final NormDocGenerator generator;
    private final NormDocStandardLookup standards;
    private final NormDocTenantProvider tenantProvider;
    private final NormDocActorProvider actorProvider;
    private final NormDocEventPublisher events;
    private final Clock clock;

    public NormDocService(NormDocRepository repo, NormDocGenerator generator,
                          NormDocStandardLookup standards,
                          NormDocTenantProvider tenantProvider,
                          NormDocActorProvider actorProvider,
                          NormDocEventPublisher events, Clock clock) {
        this.repo = repo;
        this.generator = generator;
        this.standards = standards;
        this.tenantProvider = tenantProvider;
        this.actorProvider = actorProvider;
        this.events = events;
        this.clock = clock;
    }

    /** Génère un brouillon IA complet (multi-sections) et le persiste. */
    public NormDocDto.View generate(NormDocDto.GenerateRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        UUID actor = actorProvider.requireActorId();
        if (req.standardId() == null) {
            throw new IllegalArgumentException("standardId required");
        }
        if (req.kind() == null) {
            throw new IllegalArgumentException("kind required");
        }
        NormDocStandardLookup.StandardRef ref = standards.findById(req.standardId())
                .orElseThrow(() -> new StandardNotFoundException(req.standardId()));

        NormDocDto.TenantProfile profile = req.tenantProfile();
        if (profile == null) {
            throw new IllegalArgumentException("tenantProfile required");
        }

        NormDocGenerationCommand command = new NormDocGenerationCommand(
                req.kind(), ref.code(), ref.fullName(),
                profile.organizationName(), profile.industry(), profile.size(),
                profile.language(), profile.knownProcesses(),
                toSectionRequests(req.sections()));

        GeneratedNormDoc generated = generator.generate(command);

        Instant now = Instant.now(clock);
        NormativeDocument doc = NormativeDocument.draftFromAi(
                tenantId, ref.id(), ref.code(), req.kind(),
                generated.title(), generated.sections(), generated.provider(),
                actor, now);
        NormativeDocument saved = repo.save(doc);
        events.publish(saved, NormDocEventPublisher.Action.GENERATED);
        return NormDocDto.View.of(saved);
    }

    public NormDocDto.View edit(UUID id, NormDocDto.EditRequest req) {
        NormativeDocument doc = loadForTenant(id);
        if (req.sections() == null || req.sections().isEmpty()) {
            throw new IllegalArgumentException("at least one section required");
        }
        List<com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection> sections =
                req.sections().stream().map(NormDocDto.SectionView::toDomain).toList();
        doc.editDraft(req.title(), sections, Instant.now(clock));
        NormativeDocument saved = repo.save(doc);
        events.publish(saved, NormDocEventPublisher.Action.EDITED);
        return NormDocDto.View.of(saved);
    }

    public NormDocDto.View submitForReview(UUID id) {
        NormativeDocument doc = loadForTenant(id);
        doc.submitForReview(actorProvider.requireActorId(), Instant.now(clock));
        NormativeDocument saved = repo.save(doc);
        events.publish(saved, NormDocEventPublisher.Action.SUBMITTED);
        return NormDocDto.View.of(saved);
    }

    /** Approuve + signe. L'approbateur est le sujet du JWT (jamais du body). */
    public NormDocDto.View approve(UUID id, NormDocDto.ApproveRequest req) {
        NormativeDocument doc = loadForTenant(id);
        UUID approver = actorProvider.requireActorId();
        doc.approve(approver, req.signature(), req.notes(), Instant.now(clock));
        NormativeDocument saved = repo.save(doc);
        events.publish(saved, NormDocEventPublisher.Action.APPROVED);
        return NormDocDto.View.of(saved);
    }

    public NormDocDto.View reject(UUID id, NormDocDto.RejectRequest req) {
        NormativeDocument doc = loadForTenant(id);
        doc.reject(req.reason(), Instant.now(clock));
        NormativeDocument saved = repo.save(doc);
        events.publish(saved, NormDocEventPublisher.Action.REJECTED);
        return NormDocDto.View.of(saved);
    }

    public NormDocDto.View get(UUID id) {
        return NormDocDto.View.of(loadForTenant(id));
    }

    public List<NormDocDto.View> list(NormDocStatus status) {
        UUID tenantId = tenantProvider.requireTenantId();
        List<NormativeDocument> all = status == null
                ? repo.findByTenant(tenantId)
                : repo.findByTenantAndStatus(tenantId, status);
        return all.stream().map(NormDocDto.View::of).toList();
    }

    public void delete(UUID id) {
        NormativeDocument doc = loadForTenant(id);
        if (doc.isApproved()) {
            throw new com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStateException(
                    "Approved documents cannot be deleted (preserved for audit)");
        }
        repo.delete(doc.getId());
        events.publish(doc, NormDocEventPublisher.Action.DELETED);
    }

    private NormativeDocument loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        NormativeDocument doc = repo.findById(id).orElseThrow(() -> new NormDocNotFoundException(id));
        if (!doc.getTenantId().equals(tenantId)) {
            throw new NormDocNotFoundException(id);
        }
        return doc;
    }

    private static List<NormDocGenerationCommand.SectionRequest> toSectionRequests(
            List<NormDocDto.SectionSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new IllegalArgumentException("at least one section required");
        }
        return specs.stream()
                .map(s -> new NormDocGenerationCommand.SectionRequest(
                        s.key(), s.title(), s.clauses(), s.guidance()))
                .toList();
    }
}
