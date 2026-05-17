package com.openlab.qualitos.iot.domain.model;

/** Device family — extensible enum aligned with CLAUDE.md §9.2 universality. */
public enum DeviceType {
  PLC,
  SENSOR_TEMPERATURE,
  SENSOR_VIBRATION,
  SENSOR_PRESSURE,
  SENSOR_HUMIDITY,
  SENSOR_GENERIC,
  CAMERA,
  BIOMED,
  AGRO_STATION,
  BUILDING_BMS,
  GATEWAY,
  UNKNOWN
}
