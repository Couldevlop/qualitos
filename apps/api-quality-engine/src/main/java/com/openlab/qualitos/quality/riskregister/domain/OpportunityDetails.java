package com.openlab.qualitos.quality.riskregister.domain;

import java.time.LocalDate;

/**
 * Ce qu'on saisit pour une opportunité : gain attendu × faisabilité à la place
 * de la cotation, une échéance visée à la place de la cotation résiduelle, et
 * le critère qui dira si le bénéfice est au rendez-vous.
 */
public record OpportunityDetails(
        Identification identification,
        LocalDate targetDate,
        String context,
        String benefit,
        Integer gain,
        Integer feasibility,
        OpportunityDecision decision,
        OpportunityStatus status,
        String benefitCriterion) {
}
