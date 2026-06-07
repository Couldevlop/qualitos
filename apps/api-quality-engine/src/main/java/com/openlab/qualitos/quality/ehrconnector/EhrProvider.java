package com.openlab.qualitos.quality.ehrconnector;

/**
 * Version du serveur FHIR ciblé (§13.3 « HL7 FHIR R5 »).
 *
 * <p>Le parsing du Bundle FHIR est tolérant et commun aux deux versions
 * (structure {@code entry[].resource}), donc le provider sert surtout à
 * documenter/router l'intégration sans dupliquer de client.</p>
 */
public enum EhrProvider {
    FHIR_R4,
    FHIR_R5
}
