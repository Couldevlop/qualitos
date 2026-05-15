package com.openlab.qualitos.quality.iot;

/**
 * Protocoles d'ingestion supportés (CLAUDE.md §9.4).
 *
 * MANUAL : ingestion REST POST (utilisée par les tests, les imports CSV et le mode
 * dégradé quand un capteur n'a pas encore son protocole natif câblé).
 */
public enum IotProtocol {
    OPC_UA,
    MQTT,
    SPARKPLUG_B,
    MODBUS_TCP,
    HL7_FHIR,
    HL7_V2,
    DICOM,
    LORAWAN,
    MANUAL
}
