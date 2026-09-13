package com.openlab.qualitos.quality.nonconformity.eightd.web;

import jakarta.validation.constraints.Size;

/**
 * Ce que l'API reçoit. Aucun {@code tenantId}, aucun acteur : ils viennent du
 * jeton (§18.2 #2) et un champ de corps qui les porterait serait une porte ouverte.
 */
public final class EightDWebDto {

    private EightDWebDto() {
    }

    /**
     * Les trois disciplines sans source. Facultatives : un rapport incomplet s'émet
     * et se déclare partiel, ce qui est plus honnête qu'un document aux cases
     * remplies pour qu'elles ne soient pas vides.
     */
    public record SaveRequest(
            @Size(max = 4000) String team,
            @Size(max = 4000) String containment,
            @Size(max = 4000) String recognition) {
    }
}
