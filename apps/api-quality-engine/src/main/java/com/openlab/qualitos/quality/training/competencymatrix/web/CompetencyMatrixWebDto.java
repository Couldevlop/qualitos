package com.openlab.qualitos.quality.training.competencymatrix.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Les corps de requête de la matrice, validés avant le service (OWASP A03/A04).
 *
 * <p>Aucun ne porte de client : il vient du jeton. Les personnes sont des
 * comptes de l'annuaire, avec le nom que l'annuaire leur donne au moment du
 * choix — jamais un texte libre sans compte.
 */
public final class CompetencyMatrixWebDto {

    private CompetencyMatrixWebDto() {}

    public record AddOperatorRequest(
            @NotNull UUID userId,
            @NotBlank @Size(max = 250) String userName,
            @NotBlank @Size(max = 60) String workshop) {
    }

    public record AddSkillRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 64) String category,
            @NotNull UUID trainerUserId,
            @NotBlank @Size(max = 250) String trainerName) {
    }

    public record SetLevelRequest(
            @NotNull UUID userId,
            @NotNull UUID skillId,
            @NotNull @Min(0) @Max(4) Integer level) {
    }

    public record WorkshopsRequest(
            @NotEmpty @Size(max = 30) List<@NotBlank @Size(max = 60) String> names) {
    }
}
