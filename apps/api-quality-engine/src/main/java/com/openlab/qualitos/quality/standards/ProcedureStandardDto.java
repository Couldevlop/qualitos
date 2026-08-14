package com.openlab.qualitos.quality.standards;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Requêtes d'édition de l'arborescence d'un référentiel de procédure (§8).
 *
 * <p>Les tailles maximales reprennent EXACTEMENT celles des colonnes : une valeur
 * plus permissive ferait remonter au client une erreur de base de données là où il
 * attend un 400 qui nomme le champ fautif.
 */
public final class ProcedureStandardDto {

    private ProcedureStandardDto() {}

    public record SectionRequest(@NotBlank @Size(max = 20) String code,
                                 @NotBlank @Size(max = 500) String title,
                                 String description) {}

    public record ClauseRequest(@NotBlank @Size(max = 30) String code,
                                @NotBlank @Size(max = 500) String title,
                                String description) {}

    public record RequirementRequest(@NotBlank @Size(max = 30) String code,
                                     @NotBlank String text,
                                     @NotNull ObligationLevel obligation,
                                     String evidenceTypes,
                                     String measurableCriteria,
                                     RiskLevel riskIfMissing) {}
}
