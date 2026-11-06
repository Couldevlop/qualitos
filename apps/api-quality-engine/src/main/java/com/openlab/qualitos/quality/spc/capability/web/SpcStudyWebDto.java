package com.openlab.qualitos.quality.spc.capability.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Les corps de requête.
 *
 * <p>Ces contraintes sont une première barrière, lisible dans l'OpenAPI. Les
 * règles qui comptent — une ligne par échantillon, USL au-dessus de LSL, au
 * moins un sous-groupe complet — sont tenues par le domaine
 * ({@code CapabilityStudy}).
 *
 * <p>Aucun champ tenant ni auteur : ils viennent du jeton (§18.2). Le meneur
 * d'une étude est une personne de l'annuaire : son compte et son nom recopié.
 * Ni Cp ni Cpk : le serveur les calcule.
 */
public final class SpcStudyWebDto {

    private SpcStudyWebDto() {}

    public record StudyRequest(
            @NotBlank @Size(max = 120) String partReference,
            @Size(max = 150) String characteristic,
            @Size(max = 150) String specification,
            @Size(max = 80) String machine,
            UUID leaderUserId,
            @Size(max = 150) String leaderName,
            @NotNull LocalDate studyDate,
            boolean actionPlan,
            @NotNull Double lsl,
            @NotNull Double usl,
            @NotNull @Min(2) @Max(25) Integer subgroupSize,
            @NotNull @Size(min = 2, max = 25) List<@NotNull @Size(min = 2, max = 100) List<Double>> measurements) {

        /**
         * Toutes les lignes ont la meme largeur.
         *
         * <p>La borne 20..20 d'avant l'imposait par accident ; en la relachant,
         * il faut la dire. Une grille en escalier ferait porter a un sous-groupe
         * moins d'echantillons que sa taille annoncee, et le calcul l'ecarterait
         * sans rien dire a celui qui a saisi.
         */
        @com.fasterxml.jackson.annotation.JsonIgnore
        @jakarta.validation.constraints.AssertTrue(
                message = "chaque ligne compte le meme nombre de sous-groupes")
        public boolean isGrilleRectangulaire() {
            if (measurements == null || measurements.isEmpty() || measurements.get(0) == null) {
                return true;
            }
            int colonnes = measurements.get(0).size();
            return measurements.stream().allMatch(l -> l != null && l.size() == colonnes);
        }
    }

    public record ActionPlanRequest(@NotNull Boolean actionPlan) {}

    public record SettingsRequest(
            @NotNull @DecimalMin("0.01") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2)
            BigDecimal capableThreshold,
            @NotNull @DecimalMin("0.01") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2)
            BigDecimal watchThreshold) {}
}
