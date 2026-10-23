package com.openlab.qualitos.quality.spc.gagerr.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Les corps de requête des études Gage R&amp;R.
 *
 * <p>Ces contraintes sont une première barrière, lisible dans l'OpenAPI (plan
 * dans la table : 2 ou 3 opérateurs, 2 à 10 pièces, 2 ou 3 essais). Les règles
 * qui comptent — grille aux dimensions du plan, toutes les cellules
 * renseignées, opérateurs distincts, USL au-dessus de LSL, multiplicateur 5,15
 * ou 6 — sont tenues par le domaine ({@code GageRrStudy}).
 *
 * <p>Aucun champ tenant ni auteur : ils viennent du jeton (§18.2). Ni %GRR, ni
 * ndc, ni verdict : le serveur les calcule. Les personnes sont des comptes de
 * l'annuaire, avec leur nom recopié.
 */
public final class GageRrWebDto {

    private GageRrWebDto() {}

    public record OperatorRequest(@NotNull UUID accountId, @NotBlank @Size(max = 150) String name) {}

    public record StudyRequest(
            @NotBlank @Size(max = 120) String partReference,
            @Size(max = 150) String characteristic,
            @Size(max = 150) String instrument,
            @NotNull Double lsl,
            @NotNull Double usl,
            @NotNull @Min(2) @Max(3) Integer operatorCount,
            @NotNull @Min(2) @Max(10) Integer partCount,
            @NotNull @Min(2) @Max(3) Integer trialCount,
            @NotNull Double sigmaMultiplier,
            @NotNull LocalDate studyDate,
            UUID responsibleUserId,
            @Size(max = 150) String responsibleName,
            @NotNull @Size(min = 2, max = 3) List<@NotNull @Valid OperatorRequest> operators,
            @NotNull @Size(min = 2, max = 3)
            List<@NotNull @Size(min = 2, max = 10) List<@NotNull @Size(min = 2, max = 3) List<Double>>> measurements) {}
}
