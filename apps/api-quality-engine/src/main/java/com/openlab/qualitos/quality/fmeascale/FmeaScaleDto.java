package com.openlab.qualitos.quality.fmeascale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Commandes et vues du référentiel de cotation. Aucune logique. */
public final class FmeaScaleDto {

    private FmeaScaleDto() {}

    /**
     * Une ligne proposée par le tenant.
     *
     * <p>Le score n'est pas choisi librement à l'écriture : les dix lignes
     * envoyées doivent couvrir 1 à 10, sans trou ni doublon. C'est vérifié au
     * service, parce que la règle porte sur l'ENSEMBLE et non sur chaque ligne.
     */
    public record RowRequest(
            @Min(1) @Max(10) int score,
            @NotBlank @Size(max = 120) String label,
            @Size(max = 500) String description,
            @Size(max = 120) String timePeriod,
            @Size(max = 120) String failureRate) {}

    /** Le barème complet d'une échelle, remplacé d'un bloc. */
    public record ScaleRequest(
            @NotEmpty @Valid List<RowRequest> rows) {}

    public record RowView(
            int score, String label, String description,
            String timePeriod, String failureRate) {

        public static RowView of(FmeaScaleRow row) {
            return new RowView(row.score(), row.label(), row.description(),
                    row.timePeriod(), row.failureRate());
        }
    }

    /**
     * Une échelle telle qu'elle s'affiche.
     *
     * <p>{@code custom} n'est pas cosmétique : il dit si l'organisation cote sur
     * SON barème ou sur celui de référence. Deux RPN issus de barèmes différents
     * ne se comparent pas, et l'écran doit pouvoir le signaler.
     */
    public record ScaleView(
            FmeaScaleKind kind, boolean custom, List<RowView> rows,
            UUID updatedBy, Instant updatedAt) {}

    /** Les trois échelles, telles qu'un écran de cotation en a besoin. */
    public record ReferenceView(List<ScaleView> scales) {}
}
