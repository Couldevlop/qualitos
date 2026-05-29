-- V66: enrichissement §9.9 — la dérive capteur peut référencer une fiche FMEA
-- et déclencher un cycle PDCA d'amélioration (en plus de la CAPA IOT_ALERT).

ALTER TABLE iot_thresholds ADD COLUMN fmea_item_id    UUID;
ALTER TABLE iot_thresholds ADD COLUMN open_pdca_cycle BOOLEAN NOT NULL DEFAULT FALSE;
