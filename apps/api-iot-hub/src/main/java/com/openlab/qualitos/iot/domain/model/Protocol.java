package com.openlab.qualitos.iot.domain.model;

/** Wire protocol — CLAUDE.md §9.4. Allow-list — anything outside requires a new enum. */
public enum Protocol {
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
