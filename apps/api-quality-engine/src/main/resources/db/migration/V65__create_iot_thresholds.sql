-- V65: Seuils de surveillance IoT (CLAUDE.md §9.7, §9.9).
--
-- Un dépassement de seuil lors de l'ingestion de télémétrie ouvre
-- automatiquement une CAPA (sourceType=IOT_ALERT). Le device_id NULL rend le
-- seuil applicable à tous les équipements du tenant pour la métrique.
-- Le capa_owner_id porté par le seuil devient le responsable de la CAPA générée.

CREATE TABLE iot_thresholds (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    device_id       UUID,
    metric          VARCHAR(100) NOT NULL,
    min_value       DOUBLE PRECISION,
    max_value       DOUBLE PRECISION,
    capa_criticity  VARCHAR(16) NOT NULL,
    capa_owner_id   UUID NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_iot_threshold_device FOREIGN KEY (device_id)
        REFERENCES iot_devices(id) ON DELETE CASCADE,
    CONSTRAINT chk_iot_threshold_criticity CHECK (capa_criticity IN (
        'LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_iot_threshold_bounds CHECK (min_value IS NOT NULL OR max_value IS NOT NULL),
    CONSTRAINT chk_iot_threshold_range  CHECK (min_value IS NULL OR max_value IS NULL OR min_value <= max_value)
);

CREATE INDEX idx_iot_threshold_tenant_metric ON iot_thresholds(tenant_id, metric);
CREATE INDEX idx_iot_threshold_device        ON iot_thresholds(tenant_id, device_id);
