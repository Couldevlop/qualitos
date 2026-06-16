package com.openlab.qualitos.quality.training;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO de la gamification formation (CLAUDE.md §19.3).
 *
 * <p>Le contrat d'entrée {@link CompleteLearningRequest} ne porte PAS d'identité :
 * le tenant et l'utilisateur sont résolus depuis le JWT côté service (invariant
 * de sécurité 18.2 §2). On ne décrit que ce qui a été appris et le score.</p>
 */
public final class GamificationDto {

    private GamificationDto() {}

    /** Marque un parcours / quiz comme terminé et déclenche le recalcul. */
    public record CompleteLearningRequest(
            @NotBlank @Size(max = 200)
            String itemCode,
            @Min(0) @Max(100) @NotNull
            Integer score
    ) {}

    /** Vue complète de la progression d'un apprenant. */
    public record LearnerProgressResponse(
            UUID userId,
            UUID tenantId,
            int points,
            int completedCount,
            Integer bestScore,
            BeltLevel beltLevel,
            int pointsToNextBelt,
            List<Badge> badges,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
