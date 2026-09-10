package com.openlab.qualitos.quality.ideas.application;

import com.openlab.qualitos.quality.ideas.domain.IdeaStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IdeaDto {

    private IdeaDto() {}

    /** Le tableau : une colonne par statut, dans l'ordre du cycle. */
    public record BoardView(List<ColumnView> columns) {}

    public record ColumnView(IdeaStatus status, List<IdeaView> ideas) {}

    /**
     * Une idée telle que l'écran la reçoit.
     *
     * <p>{@code votes} est CALCULÉ, jamais stocké sur l'idée : une colonne
     * dénormalisée diverge au premier vote concurrent, et rien à l'écran ne le
     * signalerait.
     */
    public record IdeaView(
            UUID id, String title, String description, IdeaStatus status,
            UUID authorId, String authorName, long votes, boolean votedByMe, boolean voteOpen,
            UUID circleId, String rejectionReason, String impactNote,
            Instant createdAt) {}

    /** Le cercle est facultatif : c'est tout l'objet de ce module. */
    public record SubmitCommand(String title, String description, UUID circleId) {}

    public record RejectCommand(String reason) {}

    public record ImpactCommand(String impactNote) {}
}
