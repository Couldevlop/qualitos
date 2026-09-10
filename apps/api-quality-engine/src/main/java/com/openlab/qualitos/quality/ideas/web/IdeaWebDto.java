package com.openlab.qualitos.quality.ideas.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class IdeaWebDto {

    private IdeaWebDto() {}

    /**
     * Dépôt d'une idée.
     *
     * <p>Aucun champ d'auteur : il vient du jeton. En accepter un ici rouvrirait
     * exactement le défaut que ce lot corrige (§18.2).
     */
    public record SubmitRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 4000) String description,
            UUID circleId) {}

    public record RejectRequest(@NotBlank @Size(max = 2000) String reason) {}

    public record ImpactRequest(@NotBlank @Size(max = 2000) String impactNote) {}
}
