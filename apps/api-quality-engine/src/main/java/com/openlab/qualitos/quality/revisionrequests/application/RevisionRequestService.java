package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestNotFoundException;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Use cases — propositions de révision.
 *
 * <p>Enregistrer une proposition supersède celle qui attendait sur la même cible :
 * une seule demande en attente par cible, sans quoi le badge « à réviser »
 * afficherait vingt propositions identiques après vingt non-conformités. La
 * précédente n'est pas supprimée mais marquée remplacée — l'historique des
 * propositions est lui-même une preuve.
 */
public class RevisionRequestService {

    private final RevisionRequestRepository repo;
    private final RevisionApplier applier;
    private final RevisionAuditPort audit;
    private final TenantProvider tenants;
    private final ActorProvider actors;
    private final Clock clock;

    public RevisionRequestService(RevisionRequestRepository repo, RevisionApplier applier,
                                  RevisionAuditPort audit, TenantProvider tenants,
                                  ActorProvider actors, Clock clock) {
        this.repo = repo;
        this.applier = applier;
        this.audit = audit;
        this.tenants = tenants;
        this.actors = actors;
        this.clock = clock;
    }

    /** Appelée par les écouteurs. Le tenant vient des demandes elles-mêmes. */
    public void record(List<RevisionRequest> proposals) {
        Instant now = Instant.now(clock);
        for (RevisionRequest proposal : proposals) {
            if (proposal.getTargetId() != null) {
                repo.findPendingForTarget(proposal.getTenantId(), proposal.getTargetType(),
                                proposal.getTargetId())
                        .ifPresent(previous -> {
                            previous.supersede(now);
                            repo.save(previous);
                        });
            }
            repo.save(proposal);
        }
    }

    public List<RevisionRequestDto.View> pendingForProduct(UUID productId) {
        return repo.findPendingByProduct(tenants.requireTenantId(), productId).stream()
                .map(RevisionRequestDto.View::of)
                .toList();
    }

    public int countPending(UUID productId) {
        return repo.countPendingByProduct(tenants.requireTenantId(), productId);
    }

    public List<RevisionRequestDto.View> forTrigger(UUID triggerRefId) {
        return repo.findByTrigger(tenants.requireTenantId(), triggerRefId).stream()
                .map(RevisionRequestDto.View::of)
                .toList();
    }

    public RevisionRequestDto.View accept(UUID id) {
        RevisionRequest request = requireOwned(id);
        UUID actor = actors.currentUserId();
        request.accept(actor, Instant.now(clock));
        applier.apply(request);
        RevisionRequest saved = repo.save(request);
        audit.record(saved.getTenantId(), actor, "revision.request.accepted", saved.getId(),
                "Proposition de révision acceptée", details(saved));
        return RevisionRequestDto.View.of(saved);
    }

    public RevisionRequestDto.View reject(UUID id, String note) {
        RevisionRequest request = requireOwned(id);
        UUID actor = actors.currentUserId();
        request.reject(actor, note, Instant.now(clock));
        RevisionRequest saved = repo.save(request);
        audit.record(saved.getTenantId(), actor, "revision.request.rejected", saved.getId(),
                "Proposition de révision refusée", details(saved));
        return RevisionRequestDto.View.of(saved);
    }

    private RevisionRequest requireOwned(UUID id) {
        UUID tenantId = tenants.requireTenantId();
        RevisionRequest request = repo.findById(id)
                .orElseThrow(() -> new RevisionRequestNotFoundException(id));
        if (!request.getTenantId().equals(tenantId)) {
            // 404 et non 403 : un 403 confirmerait l'existence de la demande voisine.
            throw new RevisionRequestNotFoundException(id);
        }
        return request;
    }

    /** JSON plat pour la ligne de journal : quatre champs, aucun sérialiseur à convoquer. */
    private static String details(RevisionRequest request) {
        return "{\"targetType\":\"" + request.getTargetType()
                + "\",\"targetId\":" + quoted(request.getTargetId())
                + ",\"productId\":" + quoted(request.getProductId())
                + ",\"status\":\"" + request.getStatus() + "\"}";
    }

    private static String quoted(UUID value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
