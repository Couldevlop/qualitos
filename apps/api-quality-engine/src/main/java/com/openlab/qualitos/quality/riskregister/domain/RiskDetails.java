package com.openlab.qualitos.quality.riskregister.domain;

import java.time.LocalDate;

/**
 * Ce qu'on saisit pour un risque : le formulaire de création, puis celui de
 * modification qui y ajoute la cotation résiduelle visée, la prochaine revue
 * et le critère de vérification d'efficacité.
 *
 * <p>Les notes sont des {@code Integer} : une note absente est une erreur qui
 * se nomme (422 sur le bon champ), pas un zéro silencieux.
 */
public record RiskDetails(
        Identification identification,
        String cause,
        String effect,
        Integer grossSeverity,
        Integer grossProbability,
        Integer residualSeverity,
        Integer residualProbability,
        RiskDecision decision,
        RiskStatus status,
        LocalDate nextReviewOn,
        String effectivenessCriterion) {
}
