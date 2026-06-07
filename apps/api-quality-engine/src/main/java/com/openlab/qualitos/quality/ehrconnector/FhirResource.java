package com.openlab.qualitos.quality.ehrconnector;

import java.time.Instant;

/**
 * Vue minimale, NON-PII, d'une ressource FHIR pertinente (Observation /
 * DiagnosticReport) extraite d'un Bundle.
 *
 * <p>PRIVACY (§11.3) : on n'extrait QUE des champs techniques / de classification :
 * <ul>
 *   <li>{@link #resourceType} / {@link #id} : identité technique de la ressource ;</li>
 *   <li>{@link #code} / {@link #codeDisplay} : libellé du type d'observation
 *       (terminologie, pas une donnée patient) ;</li>
 *   <li>{@link #status} : statut FHIR (final, amended…) ;</li>
 *   <li>{@link #interpretation} : code d'interprétation (ex. {@code A}=abnormal,
 *       {@code HH}=critical high) — c'est ce qui pilote la sévérité de la NC ;</li>
 *   <li>{@link #effective} : horodatage de la mesure.</li>
 * </ul>
 * On ne capture JAMAIS {@code subject} nominatif, ni valeur clinique, ni note libre.</p>
 */
public record FhirResource(
        String resourceType,
        String id,
        String code,
        String codeDisplay,
        String status,
        String interpretation,
        Instant effective
) {
    /** Référence technique stable et non-nominative (ex. "Observation/abc-123"). */
    public String reference() {
        return resourceType + "/" + id;
    }
}
