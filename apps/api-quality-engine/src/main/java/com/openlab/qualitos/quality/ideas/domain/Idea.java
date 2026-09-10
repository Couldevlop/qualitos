package com.openlab.qualitos.quality.ideas.domain;

import java.time.Instant;
import java.util.UUID;

/** Une idée d'amélioration, déposée dans un cercle ou en dehors. */
public class Idea {

    private UUID id;
    private final UUID tenantId;
    private final UUID circleId;
    private String title;
    private String description;
    private IdeaStatus status;
    private final UUID proposedBy;
    /** Le nom de l'auteur AU DÉPÔT. Copié, pas résolu : voir la migration. */
    private final String proposedByName;
    private UUID validatedBy;
    private Instant validatedAt;
    private Instant implementedAt;
    private Instant measuredAt;
    private String impactNote;
    private String rejectionReason;
    private final Instant createdAt;
    private Instant updatedAt;

    private Idea(UUID id, UUID tenantId, UUID circleId, String title, String description,
                 IdeaStatus status, UUID proposedBy, String proposedByName,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.circleId = circleId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.proposedBy = proposedBy;
        this.proposedByName = proposedByName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Une idée qu'on dépose. Le cercle est facultatif : c'est tout l'objet du module. */
    public static Idea submitted(UUID tenantId, UUID circleId, String title,
                                 String description, UUID author, String authorName,
                                 Instant now) {
        if (tenantId == null) throw new IdeaStateException("An idea belongs to a tenant");
        if (author == null) throw new IdeaStateException("An idea has an author");
        String cleanTitle = requireText(title, "An idea needs a title");
        return new Idea(null, tenantId, circleId, cleanTitle, blankToNull(description),
                IdeaStatus.PROPOSED, author, blankToNull(authorName), now, now);
    }

    /** Rebâtit une idée depuis le stockage. Réservé aux adaptateurs. */
    public static Idea rehydrate(UUID id, UUID tenantId, UUID circleId, String title,
                                 String description, IdeaStatus status, UUID proposedBy,
                                 String proposedByName,
                                 UUID validatedBy, Instant validatedAt, Instant implementedAt,
                                 Instant measuredAt, String impactNote, String rejectionReason,
                                 Instant createdAt, Instant updatedAt) {
        Idea idea = new Idea(id, tenantId, circleId, title, description, status,
                proposedBy, proposedByName, createdAt, updatedAt);
        idea.validatedBy = validatedBy;
        idea.validatedAt = validatedAt;
        idea.implementedAt = implementedAt;
        idea.measuredAt = measuredAt;
        idea.impactNote = impactNote;
        idea.rejectionReason = rejectionReason;
        return idea;
    }

    public void review(Instant now) {
        require(status == IdeaStatus.PROPOSED, "Only a submitted idea can be moved under review");
        this.status = IdeaStatus.UNDER_REVIEW;
        this.updatedAt = now;
    }

    public void approve(UUID validator, Instant now) {
        require(status == IdeaStatus.UNDER_REVIEW, "Only an idea under review can be approved");
        require(!validator.equals(proposedBy), "The validator cannot be the proposer");
        this.status = IdeaStatus.APPROVED;
        this.validatedBy = validator;
        this.validatedAt = now;
        this.updatedAt = now;
    }

    public void reject(UUID validator, String reason, Instant now) {
        require(status == IdeaStatus.PROPOSED || status == IdeaStatus.UNDER_REVIEW,
                "Only a submitted or reviewed idea can be rejected");
        this.rejectionReason = requireText(reason, "Rejecting an idea requires a reason");
        this.status = IdeaStatus.REJECTED;
        this.validatedBy = validator;
        this.validatedAt = now;
        this.updatedAt = now;
    }

    public void implement(Instant now) {
        require(status == IdeaStatus.APPROVED, "Only an approved idea can be implemented");
        this.status = IdeaStatus.IMPLEMENTED;
        this.implementedAt = now;
        this.updatedAt = now;
    }

    public void measure(String note, Instant now) {
        require(status == IdeaStatus.IMPLEMENTED, "Only an implemented idea can record its impact");
        this.impactNote = requireText(note, "Recording an impact requires a note");
        this.status = IdeaStatus.MEASURED;
        this.measuredAt = now;
        this.updatedAt = now;
    }

    public void assignId(UUID assigned) { this.id = assigned; }

    public boolean voteOpen() { return status.voteOpen(); }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCircleId() { return circleId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IdeaStatus getStatus() { return status; }
    public UUID getProposedBy() { return proposedBy; }
    public String getProposedByName() { return proposedByName; }
    public UUID getValidatedBy() { return validatedBy; }
    public Instant getValidatedAt() { return validatedAt; }
    public Instant getImplementedAt() { return implementedAt; }
    public Instant getMeasuredAt() { return measuredAt; }
    public String getImpactNote() { return impactNote; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IdeaStateException(message);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new IdeaStateException(message);
        return value.trim();
    }

    /** `null` et blancs sont la même absence : on ne stocke pas une chaîne vide. */
    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
