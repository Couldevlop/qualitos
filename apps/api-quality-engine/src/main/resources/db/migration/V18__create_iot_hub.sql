-- V18: IoT Hub core (CLAUDE.md §9)
--
-- Registre des équipements + table d'événements de télémétrie. Pour V1 on reste
-- sur PostgreSQL classique ; le passage TimescaleDB (hypertable sur recorded_at)
-- est planifié quand le volume dépassera ~10M lignes par tenant.

CREATE TABLE iot_devices (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    code            VARCHAR(120) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    device_type     VARCHAR(32)  NOT NULL,
    protocol        VARCHAR(32)  NOT NULL,
    status          VARCHAR(32)  NOT NULL,
    location        VARCHAR(500),
    description     VARCHAR(1000),
    metadata_json   TEXT,
    last_seen_at    TIMESTAMP WITH TIME ZONE,
    telemetry_count BIGINT NOT NULL DEFAULT 0,
    created_by      UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_iot_device_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_iot_device_protocol CHECK (protocol IN (
        'OPC_UA','MQTT','SPARKPLUG_B','MODBUS_TCP','HL7_FHIR','HL7_V2',
        'DICOM','LORAWAN','MANUAL')),
    CONSTRAINT chk_iot_device_status CHECK (status IN (
        'PROVISIONED','ACTIVE','SUSPENDED','DECOMMISSIONED')),
    CONSTRAINT chk_iot_device_type CHECK (device_type IN (
        'PLC','SENSOR_TEMPERATURE','SENSOR_VIBRATION','SENSOR_PRESSURE',
        'SENSOR_HUMIDITY','SENSOR_GENERIC','CAMERA','BIOMED',
        'AGRO_STATION','BUILDING_BMS','GATEWAY','UNKNOWN'))
);

CREATE INDEX idx_iot_device_tenant ON iot_devices(tenant_id);
CREATE INDEX idx_iot_device_status ON iot_devices(tenant_id, status);
CREATE INDEX idx_iot_device_type   ON iot_devices(tenant_id, device_type);

CREATE TABLE iot_telemetry_events (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    device_id       UUID NOT NULL,
    metric          VARCHAR(100) NOT NULL,
    value_numeric   NUMERIC(24, 6),
    value_text      VARCHAR(500),
    unit            VARCHAR(32),
    source          VARCHAR(32) NOT NULL,
    recorded_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    ingested_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_iot_telemetry_device FOREIGN KEY (device_id)
        REFERENCES iot_devices(id) ON DELETE CASCADE,
    CONSTRAINT chk_iot_telemetry_source CHECK (source IN (
        'OPC_UA','MQTT','SPARKPLUG_B','MODBUS_TCP','HL7_FHIR','HL7_V2',
        'DICOM','LORAWAN','MANUAL')),
    CONSTRAINT chk_iot_telemetry_value CHECK (
        value_numeric IS NOT NULL OR value_text IS NOT NULL)
);

CREATE INDEX idx_iot_tel_tenant_device ON iot_telemetry_events(tenant_id, device_id, recorded_at);
CREATE INDEX idx_iot_tel_tenant_metric ON iot_telemetry_events(tenant_id, metric, recorded_at);

COMMENT ON TABLE iot_devices IS 'Registre d''équipements IoT (CLAUDE.md §9.6).';
COMMENT ON TABLE iot_telemetry_events IS 'Événements télémétrie ingérés depuis l''Edge Gateway ou REST.';
