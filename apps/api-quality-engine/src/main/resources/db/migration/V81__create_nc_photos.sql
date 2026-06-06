-- Stockage binaire des photos de Non-Conformités (§4.3).
-- La table ne conserve QUE les métadonnées + la clé d'objet : les binaires vivent
-- dans un stockage S3-compatible (MinIO en dev), jamais en BDD (pas de BLOB).
CREATE TABLE nc_photos (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id         UUID         NOT NULL,
    nc_id             UUID         NOT NULL,
    object_key        VARCHAR(512) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    original_filename VARCHAR(255),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_nc_photos PRIMARY KEY (id),
    CONSTRAINT uq_nc_photos_object_key UNIQUE (object_key),
    CONSTRAINT fk_nc_photos_nc FOREIGN KEY (nc_id)
        REFERENCES non_conformities (id) ON DELETE CASCADE
);

CREATE INDEX idx_nc_photos_tenant_nc ON nc_photos (tenant_id, nc_id);
