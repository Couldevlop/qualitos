package com.openlab.qualitos.quality.iot;

/**
 * Catégories d'équipements supportés en V1. Non exhaustif : étendre selon les
 * domaines couverts (CLAUDE.md §9.2). Le type sert au routage UI et à la
 * sélection de modèles IA pré-entraînés par domaine.
 */
public enum IotDeviceType {
    PLC,                // automate industriel
    SENSOR_TEMPERATURE,
    SENSOR_VIBRATION,
    SENSOR_PRESSURE,
    SENSOR_HUMIDITY,
    SENSOR_GENERIC,
    CAMERA,             // vision industrielle / médicale
    BIOMED,             // dispositif médical
    AGRO_STATION,
    BUILDING_BMS,
    GATEWAY,            // edge gateway lui-même
    UNKNOWN
}
