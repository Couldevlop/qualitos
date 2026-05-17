-- =====================================================================
-- api-iot-hub — V1: device registry + telemetry hypertable
--
-- Runs against the dedicated `qualitos_iot` database (see docker-compose).
-- TimescaleDB extension is enabled if the runtime supports it; on plain
-- PostgreSQL (or H2 in tests) the hypertable conversion is skipped via DO.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'timescaledb') THEN
        CREATE EXTENSION IF NOT EXISTS timescaledb;
    END IF;
END $$;

CREATE TABLE iot_devices (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID         NOT NULL,
    code                    VARCHAR(100) NOT NULL,
    name                    VARCHAR(200) NOT NULL,
    type                    VARCHAR(32)  NOT NULL,
    protocol                VARCHAR(32)  NOT NULL,
    enterprise              VARCHAR(100),
    site                    VARCHAR(100),
    area                    VARCHAR(100),
    work_center             VARCHAR(100),
    equipment               VARCHAR(100),
    cert_fingerprint_sha256 VARCHAR(64),
    twin_json               TEXT,
    provisioned_at          TIMESTAMPTZ NOT NULL,
    last_seen_at            TIMESTAMPTZ,
    CONSTRAINT uk_iot_devices_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_iot_devices_protocol CHECK (protocol IN (
        'OPC_UA','MQTT','SPARKPLUG_B','MODBUS_TCP','HL7_FHIR','HL7_V2',
        'DICOM','LORAWAN','MANUAL')),
    CONSTRAINT chk_iot_devices_type CHECK (type IN (
        'PLC','SENSOR_TEMPERATURE','SENSOR_VIBRATION','SENSOR_PRESSURE',
        'SENSOR_HUMIDITY','SENSOR_GENERIC','CAMERA','BIOMED',
        'AGRO_STATION','BUILDING_BMS','GATEWAY','UNKNOWN'))
);

CREATE INDEX idx_iot_devices_tenant ON iot_devices(tenant_id);
CREATE INDEX idx_iot_devices_tenant_type ON iot_devices(tenant_id, type);
CREATE INDEX idx_iot_devices_tenant_isa95
    ON iot_devices(tenant_id, site, area, work_center, equipment);

CREATE TABLE iot_telemetry (
    id           UUID         NOT NULL,
    tenant_id    UUID         NOT NULL,
    device_id    UUID         NOT NULL,
    metric       VARCHAR(100) NOT NULL,
    value_double DOUBLE PRECISION,
    unit         VARCHAR(32),
    recorded_at  TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (id, recorded_at)
);

-- Composite indexes optimised for time-series scans per tenant + device.
CREATE INDEX idx_iot_telemetry_tenant_device_time
    ON iot_telemetry(tenant_id, device_id, recorded_at DESC);
CREATE INDEX idx_iot_telemetry_tenant_metric_time
    ON iot_telemetry(tenant_id, metric, recorded_at DESC);

-- Promote to TimescaleDB hypertable when the extension is available.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'timescaledb') THEN
        PERFORM create_hypertable('iot_telemetry', 'recorded_at',
                                   if_not_exists => TRUE,
                                   migrate_data => TRUE);
    END IF;
END $$;

COMMENT ON TABLE iot_devices   IS 'IoT device registry (CLAUDE.md §9.6 — ISA-95 hierarchy).';
COMMENT ON TABLE iot_telemetry IS 'Telemetry hypertable (CLAUDE.md §9.3 — TimescaleDB).';
